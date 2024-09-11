---

layout:     post
title:      "linux软件-基于MGR的多主集群"
subtitle:   " \"linux\""
date:       2024-1-29 17:22:49
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

# MySQL8.0基于MGR的多主集群





1. 高可用性：MGR提供了原生的高可用性解决方案，通过自动故障检测和自动故障转移，确保数据库的持续可用性。当主节点发生故障时，集群会自动选举新的主节点，不会影响数据库的正常运行。
2. 数据一致性：MGR使用了多主复制的方式，所有节点都可以接收和处理写入操作。通过使用基于原子广播的组提交协议，确保所有节点上的数据保持一致性。这意味着在任何节点上进行的写入操作都会被同步到其他节点，保证了数据的一致性。
3. 扩展性：多主集群可以方便地进行水平扩展，通过添加更多的节点来增加集群的处理能力。新节点加入集群后，会自动同步数据，并参与到主节点的选举过程中。
4. 读写分离：多主集群可以支持读写分离，即读操作可以在任何节点上进行，而写操作只能在主节点上进行。这样可以有效地分担主节点的负载，提高整个集群的性能。
5. 自动故障转移：当主节点发生故障时，MGR会自动选举新的主节点，并将所有读写操作重定向到新的主节点上，实现自动故障转移。这样可以减少手动干预的需要，提高系统的可靠性。
6. 简化管理：MGR提供了基于MySQL Shell的管理工具，可以方便地管理和监控集群的状态。管理员可以通过命令行界面进行集群的配置、监控和维护，简化了集群管理的工作。



现在就来搭建MGR多主集群

环境：centos 7 三台

​		   mysql 8.0.26

## 一、修改主机名 host文件 删除auto.cnf 关闭SElinux 防火墙

### 1、修改主机名

```
vi /etc/hosts
```

![image-20240129162643884](\img\springBoot\image-20240129162643884.png)



### 2、删除auto.cnf 

如果是克隆来的虚拟机，mysql的uuid会是一样的，这样直接导致后面搭建失败，需要删除auto.cnf 文件，然后重启mysql，就会自动生成新的uuid。

```shell
rm -rf /usr/local/mysql/data/auto.cnf
systemctl restart mysql 
```



### 3、关闭SElinux 防火墙

- 打开终端，以root用户身份登录。
- 检查当前SElinux状态，输入以下命令：

```
sestatus
```



如果输出结果中的"SELinux status"为"enabled"，则表示SElinux处于启用状态。

- 临时关闭SElinux，输入以下命令：

```
setenforce 0
```



此命令会将SElinux模式从Enforcing切换为Permissive，但重启系统后会恢复为Enforcing模式。

- 永久关闭SElinux，编辑配置文件/etc/selinux/config，输入以下命令：

```
vi /etc/selinux/config
```



将文件中的`SELINUX=enforcing`改为`SELINUX=disabled`。保存并退出配置文件，重启系统使设置生效。



## 二、chrony同步时间

```shell
yum -y install chrony
//注释pool 2.centos.pool.ntp.org iburst
sed -i 's/^pool pool.ntp.org iburst/#&/' /etc/chrony.conf
//添加三个阿里云NTP的服务器
echo -e "server ntp1.aliyun.com iburst\nserver ntp2.aliyun.com iburst\nserver ntp3.aliyun.com iburst" | sudo tee -a /etc/chrony.conf


systemctl restart chronyd
chronyc sourcestats -v

systemctl enable chronyd --now  #设为开机自启
```



## 三、编辑配置文件

### 1、第一台

