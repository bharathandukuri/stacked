FROM execution/isolate:1.0

RUN apk add --no-cache go \
    && go clean -cache -modcache \
    && rm -rf /root/go /root/.cache

WORKDIR /execution

RUN go version

CMD ["/bin/bash"]
