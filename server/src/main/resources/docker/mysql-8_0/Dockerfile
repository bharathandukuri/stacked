FROM mysql:8.0

ENV MYSQL_ROOT_PASSWORD=stacked_judge
ENV MYSQL_DATABASE=stacked_judge_db

# Judge execution performance tuning: disable binary logs and relax fsync for fast test evaluation
RUN printf '[mysqld]\nskip-log-bin\ninnodb_flush_log_at_trx_commit=2\ninnodb_flush_method=O_DIRECT\nsync_binlog=0\nmax_connections=50\n' > /etc/mysql/conf.d/judge.cnf

EXPOSE 3306

CMD ["mysqld"]