```
[mysqld]
server-id = 1 #服务器 id，随意，但要唯一
log-bin=mysql-bin #开启二进制日志
#binlog-format=mixed #指定日志格式
log_bin = /usr/local/mysql/data/mysql-bin.log    #二进制文件存放路径
#read-only = 0    #[可选] 0（默认）表示读写（主机），1表示只读（从机）
binlog_expire_logs_seconds = 2592000    #设置日志文件保留的时长，单位是秒
symbolic-links = 0
#max_binlog_size = 100M    #控制单个二进制日志大小。此参数的最大和默认值是1GB
#binlog_do_db = work    #待同步的数据库日志
#binlog_ignore_db = mysql,sys    #不同步的数据库日志
log-slave-updates=ON
#slave-skip-errors=all
#sync_binlog=1
#auto_increment_increment=2
#auto_increment_offset=1

# 开启GTID复制
gtid_mode=on
# 跳过一些可能导致执行出错的SQL语句
enforce-gtid-consistency=true
binlog_checksum=NONE
log_bin=binlog

#binlog格式,MGR要求必须是ROW,不过就算不是MGR,也最好用row
binlog_format=row
#MGR使用乐观锁,所以官网建议隔离级别是RC,减少锁粒度
transaction_isolation = READ-COMMITTED
#因为集群会在故障恢复时互相检查binlog的数据,
#所以需要记录下集群内其他服务器发过来已经执行过的binlog,按GTID来区分是否执行过.
log-slave-updates=1
#binlog校验规则,5.6之后的高版本是CRC32,低版本都是NONE,但是MGR要求使用NONE
binlog_checksum=NONE
#基于安全的考虑,MGR集群要求复制模式要改成slave记录记录到表中,不然就报错
master_info_repository=TABLE
#同上配套
relay_log_info_repository=TABLE
 
#组复制设置
#记录事务的算法,官网建议设置该参数使用 XXHASH64 算法
transaction_write_set_extraction = XXHASH64
plugin_load_add='group_replication=group_replication.so'
#相当于此GROUP的名字,是UUID值,不能和集群内其他GTID值的UUID混用,可用uuidgen来生成一个新的,
#主要是用来区分整个内网里边的各个不同的GROUP,而且也是这个group内的GTID值的UUID
loose-group_replication_group_name = '5dbabbe6-8050-49a0-9131-1de449167446'
#IP地址白名单,默认只添加127.0.0.1,不会允许来自外部主机的连接,按需安全设置
loose-group_replication_ip_whitelist = '127.0.0.1/8,192.168.171.0/24'
#是否随服务器启动而自动启动组复制,不建议直接启动,怕故障恢复时有扰乱数据准确性的特殊情况
loose-group_replication_start_on_boot = OFF
#本地MGR的IP地址和端口，host:port,是MGR的端口,不是数据库的端口
loose-group_replication_local_address = 'ys1:33081'
#需要接受本MGR实例控制的服务器IP地址和端口,是MGR的端口,不是数据库的端口
loose-group_replication_group_seeds = 'ys1:33081,ys2:33081,ys3:33081'
#开启引导模式,添加组成员，用于第一次搭建MGR或重建MGR的时候使用,只需要在集群内的其中一台开启,
loose-group_replication_bootstrap_group = OFF
loose-group_replication_recovery_retry_count=31536000
#是否启动单主模式，如果启动，则本实例是主库，提供读写，其他实例仅提供读,如果为off就是多主模式了
loose-group_replication_single_primary_mode = ON
#多主模式下,强制检查每一个实例是否允许该操作,如果不是多主,可以关闭
loose-group_replication_enforce_update_everywhere_checks = off

report_host=192.168.171.161
report_port=3306
loose-group_replication_recovery_get_public_key=ON


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

loose-plugin_load = "rpl_semi_sync_master=semisync_master.so;rpl_semi_sync_slave=semisync_slave.so"
rpl_semi_sync_master_enabled=1   #开启半同步
rpl_semi_sync_master_timeout=1000  #定义半同步超时时间
rpl_semi_sync_slave_enabled=1

[client]
socket = /usr/local/mysql/mysql.sock
default-character-set=utf8mb4
```



### 2、第二台

