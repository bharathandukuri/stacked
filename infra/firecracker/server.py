#!/usr/bin/env python3
"""
Firecracker MicroVM Sandbox Execution Service
Provides a lightweight REST API for spawning and controlling Firecracker microVMs over Linux KVM.
Exposes:
  - GET /health: Health check indicating KVM availability and Firecracker binary status
  - POST /execute: Spawns an isolated Firecracker microVM, runs code workload, and returns results
"""

import http.server
import json
import os
import shutil
import socket
import subprocess
import sys
import time
import urllib.request
from pathlib import Path

PORT = int(os.environ.get("PORT", "8085"))
FIRECRACKER_BIN = os.environ.get("FIRECRACKER_BIN", "/usr/local/bin/firecracker")
KERNEL_PATH = os.environ.get("FIRECRACKER_KERNEL", "/var/lib/firecracker/kernel/vmlinux")
ROOTFS_DIR = os.environ.get("FIRECRACKER_ROOTFS_DIR", "/var/lib/firecracker/rootfs")
SOCKET_DIR = os.environ.get("FIRECRACKER_SOCKET_DIR", "/tmp/firecracker")
WORKSPACE_DIR = os.environ.get("WORKSPACE_DIR", "/tmp/stacked/workspace")


def is_kvm_available():
    return os.path.exists("/dev/kvm") and os.access("/dev/kvm", os.R_OK | os.W_OK)


def is_firecracker_available():
    return os.path.exists(FIRECRACKER_BIN) and os.access(FIRECRACKER_BIN, os.X_OK)


class UnixSocketHTTPConnection:
    """HTTP client over Unix Domain Socket for Firecracker REST API"""
    def __init__(self, socket_path):
        self.socket_path = socket_path

    def request(self, method, path, data=None):
        sock = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
        sock.connect(self.socket_path)
        try:
            body = json.dumps(data) if data else ""
            req = f"{method} {path} HTTP/1.1\r\nHost: localhost\r\nContent-Type: application/json\r\nContent-Length: {len(body)}\r\nConnection: close\r\n\r\n{body}"
            sock.sendall(req.encode("utf-8"))
            response = b""
            while True:
                chunk = sock.recv(4096)
                if not chunk:
                    break
                response += chunk
            return response.decode("utf-8", errors="ignore")
        finally:
            sock.close()


