FROM execution/java:21

RUN apk add --no-cache --virtual .build-deps \
        curl \
        unzip \
    && curl -fsSL https://github.com/JetBrains/kotlin/releases/download/v1.9.24/kotlin-compiler-1.9.24.zip -o /tmp/kotlinc.zip \
    && unzip -q /tmp/kotlinc.zip -d /usr/local \
    && rm -f /tmp/kotlinc.zip \
    && apk del .build-deps \
    && ln -s /usr/local/kotlinc/bin/kotlinc /usr/local/bin/kotlinc \
    && ln -s /usr/local/kotlinc/bin/kotlin /usr/local/bin/kotlin \
    && kotlinc -version

WORKDIR /execution

CMD ["/bin/bash"]
