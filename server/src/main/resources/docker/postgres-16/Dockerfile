FROM postgres:16-alpine

ENV POSTGRES_PASSWORD=stacked_judge
ENV POSTGRES_DB=stacked_judge_db

# Judge execution performance tuning: disable disk syncs for ultra-fast transient grading transactions
RUN printf 'fsync = off\nsynchronous_commit = off\nfull_page_writes = off\nmax_connections = 50\n' >> /usr/local/share/postgresql/postgresql.conf.sample

EXPOSE 5432

CMD ["postgres"]
