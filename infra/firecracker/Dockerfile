FROM debian:bookworm-slim

# Install system utilities and Python runtime
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    ca-certificates \
    tar \
    python3 \
    && rm -rf /var/lib/apt/lists/*

# Install official Firecracker release binary
ARG FIRECRACKER_VERSION=v1.7.0
RUN ARCH="$(uname -m)" && \
    curl -fSL "https://github.com/firecracker-microvm/firecracker/releases/download/${FIRECRACKER_VERSION}/firecracker-${FIRECRACKER_VERSION}-${ARCH}.tgz" -o /tmp/firecracker.tgz && \
    tar -xzf /tmp/firecracker.tgz -C /tmp && \
    mv /tmp/release-${FIRECRACKER_VERSION}-${ARCH}/firecracker-${FIRECRACKER_VERSION}-${ARCH} /usr/local/bin/firecracker && \
    mv /tmp/release-${FIRECRACKER_VERSION}-${ARCH}/jailer-${FIRECRACKER_VERSION}-${ARCH} /usr/local/bin/jailer && \
    chmod +x /usr/local/bin/firecracker /usr/local/bin/jailer && \
    rm -rf /tmp/firecracker* /tmp/release*

WORKDIR /app

COPY server.py /app/server.py
COPY entrypoint.sh /app/entrypoint.sh

RUN chmod +x /app/entrypoint.sh /app/server.py

# Create storage mounts
RUN mkdir -p /var/lib/firecracker/rootfs /var/lib/firecracker/kernel /tmp/firecracker /tmp/stacked/workspace

EXPOSE 8085

ENTRYPOINT ["/app/entrypoint.sh"]