```
[mysqld]
server-id = 2 #服务器 id，随意，但要唯一
log-bin=mysql-bin #开启二进制日志
#binlog-format=mixed #指定日志格式
log_bin = /usr/local/mysql/data/mysql-bin.log    #二进制文件存放路径
#read-only = 0    #[可选] 0（默认）表示读写（主机），1表示只读（从机）
binlog_expire_logs_seconds = 2592000    #设置日志文件保留的时长，单位是秒
symbolic-links = 0
#max_binlog_size = 100M    #控制单个二进制日志大小。此参数的最大和默认值是1GB
#binlog_do_db = work    #待同步的数据库日志
#binlog_ignore_db = mysql,sys    #不同步的数据库日志
log-slave-updates=ON
#slave-skip-errors=all
#sync_binlog=1
#auto_increment_increment=2
#auto_increment_offset=1

# 开启GTID复制
gtid_mode=on
# 跳过一些可能导致执行出错的SQL语句
enforce-gtid-consistency=true
binlog_checksum=NONE
log_bin=binlog

#binlog格式,MGR要求必须是ROW,不过就算不是MGR,也最好用row
binlog_format=row
#MGR使用乐观锁,所以官网建议隔离级别是RC,减少锁粒度
transaction_isolation = READ-COMMITTED
#因为集群会在故障恢复时互相检查binlog的数据,
#所以需要记录下集群内其他服务器发过来已经执行过的binlog,按GTID来区分是否执行过.
log-slave-updates=1
#binlog校验规则,5.6之后的高版本是CRC32,低版本都是NONE,但是MGR要求使用NONE
binlog_checksum=NONE
#基于安全的考虑,MGR集群要求复制模式要改成slave记录记录到表中,不然就报错
master_info_repository=TABLE
#同上配套
relay_log_info_repository=TABLE
 
#组复制设置
#记录事务的算法,官网建议设置该参数使用 XXHASH64 算法
transaction_write_set_extraction = XXHASH64
plugin_load_add='group_replication=group_replication.so'
#相当于此GROUP的名字,是UUID值,不能和集群内其他GTID值的UUID混用,可用uuidgen来生成一个新的,
#主要是用来区分整个内网里边的各个不同的GROUP,而且也是这个group内的GTID值的UUID
loose-group_replication_group_name = '5dbabbe6-8050-49a0-9131-1de449167446'
#IP地址白名单,默认只添加127.0.0.1,不会允许来自外部主机的连接,按需安全设置
loose-group_replication_ip_whitelist = '127.0.0.1/8,192.168.171.0/24'
#是否随服务器启动而自动启动组复制,不建议直接启动,怕故障恢复时有扰乱数据准确性的特殊情况
loose-group_replication_start_on_boot = OFF
#本地MGR的IP地址和端口，host:port,是MGR的端口,不是数据库的端口
loose-group_replication_local_address = 'ys2:33081'
#需要接受本MGR实例控制的服务器IP地址和端口,是MGR的端口,不是数据库的端口
loose-group_replication_group_seeds = 'ys1:33081,ys2:33081,ys3:33081'
#开启引导模式,添加组成员，用于第一次搭建MGR或重建MGR的时候使用,只需要在集群内的其中一台开启,
loose-group_replication_bootstrap_group = OFF
loose-group_replication_recovery_retry_count=31536000
#是否启动单主模式，如果启动，则本实例是主库，提供读写，其他实例仅提供读,如果为off就是多主模式了
loose-group_replication_single_primary_mode = ON
#多主模式下,强制检查每一个实例是否允许该操作,如果不是多主,可以关闭
loose-group_replication_enforce_update_everywhere_checks = off

report_host=192.168.171.162
report_port=3306
loose-group_replication_recovery_get_public_key=ON


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

loose-plugin_load = "rpl_semi_sync_master=semisync_master.so;rpl_semi_sync_slave=semisync_slave.so"
rpl_semi_sync_master_enabled=1   #开启半同步
rpl_semi_sync_master_timeout=1000  #定义半同步超时时间
rpl_semi_sync_slave_enabled=1

[client]
socket = /usr/local/mysql/mysql.sock
default-character-set=utf8mb4
```



### 3、第三台

