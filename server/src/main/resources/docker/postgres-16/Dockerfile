FROM postgres:16-alpine3.20

ENV POSTGRES_PASSWORD=stacked_judge
ENV POSTGRES_DB=stacked_judge_db
ENV PGDATA=/var/lib/postgresql/data

# Performance tuning and pre-initialization for ultra-fast ephemeral judge executions
RUN printf 'fsync = off\nsynchronous_commit = off\nfull_page_writes = off\nmax_connections = 50\nshared_buffers = 32MB\n' >> /usr/local/share/postgresql/postgresql.conf.sample \
    && su-exec postgres initdb -D "$PGDATA" \
    && su-exec postgres pg_ctl -D "$PGDATA" -o "-c listen_addresses=''" -w start \
    && psql -U postgres -c "ALTER USER postgres WITH PASSWORD 'stacked_judge';" \
    && psql -U postgres -c "CREATE DATABASE stacked_judge_db;" \
    && su-exec postgres pg_ctl -D "$PGDATA" -m fast -w stop \
    && rm -rf /usr/local/share/doc /usr/local/share/man

EXPOSE 5432

CMD ["postgres"]
