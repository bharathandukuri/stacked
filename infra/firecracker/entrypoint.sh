#!/bin/bash
set -e

echo "[Entrypoint] Initializing Firecracker Sandbox Environment..."

# Ensure KVM permissions if device is mounted
if [ -e /dev/kvm ]; then
    chmod 666 /dev/kvm || true
    echo "[Entrypoint] /dev/kvm permissions verified."
else
    echo "[Entrypoint] Warning: /dev/kvm not mounted. Hardware virtualization will be unavailable."
fi

# Create required directories
mkdir -p /var/lib/firecracker/rootfs
mkdir -p /var/lib/firecracker/kernel
mkdir -p /tmp/firecracker
mkdir -p /tmp/stacked/workspace

chmod -R 777 /tmp/firecracker /tmp/stacked/workspace

echo "[Entrypoint] Starting Firecracker REST API service..."
exec python3 /app/server.py
