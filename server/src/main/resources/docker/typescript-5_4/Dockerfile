FROM execution/javascript:node-20

RUN npm install -g tsx \
    && npm cache clean --force \
    && rm -rf /root/.npm \
    && tsx --version

WORKDIR /execution

CMD ["/bin/bash"]
