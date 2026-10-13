FROM execution/isolate:1.0

RUN apk add --no-cache \
    gcc \
    make \
    musl-dev

WORKDIR /execution

RUN gcc --version

CMD ["/bin/bash"]
