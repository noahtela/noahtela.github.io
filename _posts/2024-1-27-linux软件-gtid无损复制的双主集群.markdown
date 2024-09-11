---

layout:     post
title:      "linux软件-基于GTID无损复制的双主集群"
subtitle:   " \"linux\""
date:       2024-1-27 18:14:49
author:     "yangsir"
header-img: "img/bg-material.jpg"
catalog: true
tags:
    - 笔记
    - linux学习
    - 云原生


---

> “Yeah It's on. ”


<p id = "build"></p>

# MySQL8.0基于GTID无损复制的双主集群



1. 数据一致性：基于GTID（全局事务标识符）的复制，确保了主从服务器之间的数据一致性。每个事务都有一个独一无二的标识，这样就可以避免数据的重复执行和丢失，提高了数据的准确性。
2. 容错性强：在使用GTID的情况下，如果主服务器出现故障，从服务器可以无缝地接管主服务器的角色，提高了系统的可用性。
3. 简化运维：使用GTID可以简化数据库的运维工作。例如，在切换主从服务器时，不需要关心文件和位置点，只需要指定GTID即可。
4. 支持多主模式：MySQL 8支持多主模式，可以实现多个主服务器之间的数据同步，提高了系统的读写能力。
5. 支持事务级别的复制：GTID提供了事务级别的复制，这意味着在复制过程中，可以精确地控制哪些事务被复制，哪些事务被跳过。
6. 提高性能：MySQL 8在性能方面进行了大量的优化，包括InnoDB存储引擎的改进，新的复制特性等，可以提供更高的处理能力。
7. 支持在线DDL操作：MySQL 8支持在线DDL操作，这意味着在进行表结构修改时，不会阻塞对该表的读写操作，提高了系统的可用性。



安装主从复制模块直接跳过



## 一、第一台

创建用户

```
CREATE USER 'copyuser'@'192.168.171.159' IDENTIFIED WITH mysql_native_password BY '123456';
```

授权

```
grant replication slave on *.* to 'copyuser'@'192.168.171.159';
```



编辑配置文件



```
[mysqld]
server-id = 1 #服务器 id，随意，但要唯一
log-bin=mysql-bin #开启二进制日志
binlog-format=mixed #指定日志格式
log_bin = /usr/local/mysql/data/mysql-bin.log
read-only = 0 #[可选] 0（默认）表示读写（主机），1表示只读（从机）
binlog_expire_logs_seconds = 2592000  #设置日志文件保留的时长，单位是秒
#max_binlog_size = 100M #控制单个二进制日志大小。此参数的最大和默认值是1GB
#replicate_do_db = work #待同步的数据库日志
#replicate_ignore_db = mysql,sys #不同步的数据库日志
gtid_mode=on # 开启GTID复制
enforce-gtid-consistency=true # 跳过一些可能导致执行出错的SQL语句
log-slave-updates=on  #启动链式复制服务器
slave-skip-errors=all #复制过程中允许自动跳过错误
sync_binlog=1 #事务sync缓存刷新
auto_increment_increment=2 #自增值，一般有几台机子就是几
auto_increment_offset=1  #漂移值，也就是步长 另一台为2

basedir = /usr/local/mysql
datadir = /usr/local/mysql/data
socket = /usr/local/mysql/mysql.sock
character-set-server=utf8mb4
port = 3306
sql_mode=NO_ENGINE_SUBSTITUTION,STRICT_TRANS_TABLES
mysqlx_max_connections=65535
max_connections=65535     #修改MySQL最大连接数
thread_cache_size=16384   #实际最大可连接数为16384 不能超过16384即使超过也以16384为准
default-storage-engine=InnoDB  #默认存储引擎
innodb_file_per_table=on       #设置InnoDB为独立表空间模式
innodb_open_files = 1024 #限制Innodb能打开的表的数据
innodb_buffer_pool_size = 1G #缓冲池大小 设置服务器物理内存大小的80%。不要设置过大
innodb_write_io_threads = 8  #io线程
innodb_read_io_threads = 8
innodb_thread_concurrency = 0 #线程并发限制0为无限制
innodb_purge_threads = 1 #线程回收
innodb_flush_log_at_trx_commit = 2 #写日志到磁盘2性能提升
innodb_log_buffer_size = 8M #日志缓存池大小
innodb_log_files_in_group = 3 #设置日志文件的个数推荐为3
innodb_log_group_home_dir = /usr/local/mysql/data #设置表空间文件路径
innodb_max_dirty_pages_pct = 90 #缓冲池脏数据比率
innodb_lock_wait_timeout = 120 #事务锁定超时时间单位为秒默认为50
innodb_buffer_pool_instances = 2 #多个缓冲池
innodb_flush_method = O_DIRECT #写数据和日志文件的方式
rpl_semi_sync_master_enabled=1
rpl_semi_sync_slave_enabled=1
[client]
socket = /usr/local/mysql/mysql.sock
default-character-set=utf8mb4
```

