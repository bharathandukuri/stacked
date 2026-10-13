FROM execution/isolate:1.0

RUN apk add --no-cache \
    nodejs \
    npm \
    && npm cache clean --force \
    && rm -rf /root/.npm

WORKDIR /execution

RUN node --version && npm --version

CMD ["/bin/bash"]
