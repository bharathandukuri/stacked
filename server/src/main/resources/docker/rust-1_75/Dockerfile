FROM execution/isolate:1.0

RUN apk add --no-cache \
    rust \
    cargo \
    gcc \
    musl-dev \
    && rm -rf /root/.cargo/registry /root/.cargo/git

WORKDIR /execution

RUN rustc --version

CMD ["/bin/bash"]