```
[mysqld]
server-id = 3 #服务器 id，随意，但要唯一
log-bin=mysql-bin #开启二进制日志
#binlog-format=mixed #指定日志格式
log_bin = /usr/local/mysql/data/mysql-bin.log    #二进制文件存放路径
#read-only = 0    #[可选] 0（默认）表示读写（主机），1表示只读（从机）
binlog_expire_logs_seconds = 2592000    #设置日志文件保留的时长，单位是秒
symbolic-links = 0
#max_binlog_size = 100M    #控制单个二进制日志大小。此参数的最大和默认值是1GB
#binlog_do_db = work    #待同步的数据库日志
#binlog_ignore_db = mysql,sys    #不同步的数据库日志
log-slave-updates=ON
#slave-skip-errors=all
#sync_binlog=1
#auto_increment_increment=2
#auto_increment_offset=1

# 开启GTID复制
gtid_mode=on
# 跳过一些可能导致执行出错的SQL语句
enforce-gtid-consistency=true
binlog_checksum=NONE
log_bin=binlog

#binlog格式,MGR要求必须是ROW,不过就算不是MGR,也最好用row
binlog_format=row
#MGR使用乐观锁,所以官网建议隔离级别是RC,减少锁粒度
transaction_isolation = READ-COMMITTED
#因为集群会在故障恢复时互相检查binlog的数据,
#所以需要记录下集群内其他服务器发过来已经执行过的binlog,按GTID来区分是否执行过.
log-slave-updates=1
#binlog校验规则,5.6之后的高版本是CRC32,低版本都是NONE,但是MGR要求使用NONE
binlog_checksum=NONE
#基于安全的考虑,MGR集群要求复制模式要改成slave记录记录到表中,不然就报错
master_info_repository=TABLE
#同上配套
relay_log_info_repository=TABLE
 
#组复制设置
#记录事务的算法,官网建议设置该参数使用 XXHASH64 算法
transaction_write_set_extraction = XXHASH64
plugin_load_add='group_replication=group_replication.so'
#相当于此GROUP的名字,是UUID值,不能和集群内其他GTID值的UUID混用,可用uuidgen来生成一个新的,
#主要是用来区分整个内网里边的各个不同的GROUP,而且也是这个group内的GTID值的UUID
loose-group_replication_group_name = '5dbabbe6-8050-49a0-9131-1de449167446'
#IP地址白名单,默认只添加127.0.0.1,不会允许来自外部主机的连接,按需安全设置
loose-group_replication_ip_whitelist = '127.0.0.1/8,192.168.171.0/24'
#是否随服务器启动而自动启动组复制,不建议直接启动,怕故障恢复时有扰乱数据准确性的特殊情况
loose-group_replication_start_on_boot = OFF
#本地MGR的IP地址和端口，host:port,是MGR的端口,不是数据库的端口
loose-group_replication_local_address = 'ys3:33081'
#需要接受本MGR实例控制的服务器IP地址和端口,是MGR的端口,不是数据库的端口
loose-group_replication_group_seeds = 'ys1:33081,ys2:33081,ys3:33081'
#开启引导模式,添加组成员，用于第一次搭建MGR或重建MGR的时候使用,只需要在集群内的其中一台开启,
loose-group_replication_bootstrap_group = OFF
loose-group_replication_recovery_retry_count=31536000
#是否启动单主模式，如果启动，则本实例是主库，提供读写，其他实例仅提供读,如果为off就是多主模式了
loose-group_replication_single_primary_mode = ON
#多主模式下,强制检查每一个实例是否允许该操作,如果不是多主,可以关闭
loose-group_replication_enforce_update_everywhere_checks = off

report_host=192.168.171.163
report_port=3306
loose-group_replication_recovery_get_public_key=ON


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

loose-plugin_load = "rpl_semi_sync_master=semisync_master.so;rpl_semi_sync_slave=semisync_slave.so"
rpl_semi_sync_master_enabled=1   #开启半同步
rpl_semi_sync_master_timeout=1000  #定义半同步超时时间
rpl_semi_sync_slave_enabled=1

[client]
socket = /usr/local/mysql/mysql.sock
default-character-set=utf8mb4
```



可以发现配置文件 `/etc/my.cnf` 主要是`server_id`、`report_host`和`loose-group_replication_local_address`不同

重启mysql



## 四、增加MGR复制用户(三台)

 

```
SET SQL_LOG_BIN=0;

CREATE USER mgruser@'%' IDENTIFIED BY'mgruser';

GRANT REPLICATION SLAVE ON *.* TO mgruser@'%';

FLUSH PRIVILEGES;

SET SQL_LOG_BIN=1;

CHANGE MASTER TO MASTER_USER='mgruser',MASTER_PASSWORD='mgruser' FOR CHANNEL 'group_replication_recovery';

install PLUGIN group_replication SONAME'group_replication.so';


show plugins; #列出插件
```

![image-20240129165339684](\img\springBoot\image-20240129165339684.png)



## 五、MGR单主模式和多主模式



### 1、单主模式配置

主节点配置如下

```
SET GLOBAL group_replication_bootstrap_group=ON;
START GROUP_REPLICATION;
SET GLOBAL group_replication_bootstrap_group=OFF;
```

查看mgr组信息

```
SELECT * FROM performance_schema.replication_group_members;
```

另两个从节点

```
START GROUP_REPLICATION;
```

![image-20240129170825261](\img\springBoot\image-20240129170825261.png)



### 2、多主模式配置

从上面切换到多主模式

停止复制并修改相关配置，MGR切换模式需要重新启动组复制，因些需要在所有节点上先关闭组复制，设置 group_replication_single_primary_mode=OFF 等参数，再启动组复制。

停止组复制(所有节点执行)：

```
stop group_replication;

set global group_replication_single_primary_mode=OFF;

set global group_replication_enforce_update_everywhere_checks=ON;
```

随便选择某个节点执行

```
SET GLOBAL group_replication_bootstrap_group=ON; 

START GROUP_REPLICATION; 

SET GLOBAL group_replication_bootstrap_group=OFF;
```

 其他节点执行

```
START GROUP_REPLICATION; 

SELECT * FROM performance_schema.replication_group_members; 

SHOW STATUS LIKE 'group_replication_primary_member';
```

![image-20240129172115301](\img\springBoot\image-20240129172115301.png)



搭建成功！！！！
