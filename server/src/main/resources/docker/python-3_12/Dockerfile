FROM execution/isolate:1.0

RUN apk add --no-cache \
    python3 \
    && find / -name '__pycache__' -exec rm -rf {} + 2>/dev/null || true

WORKDIR /execution

RUN python3 --version

CMD ["/bin/bash"]
