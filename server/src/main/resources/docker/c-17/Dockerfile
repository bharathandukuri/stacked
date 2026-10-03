FROM execution/isolate:1.0

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        gcc \
        make \
        libc6-dev \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /execution

RUN gcc --version

CMD ["/bin/bash"]
