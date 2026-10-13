# Stage 1: Build isolate from source with a pinned stable release version
FROM alpine:3.20 AS builder

ARG ISOLATE_VERSION=v2.7

RUN apk add --no-cache \
    gcc \
    make \
    musl-dev \
    libcap-dev \
    libseccomp-dev \
    git \
    linux-headers

RUN git clone --branch "${ISOLATE_VERSION}" --depth 1 https://github.com/ioi/isolate.git /tmp/isolate \
    && cd /tmp/isolate \
    && make PREFIX=/usr CONFIG=/etc/isolate VARPREFIX=/var isolate default.cf CFLAGS_EXTRA="-DSYS_quotactl_fd=443" \
    && strip /tmp/isolate/isolate \
    && sed -i "s/^subid_user = isolate/#subid_user = isolate/" default.cf \
    && sed -i "s/^# first_uid = 60000/first_uid = 60000/" default.cf \
    && sed -i "s/^# first_gid = 60000/first_gid = 60000/" default.cf \
    && sed -i "s/^# num_boxes = 1000/num_boxes = 1000/" default.cf

# Stage 2: Minimal Alpine runtime image
FROM alpine:3.20

RUN apk add --no-cache \
    bash \
    coreutils \
    libcap \
    libseccomp

COPY --from=builder /tmp/isolate/isolate /usr/bin/isolate
COPY --from=builder /tmp/isolate/default.cf /etc/isolate

# Setup permissions, directories, and backwards compatibility symlinks
RUN chmod 4755 /usr/bin/isolate \
    && mkdir -p /var/lib/isolate /run/isolate/locks /usr/etc /usr/local/etc /var/local/lib \
    && chown -R root:root /var/lib/isolate \
    && ln -sf /usr/bin/isolate /usr/local/bin/isolate \
    && ln -sf /etc/isolate /usr/etc/isolate \
    && ln -sf /etc/isolate /usr/local/etc/isolate \
    && ln -sf /var/lib/isolate /var/local/lib/isolate \
    && isolate --version

WORKDIR /execution

CMD ["/bin/bash"]