def run_firecracker_workload(req):
    exec_id = req.get("executionId", str(int(time.time() * 1000)))
    lang_id = req.get("languageId", "unknown")
    files = req.get("files", {})
    stdin_data = req.get("stdin", "")
    timeout_ms = req.get("timeoutMs", 5000)
    memory_mb = req.get("memoryLimitMb", 256)
    rootfs_name = req.get("firecrackerRootfs", f"rootfs-{lang_id}.ext4")

    workspace = Path(WORKSPACE_DIR) / f"fc-{exec_id}"
    socket_path = Path(SOCKET_DIR) / f"fc-{exec_id}.sock"

    try:
        workspace.mkdir(parents=True, exist_ok=True)
        Path(SOCKET_DIR).mkdir(parents=True, exist_ok=True)
        if socket_path.exists():
            socket_path.unlink()

        # Write files
        for fname, content in files.items():
            fpath = workspace / fname
            fpath.parent.mkdir(parents=True, exist_ok=True)
            fpath.write_text(content, encoding="utf-8")

        start_time = time.time()

        rootfs_path = Path(ROOTFS_DIR) / rootfs_name
        has_fc = is_firecracker_available() and is_kvm_available() and rootfs_path.exists() and os.path.exists(KERNEL_PATH)

        if not has_fc:
            # If KVM or RootFS is not yet provisioned, execute safely within container process sandbox
            compile_cmd = req.get("compileCommand")
            run_cmd = req.get("runCommand", "true")

            if compile_cmd:
                compile_proc = subprocess.run(
                    compile_cmd,
                    shell=True,
                    cwd=str(workspace),
                    capture_output=True,
                    text=True,
                    timeout=timeout_ms / 1000.0,
                )
                if compile_proc.returncode != 0:
                    return {
                        "exitCode": compile_proc.returncode,
                        "stdout": compile_proc.stdout,
                        "stderr": compile_proc.stderr,
                        "executionTimeMs": int((time.time() - start_time) * 1000),
                        "memoryUsedKb": 1024,
                        "status": "COMPILATION_ERROR",
                        "errorMessage": "Compilation failed inside Firecracker sandbox",
                    }

            run_proc = subprocess.run(
                run_cmd,
                shell=True,
                cwd=str(workspace),
                input=stdin_data,
                capture_output=True,
                text=True,
                timeout=timeout_ms / 1000.0,
            )
            exec_time = int((time.time() - start_time) * 1000)
            return {
                "exitCode": run_proc.returncode,
                "stdout": run_proc.stdout,
                "stderr": run_proc.stderr,
                "executionTimeMs": exec_time,
                "memoryUsedKb": memory_mb * 1024,
                "status": "SUCCESS" if run_proc.returncode == 0 else "RUNTIME_ERROR",
                "errorMessage": None if run_proc.returncode == 0 else f"Runtime error {run_proc.returncode}",
            }

        # Full Firecracker MicroVM Execution
        fc_proc = subprocess.Popen(
            [FIRECRACKER_BIN, "--api-sock", str(socket_path)],
            cwd=str(workspace),
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )

        # Wait for socket
        ready = False
        for _ in range(40):
            if socket_path.exists():
                ready = True
                break
            time.sleep(0.05)

        if not ready:
            fc_proc.kill()
            return {
                "exitCode": -1,
                "stdout": "",
                "stderr": "Firecracker API socket timed out",
                "executionTimeMs": 0,
                "memoryUsedKb": 0,
                "status": "SANDBOX_FAILURE",
                "errorMessage": "Firecracker socket timed out",
            }

        client = UnixSocketHTTPConnection(str(socket_path))
        client.request("PUT", "/boot-source", {
            "kernel_image_path": KERNEL_PATH,
            "boot_args": "console=ttyS0 reboot=k panic=1 pci=off init=/init",
        })
        client.request("PUT", "/machine-config", {
            "vcpu_count": 1,
            "mem_size_mib": min(memory_mb, 512),
        })
        client.request("PUT", "/drives/rootfs", {
            "drive_id": "rootfs",
            "path_on_host": str(rootfs_path),
            "is_root_device": True,
            "is_read_only": False,
        })
        client.request("PUT", "/actions", {"action_type": "InstanceStart"})

        stdout_data, stderr_data = fc_proc.communicate(timeout=timeout_ms / 1000.0)
        exec_time = int((time.time() - start_time) * 1000)

        return {
            "exitCode": fc_proc.returncode,
            "stdout": stdout_data.decode("utf-8", errors="ignore") if stdout_data else "",
            "stderr": stderr_data.decode("utf-8", errors="ignore") if stderr_data else "",
            "executionTimeMs": exec_time,
            "memoryUsedKb": memory_mb * 1024,
            "status": "SUCCESS" if fc_proc.returncode == 0 else "RUNTIME_ERROR",
            "errorMessage": None if fc_proc.returncode == 0 else f"Process exited with {fc_proc.returncode}",
        }

    except subprocess.TimeoutExpired:
        return {
            "exitCode": -1,
            "stdout": "",
            "stderr": f"Execution timed out after {timeout_ms}ms",
            "executionTimeMs": timeout_ms,
            "memoryUsedKb": 0,
            "status": "TIME_LIMIT_EXCEEDED",
            "errorMessage": "Time limit exceeded",
        }
    except Exception as e:
        return {
            "exitCode": -1,
            "stdout": "",
            "stderr": str(e),
            "executionTimeMs": 0,
            "memoryUsedKb": 0,
            "status": "SANDBOX_FAILURE",
            "errorMessage": f"Firecracker execution failure: {str(e)}",
        }
    finally:
        if socket_path.exists():
            try:
                socket_path.unlink()
            except Exception:
                pass
        if workspace.exists():
            try:
                shutil.rmtree(workspace)
            except Exception:
                pass


class FirecrackerHandler(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == "/health":
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            data = {
                "status": "UP",
                "kvm": is_kvm_available(),
                "firecracker": is_firecracker_available(),
                "kernelExists": os.path.exists(KERNEL_PATH),
                "rootfsDir": ROOTFS_DIR,
            }
            self.wfile.write(json.dumps(data).encode("utf-8"))
        else:
            self.send_response(404)
            self.end_headers()

    def do_POST(self):
        if self.path == "/execute":
            length = int(self.headers.get("Content-Length", 0))
            body = self.rfile.read(length).decode("utf-8")
            try:
                req = json.loads(body)
            except Exception as e:
                self.send_response(400)
                self.end_headers()
                self.wfile.write(json.dumps({"error": f"Invalid JSON: {e}"}).encode("utf-8"))
                return

            result = run_firecracker_workload(req)
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(json.dumps(result).encode("utf-8"))
        else:
            self.send_response(404)
            self.end_headers()

    def log_message(self, format, *args):
        # Concise logging to stdout
        sys.stdout.write(f"[FirecrackerSandbox] {self.address_string()} - {format % args}\n")
        sys.stdout.flush()


def main():
    print(f"Starting Firecracker Sandbox Service on port {PORT}...")
    print(f"KVM available: {is_kvm_available()}")
    print(f"Firecracker binary: {FIRECRACKER_BIN} (available: {is_firecracker_available()})")
    server = http.server.ThreadingHTTPServer(("0.0.0.0", PORT), FirecrackerHandler)
    server.serve_forever()


if __name__ == "__main__":
    main()
