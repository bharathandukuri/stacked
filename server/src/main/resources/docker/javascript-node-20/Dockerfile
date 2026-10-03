FROM execution/isolate:1.0

# Copy Node.js runtime and npm from official Node LTS image
COPY --from=node:20-bookworm-slim /usr/local /usr/local

WORKDIR /execution

RUN node --version && npm --version

CMD ["/bin/bash"]
