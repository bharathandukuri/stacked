FROM execution/isolate:1.0

RUN apk add --no-cache \
    openjdk21-jdk \
    && rm -rf /usr/lib/jvm/java-21-openjdk/lib/src.zip \
              /usr/lib/jvm/java-21-openjdk/demo \
              /usr/lib/jvm/java-21-openjdk/man

ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"

WORKDIR /execution

RUN java -version && javac -version

CMD ["/bin/bash"]