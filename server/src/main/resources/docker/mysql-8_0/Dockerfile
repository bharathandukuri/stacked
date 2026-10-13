FROM mysql:8.0

ENV MYSQL_ROOT_PASSWORD=stacked_judge
ENV MYSQL_DATABASE=stacked_judge_db

RUN mkdir -p /var/lib/mysql-data && chown -R mysql:mysql /var/lib/mysql-data \
    && printf '[mysqld]\ndatadir=/var/lib/mysql-data\nsocket=/var/run/mysqld/mysqld.sock\nperformance_schema=OFF\ninnodb_buffer_pool_size=32M\nskip-log-bin\ninnodb_flush_log_at_trx_commit=2\ninnodb_flush_method=O_DIRECT\nsync_binlog=0\nmax_connections=50\nskip-name-resolve\ninnodb_doublewrite=0\n\n[client]\nsocket=/var/run/mysqld/mysqld.sock\n' > /etc/mysql/conf.d/judge.cnf \
    && chmod 644 /etc/mysql/conf.d/judge.cnf \
    && mysqld --initialize-insecure --datadir=/var/lib/mysql-data --user=mysql \
    && mysqld --user=mysql --daemonize \
    && for i in $(seq 1 30); do mysql -u root -e "SELECT 1" 2>/dev/null && break || sleep 0.5; done \
    && mysql -u root -e "ALTER USER 'root'@'localhost' IDENTIFIED BY 'stacked_judge'; CREATE DATABASE IF NOT EXISTS stacked_judge_db;" \
    && printf '[client]\nsocket=/var/run/mysqld/mysqld.sock\nuser=root\npassword=stacked_judge\n' > /etc/mysql/conf.d/client.cnf \
    && chmod 644 /etc/mysql/conf.d/client.cnf \
    && cp /etc/mysql/conf.d/client.cnf /root/.my.cnf \
    && chmod 600 /root/.my.cnf \
    && mysqladmin shutdown \
    && rm -rf /usr/share/doc /usr/share/man /var/lib/apt/lists/* /var/log/mysql/* /var/lib/mysql-data/*.err

EXPOSE 3306

CMD ["mysqld", "--user=mysql"]
