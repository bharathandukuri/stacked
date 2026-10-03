FROM ubuntu:24.04

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        ca-certificates \
        curl \
        coreutils \
        bash \
    && mkdir -p /etc/apt/keyrings \
    && curl -fsSL https://www.ucw.cz/isolate/debian/signing-key.asc \
        -o /etc/apt/keyrings/isolate.asc \
    && printf '%s\n' \
        'Types: deb' \
        'URIs: http://www.ucw.cz/isolate/debian/' \
        'Suites: trixie-isolate' \
        'Components: main' \
        'Architectures: amd64' \
        'Signed-By: /etc/apt/keyrings/isolate.asc' \
        > /etc/apt/sources.list.d/isolate.sources \
    && apt-get update \
    && apt-get install -y --no-install-recommends \
        isolate \
    && isolate --version \
    && rm -rf /var/lib/apt/lists/*

# Directory used by isolate
RUN mkdir -p /var/local/lib/isolate \
    && chown -R root:root /var/local/lib/isolate

WORKDIR /execution

CMD ["/bin/bash"]