```
systemctl restart mysql
```



进入mysql(两边都进行完以上配置后)

```
change master to master_host='192.168.171.159',master_user='copyuser',master_password='123456',master_port=3306,master_auto_position=1;
```

```
start slave;
```

```
SHOW STATUS LIKE 'Rpl_semi_sync%';
```

![39d6f0fa9d9e76e98d66bd4eee0860d](\img\springBoot\39d6f0fa9d9e76e98d66bd4eee0860d.png)



## 二、第二台

创建用户

```
CREATE USER 'copyuser'@'192.168.171.151' IDENTIFIED WITH mysql_native_password BY '123456';
```

授权

```
grant replication slave on *.* to 'copyuser'@'192.168.171.151';
```



编辑配置文件



```
[mysqld]
server-id = 2 #服务器 id，随意，但要唯一
log-bin=mysql-bin #开启二进制日志
binlog-format=mixed #指定日志格式
log_bin = /usr/local/mysql/data/mysql-bin.log
read-only = 0 #[可选] 0（默认）表示读写（主机），1表示只读（从机）
binlog_expire_logs_seconds = 2592000  #设置日志文件保留的时长，单位是秒
#max_binlog_size = 100M #控制单个二进制日志大小。此参数的最大和默认值是1GB
#replicate_do_db = work #待同步的数据库日志
#replicate_ignore_db = mysql,sys #不同步的数据库日志
gtid_mode=on # 开启GTID复制
enforce-gtid-consistency=true # 跳过一些可能导致执行出错的SQL语句
log-slave-updates=on  #启动链式复制服务器
slave-skip-errors=all #复制过程中允许自动跳过错误
sync_binlog=1 #事务sync缓存刷新
auto_increment_increment=2 #自增值，一般有几台机子就是几
auto_increment_offset=2  #漂移值，也就是步长 另一台为2

basedir = /usr/local/mysql
datadir = /usr/local/mysql/data
socket = /usr/local/mysql/mysql.sock
character-set-server=utf8mb4
port = 3306
sql_mode=NO_ENGINE_SUBSTITUTION,STRICT_TRANS_TABLES
mysqlx_max_connections=65535
max_connections=65535     #修改MySQL最大连接数
thread_cache_size=16384   #实际最大可连接数为16384 不能超过16384即使超过也以16384为准
default-storage-engine=InnoDB  #默认存储引擎
innodb_file_per_table=on       #设置InnoDB为独立表空间模式
innodb_open_files = 1024 #限制Innodb能打开的表的数据
innodb_buffer_pool_size = 1G #缓冲池大小 设置服务器物理内存大小的80%。不要设置过大
innodb_write_io_threads = 8  #io线程
innodb_read_io_threads = 8
innodb_thread_concurrency = 0 #线程并发限制0为无限制
innodb_purge_threads = 1 #线程回收
innodb_flush_log_at_trx_commit = 2 #写日志到磁盘2性能提升
innodb_log_buffer_size = 8M #日志缓存池大小
innodb_log_files_in_group = 3 #设置日志文件的个数推荐为3
innodb_log_group_home_dir = /usr/local/mysql/data #设置表空间文件路径
innodb_max_dirty_pages_pct = 90 #缓冲池脏数据比率
innodb_lock_wait_timeout = 120 #事务锁定超时时间单位为秒默认为50
innodb_buffer_pool_instances = 2 #多个缓冲池
innodb_flush_method = O_DIRECT #写数据和日志文件的方式
rpl_semi_sync_master_enabled=1
rpl_semi_sync_slave_enabled=1
[client]
socket = /usr/local/mysql/mysql.sock
default-character-set=utf8mb4
```

```
systemctl restart mysql
```



进入mysql(两边都进行完以上配置后)

```
change master to master_host='192.168.171.151',master_user='copyuser',master_password='123456',master_port=3306,master_auto_position=1;
```

```
start slave;
```

```
SHOW STATUS LIKE 'Rpl_semi_sync%';
```

![8a0b51a072afd26f6b20228f4553940](\img\springBoot\8a0b51a072afd26f6b20228f4553940.png)



## 三、测试

![b0180553c535ffc63d666bfaa68384f](\img\springBoot\b0180553c535ffc63d666bfaa68384f.png)





![af137153655f3bc1e80f7ca20a84150](C\img\springBoot\af137153655f3bc1e80f7ca20a84150.png)





![4e4aa9775134cf0e0ba61f555d2620c](\img\springBoot\4e4aa9775134cf0e0ba61f555d2620c.png)





![1b4b9b1c40fa8bb8feff32567e6b095](\img\springBoot\1b4b9b1c40fa8bb8feff32567e6b095.png)





测试成功！！！！
