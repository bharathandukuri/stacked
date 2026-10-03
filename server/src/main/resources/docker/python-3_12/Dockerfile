FROM execution/isolate:1.0

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        python3 \
        python3-pip \
        python3-setuptools \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /execution

RUN python3 --version

CMD ["/bin/bash"]
