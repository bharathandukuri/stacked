FROM mongo:8.0

ENV GLIBC_TUNABLES=glibc.pthread.rseq=1

# Pre-initialize WiredTiger storage and stacked_judge_db database for instant container startup
RUN mongod --fork --logpath /tmp/mongod.log --bind_ip_all --wiredTigerCacheSizeGB 0.25 \
    && for i in $(seq 1 30); do mongosh --quiet --eval "db.adminCommand('ping')" >/dev/null 2>&1 && break || sleep 0.2; done \
    && mongosh --quiet stacked_judge_db --eval "db.init.insertOne({ ready: true })" >/dev/null \
    && mongod --shutdown \
    && rm -rf /tmp/mongod.log /usr/share/doc /usr/share/man /var/lib/apt/lists/*

EXPOSE 27017

CMD ["mongod", "--bind_ip_all", "--wiredTigerCacheSizeGB", "0.25"]
