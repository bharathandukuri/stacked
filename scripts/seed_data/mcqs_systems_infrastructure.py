"""Systems, Linux, and Infrastructure MCQs."""

QUESTIONS = [
    {
        "title": "Operating Systems: Process vs Thread Address Space",
        "slug": "os-process-vs-thread-address-space",
        "content": """What is the key difference between **processes** and **threads** in modern operating systems (Linux/POSIX)?

| Entity | Address Space | Memory Sharing | Creation & Switch Overhead |
| :--- | :--- | :--- | :--- |
| **Process** | Isolated private virtual address space | Requires IPC (Shared Memory, Pipes, Sockets) | Higher (MMU Page Table switch) |
| **Thread** | Shared address space within parent process | Shares Heap, Data, Code, File Descriptors | Lower (Context switch preserves page tables) |
""",
        "type": "SINGLE_CHOICE",
        "difficulty": "EASY",
        "status": "PUBLISHED",
        "topics": ["Systems, Linux & Infrastructure"],
        "tags": ["operating-systems", "processes", "threads", "memory"],
        "options": [
            {
                "id": "opt_a",
                "text": "Threads within the same process share the heap and data segments but each maintain their own independent call stack and registers; distinct processes have completely isolated virtual memory spaces",
                "explanation": "Threads share text, data, heap, and open file descriptors, while having dedicated program counter, registers, and stack."
            },
            {
                "id": "opt_b",
                "text": "Threads have isolated memory spaces, while processes share the same heap memory",
                "explanation": "Inverts the definitions."
            },
            {
                "id": "opt_c",
                "text": "Processes can only execute on a single CPU core, while threads can run across multiple cores",
                "explanation": "Processes are scheduled across all available CPU cores by the OS kernel."
            },
            {
                "id": "opt_d",
                "text": "Threads require inter-process communication (IPC) sockets to share variables",
                "explanation": "Threads share memory directly within the process heap."
            }
        ],
        "correctOptionIds": ["opt_a"],
        "explanation": "Threads created within the same process share its heap, open file table, and global variables, but possess private thread stacks and register sets. Separate processes have isolated virtual address spaces protected by hardware MMU page tables.",
        "defaultPoints": 1.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "TCP 4-Way Handshake and TIME_WAIT State",
        "slug": "tcp-4way-handshake-and-time-wait",
        "content": """During the graceful termination of a TCP connection (the 4-way FIN/ACK handshake):

Why does the actively closing endpoint transition into the **`TIME_WAIT`** state (typically lasting $2 \\times \\text{MSL}$ = 60 to 120 seconds)?

Select all that apply.""",
        "type": "MULTIPLE_CHOICE",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["Systems, Linux & Infrastructure"],
        "tags": ["networking", "tcp", "time-wait", "sockets"],
        "options": [
            {
                "id": "opt_ack",
                "text": "To reliably retransmit the final `ACK` if it gets lost in transit, preventing the peer from remaining stuck in the `LAST_ACK` state",
                "explanation": "If the peer's FIN retransmission arrives, the endpoint in TIME_WAIT can resend the ACK."
            },
            {
                "id": "opt_stray",
                "text": "To allow delayed, duplicated, or stray packets from the closed connection to expire in the network, preventing them from corrupting a future new connection using the same IP and port tuple",
                "explanation": "Waiting 2 * MSL guarantees old segments in the network die out before the port tuple is recycled."
            },
            {
                "id": "opt_dns",
                "text": "To query the DNS server and invalidate the IP address mapping",
                "explanation": "TIME_WAIT has nothing to do with DNS."
            },
            {
                "id": "opt_cpu",
                "text": "To cool down the physical CPU temperature after heavy network throughput",
                "explanation": "TIME_WAIT is a network protocol state, not physical hardware throttling."
            }
        ],
        "correctOptionIds": ["opt_ack", "opt_stray"],
        "explanation": "The `TIME_WAIT` state ensures the final ACK reaches the peer (retransmitting if lost) and prevents stray duplicate segments from an old incarnation of the connection from interfering with a newly opened connection on the same socket pair.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Linux Signals: SIGTERM (15) vs SIGKILL (9)",
        "slug": "linux-signals-sigterm-vs-sigkill",
        "content": """In Linux process management, what is the crucial operational difference between **`SIGTERM` (Signal 15)** and **`SIGKILL` (Signal 9)**?

```bash
kill -15 <pid> # SIGTERM
kill -9  <pid> # SIGKILL
```
""",
        "type": "SINGLE_CHOICE",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["Systems, Linux & Infrastructure"],
        "tags": ["linux", "signals", "devops", "processes"],
        "options": [
            {
                "id": "opt_1",
                "text": "`SIGTERM` can be caught, handled, or blocked by the application to execute graceful shutdown routines (flushing buffers, closing connections); `SIGKILL` cannot be caught, blocked, or ignored and terminates the process immediately via the kernel",
                "explanation": "SIGKILL and SIGSTOP are the two signals that cannot be caught or ignored by any user-space process."
            },
            {
                "id": "opt_2",
                "text": "`SIGTERM` kills root processes only, while `SIGKILL` kills non-root user processes",
                "explanation": "Signals apply to processes regardless of whether the user is root or non-root (subject to standard permissions)."
            },
            {
                "id": "opt_3",
                "text": "`SIGTERM` reboots the operating system kernel",
                "explanation": "SIGTERM terminates an individual process, not the host machine."
            },
            {
                "id": "opt_4",
                "text": "`SIGKILL` triggers an in-memory core dump file generation, while `SIGTERM` does not",
                "explanation": "SIGQUIT and SIGSEGV generate core dumps; SIGKILL does not."
            }
        ],
        "correctOptionIds": ["opt_1"],
        "explanation": "`SIGTERM` is the polite termination signal allowing processes to clean up resources. `SIGKILL` forces the Linux kernel to immediately reclaim all resources and kill the process without notifying user space code.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Bash Pipelines and pipefail Option",
        "slug": "bash-pipelines-and-pipefail-option",
        "content": """Consider the following bash pipeline execution:

```bash
# Line 1:
cat non_existent_file.txt | grep "error" | wc -l
echo "Exit Code: $?"

# Line 2 (with pipefail):
set -o pipefail
cat non_existent_file.txt | grep "error" | wc -l
echo "Exit Code: $?"
```

What exit codes are printed for Line 1 and Line 2?

> [!NOTE]
> `cat` exits with 1 (file not found); `grep` receives empty input and exits with 1; `wc -l` counts 0 lines and exits with 0.""",
        "type": "SINGLE_CHOICE",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["Systems, Linux & Infrastructure"],
        "tags": ["linux", "bash", "shell", "scripts", "pipefail"],
        "options": [
            {
                "id": "opt_a",
                "text": "Line 1: `0` (status of the rightmost command `wc -l`) | Line 2: `1` (status of the first non-zero failing command in the pipeline)",
                "explanation": "By default, a pipeline returns the exit status of the LAST command (`wc -l` -> 0). With `set -o pipefail`, the return value is the exit status of the last (rightmost) command that exited with a non-zero status."
            },
            {
                "id": "opt_b",
                "text": "Line 1: `1` | Line 2: `0`",
                "explanation": "Without pipefail, pipelines mask errors from earlier stages if the final command succeeds."
            },
            {
                "id": "opt_c",
                "text": "Line 1: `0` | Line 2: `0`",
                "explanation": "pipefail surfaces the non-zero failure."
            },
            {
                "id": "opt_d",
                "text": "Line 1: `1` | Line 2: `1`",
                "explanation": "Default bash behavior returns the last command's status (0 for wc)."
            }
        ],
        "correctOptionIds": ["opt_a"],
        "explanation": "By default, bash pipelines mask errors because `$?` only reflects the rightmost command (`wc -l` succeeded with exit code 0). Enabling `set -e -o pipefail` in production scripts ensures any failure in the pipeline causes the entire pipeline to report an error.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Docker Containers Share the Host Kernel",
        "slug": "docker-containers-share-host-kernel",
        "content": """Unlike hardware virtual machines (VMs) which run their own guest operating systems on top of a hypervisor:

Docker containers running on a Linux host **share the single host Linux kernel**, isolating processes using Linux kernel **namespaces** (PID, NET, IPC, MNT, UTS) and **cgroups** (resource limits).

Is this statement **True** or **False**?
""",
        "type": "BOOLEAN",
        "difficulty": "EASY",
        "status": "PUBLISHED",
        "topics": ["Systems, Linux & Infrastructure"],
        "tags": ["docker", "containers", "linux", "kernel", "namespaces"],
        "options": [
            {
                "id": "opt_true",
                "text": "True",
                "explanation": "Containers are isolated user-space processes running directly on the host kernel, separated by cgroups and namespaces."
            },
            {
                "id": "opt_false",
                "text": "False",
                "explanation": "Containers do not emulate hardware or boot independent guest kernels."
            }
        ],
        "correctOptionIds": ["opt_true"],
        "explanation": "True. Containers are lightweight because they do not run a guest kernel or hypervisor; they are standard OS processes isolated by kernel namespaces (visibility) and cgroups (CPU/memory constraints) sharing the host kernel.",
        "defaultPoints": 1.0,
        "negativePoints": 0.0,
        "shuffleOptions": False
    },
    {
        "title": "Dockerfile Layer Caching Optimization",
        "slug": "dockerfile-layer-caching-optimization",
        "content": """Which Dockerfile layout maximizes build layer caching for a Node.js or Java application during continuous development?

```dockerfile
# Approach 1:
COPY . .
RUN npm install
CMD ["node", "server.js"]

# Approach 2:
COPY package*.json ./
RUN npm install
COPY . .
CMD ["node", "server.js"]
```
""",
        "type": "SINGLE_CHOICE",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["Systems, Linux & Infrastructure"],
        "tags": ["docker", "dockerfile", "ci-cd", "caching"],
        "options": [
            {
                "id": "opt_app2",
                "text": "**Approach 2**: Copying dependency descriptors (`package*.json`) and installing packages *before* copying application source code ensures `npm install` is only re-executed when dependencies change",
                "explanation": "In Docker, if a layer's inputs change, that layer and all subsequent layers are invalidated. In Approach 1, modifying a single line of JS code invalidates the COPY layer and forces npm install on every single build."
            },
            {
                "id": "opt_app1",
                "text": "**Approach 1**: Copying everything at once reduces the total number of Docker layers, making builds faster",
                "explanation": "Approach 1 destroys cache on every single code edit."
            },
            {
                "id": "opt_both",
                "text": "Both approaches have identical caching performance in modern Docker BuildKit",
                "explanation": "BuildKit still invalidates downstream steps if earlier copied files change."
            },
            {
                "id": "opt_neither",
                "text": "Neither; npm install cannot be cached in Docker",
                "explanation": "Layer caching caches RUN steps if previous layers are unchanged."
            }
        ],
        "correctOptionIds": ["opt_app2"],
        "explanation": "In Docker's layer cache, modifying any file copied by `COPY` invalidates the cache for that step and all downstream steps. By isolating `COPY package*.json` and `RUN npm install`, dependencies are cached and only rebuilt when `package.json` actually changes.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Standard Linux File Descriptors",
        "slug": "standard-linux-file-descriptors",
        "content": """In POSIX/Linux systems, what are the integer file descriptors assigned by default to standard input, standard output, and standard error?

```bash
command > stdout.log 2> stderr.log < input.txt
```
""",
        "type": "SINGLE_CHOICE",
        "difficulty": "EASY",
        "status": "PUBLISHED",
        "topics": ["Systems, Linux & Infrastructure"],
        "tags": ["linux", "file-descriptors", "bash", "io-redirection"],
        "options": [
            {
                "id": "opt_1",
                "text": "`0 = STDIN`, `1 = STDOUT`, `2 = STDERR`",
                "explanation": "Standard POSIX definition: 0 is standard input, 1 is standard output, and 2 is standard error."
            },
            {
                "id": "opt_2",
                "text": "`1 = STDIN`, `2 = STDOUT`, `3 = STDERR`",
                "explanation": "File descriptors start at index 0."
            },
            {
                "id": "opt_3",
                "text": "`0 = STDOUT`, `1 = STDIN`, `2 = STDERR`",
                "explanation": "0 is input, not output."
            },
            {
                "id": "opt_4",
                "text": "`0 = STDERR`, `1 = STDIN`, `2 = STDOUT`",
                "explanation": "Incorrect assignment."
            }
        ],
        "correctOptionIds": ["opt_1"],
        "explanation": "POSIX specifies that `0` is `stdin`, `1` is `stdout`, and `2` is `stderr`. This is why redirecting standard error uses syntax `2>&1` or `2> error.log`.",
        "defaultPoints": 1.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Git Branching Workflows: merge vs rebase",
        "slug": "git-branching-merge-vs-rebase",
        "content": """When integrating changes from a feature branch into `main`:

What are the architectural differences between **`git merge`** and **`git rebase`**?

Select all that apply.""",
        "type": "MULTIPLE_CHOICE",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["Systems, Linux & Infrastructure"],
        "tags": ["git", "version-control", "rebase", "merge", "devops"],
        "options": [
            {
                "id": "opt_merge",
                "text": "**`git merge`**: Creates a non-destructive merge commit with two parent commits, preserving the exact chronological history and context of when the branch existed",
                "explanation": "Merge preserves true historical commit order and branch topology."
            },
            {
                "id": "opt_rebase",
                "text": "**`git rebase`**: Replays feature branch commits onto the tip of the base branch, producing a clean, linear project history with newly generated commit hashes",
                "explanation": "Rebase rewrites commit history and hashes by re-applying diffs on top of upstream."
            },
            {
                "id": "opt_public_rebase",
                "text": "It is safe practice to run `git rebase` on shared public branches (such as `main`) that other developers are actively branching off of",
                "explanation": "Golden Rule of Rebase: Never rebase a public shared branch, as it invalidates commit hashes for everyone else."
            },
            {
                "id": "opt_squash",
                "text": "Interactive rebase (`git rebase -i`) allows squashing multiple small work-in-progress commits into a single cohesive commit before opening a pull request",
                "explanation": "Squash combines commits to maintain clean repository logs."
            }
        ],
        "correctOptionIds": ["opt_merge", "opt_rebase", "opt_squash"],
        "explanation": "`git merge` preserves branch history via a two-parent merge commit. `git rebase` rewrites history linearly by creating new commits on top of the target branch. Rebasing shared public branches causes severe divergence for team members.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "TLS 1.3 Handshake and Forward Secrecy",
        "slug": "tls-13-handshake-and-forward-secrecy",
        "content": """In modern internet security protocols:

**TLS 1.3** achieved major performance and security improvements over TLS 1.2 by:

1. Reducing standard handshake latency from **2 round-trips (2-RTT) down to 1 round-trip (1-RTT)**
2. Mandating **Forward Secrecy** by deprecating static RSA key exchange in favor of Ephemeral Diffie-Hellman (`ECDHE`)

Is this statement **True** or **False**?
""",
        "type": "BOOLEAN",
        "difficulty": "HARD",
        "status": "PUBLISHED",
        "topics": ["Systems, Linux & Infrastructure", "Backend Engineering & Spring"],
        "tags": ["tls", "security", "cryptography", "networking", "forward-secrecy"],
        "options": [
            {
                "id": "opt_true",
                "text": "True",
                "explanation": "TLS 1.3 eliminated redundant round-trips (allowing 1-RTT and 0-RTT session resumption) and removed static RSA key exchange to guarantee forward secrecy."
            },
            {
                "id": "opt_false",
                "text": "False",
                "explanation": "TLS 1.3 specifically made 1-RTT standard and removed non-PFS cipher suites."
            }
        ],
        "correctOptionIds": ["opt_true"],
        "explanation": "True. TLS 1.3 cut handshake latency in half (1-RTT) and removed legacy ciphers (static RSA key exchange) that did not support Perfect Forward Secrecy (PFS). With Ephemeral Diffie-Hellman (`ECDHE`), compromising a server's private key in the future does not decrypt past recorded sessions.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": False
    }
]
