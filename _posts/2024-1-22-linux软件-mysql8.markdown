---

layout:     post
title:      "linux软件-mysql8.0"
subtitle:   " \"linux\""
date:       2024-1-26 13:16:49
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

# MYSQL8.0



MySQL 8.0是一个开源的关系型数据库管理系统，由Oracle公司开发。这个版本在2018年4月正式发布，相较于前一个版本MySQL 5.7，MySQL 8.0带来了很多新的特性和改进。

1. 数据字典：MySQL 8.0引入了一个事务性的数据字典，这使得元数据在系统崩溃后能够恢复，从而提高了系统的可靠性。
2. 角色管理：MySQL 8.0引入了角色的概念，可以将一组权限赋予给一个角色，然后将角色赋予给用户，这样可以更方便地管理用户权限。
3. 支持窗口函数：窗口函数可以在结果集的每一行上进行计算，这对于进行复杂查询和数据分析非常有用。
4. 支持公共表表达式（CTE）：公共表表达式可以使得查询语句更加清晰和易于理解。
5. 支持Unicode 9.0：这使得MySQL能够支持更多的字符集，包括各种表情符号。
6. 性能提升：MySQL 8.0在查询优化器、InnoDB存储引擎、复制等方面做了大量的性能优化，使得系统的性能得到了显著提升。
7. 支持JSON：MySQL 8.0提供了对JSON数据类型的原生支持，使得MySQL能够更好地处理半结构化数据。
8. 支持GIS：MySQL 8.0提供了对地理信息系统（GIS）的支持，可以进行地理空间数据的存储和查询。
9. 安全性增强：MySQL 8.0在安全性方面做了很多改进，例如支持密码旋转策略、提供数据脱敏功能等。
10. 支持Invisible Index：这个特性允许管理员在不影响现有查询的情况下测试索引的影响，有助于数据库的性能调优。





## 一、下载mysql并安装初始化

### 1、下载

```shell
wget https://downloads.mysql.com/archives/get/p/23/file/mysql-8.0.26-linux-glibc2.12-x86_64.tar.xz
```



### 2、解压

```shell
tar xvJf mysql-8.0.26-linux-glibc2.12-x86_64.tar.xz -C /usr/local/
```

### 3 、创建符号连接 

```
cd /usr/local/
ln -s /usr/local/mysql-8.0.26-linux-glibc2.12-x86_64 /usr/local/mysql
```

### 4、创建data文件夹存储文件

```
cd /usr/local/mysql
mkdir data 
```

### 5、创建用户组以及用户和密码

```
groupadd  mysql
useradd -s /sbin/nologin -g mysql mysql
```

### 6、授权用户

```
chown -R mysql.mysql /usr/local/mysql-8.0.26-linux-glibc2.12-x86_64/
```

### 7、编辑my.cnf文件 

```shell
#vi /etc/my.cnf


[mysqld]
basedir = /usr/local/mysql
datadir = /usr/local/mysql/data
socket = /usr/local/mysql/mysql.sock
character-set-server=utf8mb4
port = 3306
sql_mode=NO_ENGINE_SUBSTITUTION,STRICT_TRANS_TABLES
[client]
socket = /usr/local/mysql/mysql.sock
default-character-set=utf8mb4

#chmod +x /etc/my.cnf
```

### 8、初始化基础信息

```shell
./bin/mysqld --user=mysql --basedir=/usr/local/mysql --datadir=/usr/local/mysql/data --initialize
#得到临时密码
```



### 9、添加mysqld服务到系统

```shell
cp -a ./support-files/mysql.server /etc/init.d/mysql
```

### 10、授权以及添加服务

```shell
chmod +x /etc/init.d/mysql
chkconfig --add mysql
```

### 11、启动mysql

```shell
service mysql start

service mysql status
```

### 12、将mysql命令添加到服务

```shell
ln -s /usr/local/mysql/bin/mysql /usr/bin
```

### 13、登录mysql mysql -uroot -p 密码使用之前随机生成的密码

虽然登录成功，但是不能对数据库进行操作，必须先修改密码

![3747739c7f9a618bdafb872c4b2de02](\img\springBoot\3747739c7f9a618bdafb872c4b2de02.png)



### 14、修改root密码 其中123456是新的密码自己设置

```
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '123456';

flush privileges; #执行使密码生效
```

### 15、选择mysql数据库 

```
use mysql;
```

### 16、修改远程连接并生效    

```
update user set host='%' where user='root';

flush privileges;
```

![ebcc8af835101aa30c4361b1876df5f](\img\springBoot\ebcc8af835101aa30c4361b1876df5f.png)

![0b9a1a10aa31d081e91b67ef93d0229](\img\springBoot\0b9a1a10aa31d081e91b67ef93d0229.png)



数据库已经可以正常登录和使用





## 二、mysql优化

### 1、mysql最大连接数

#### 1）查看MySQL最大连接数

```
show variables like '%max_connections%';
```

![image-20240126152649670](\img\springBoot\image-20240126152649670.png)

（默认就是151）

mysqlx_max_connections是MySQL 8.0版本中的一个系统变量，它用于设置MySQL X Protocol（X协议）的最大连接数。MySQL X Protocol是MySQL的一种新的协议，它基于WebSocket和JSON，用于提供无状态、基于文档的访问MySQL的功能。

### 2）修改MySQL最大连接数

修改mysql的配置文件

```
vi /etc/my.cnf

#添加下面两行
max_connections=65535
mysqlx_max_connections=65535

```

再次查询

```
mysql> show variables like '%max_connections%';
+------------------------+-------+
| Variable_name          | Value |
+------------------------+-------+
| max_connections        | 65535 |
| mysqlx_max_connections | 65535 |
+------------------------+-------+
2 rows in set (0.01 sec)
```



### 2、修改MySQL线程池数量(thread_cache_size)

#### 1)查看Mysql线程缓存池数量

```
mysql> show variables like '%thread_cache_size%';
+-------------------+-------+
| Variable_name     | Value |
+-------------------+-------+
| thread_cache_size | 100   |
+-------------------+-------+
1 row in set (0.01 sec)

```

#### 2)什么是Mysql线程缓存池数量

MySQL线程缓存池数量是指MySQL服务器为减少线程创建和销毁的开销，而设置的一个线程缓存池。当客户端断开连接时，如果该连接对应的线程在缓存池中的线程数已满，那么该线程会被立即销毁；否则，该线程会被放入缓存池中，供后续新的客户端连接使用。

线程缓存池的大小可以通过系统变量thread_cache_size来设置。如果服务器的连接数非常高，增大这个值可能会提高系统的性能。

需要注意的是，线程缓存仅对短连接有效，如果是长连接，线程缓存无法发挥作用。

#### 3)修改配置文件

```
vi /etc/my.cnf
#添加
thread_cache_size=16384
```

### 3、MySQL8 整体配置实例

```
[mysqld]
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

[client]
socket = /usr/local/mysql/mysql.sock
default-character-set=utf8mb4
```

## 三、MySQL主从服务器搭建



### 1、安装半同步复制

半同步复制是通过插件的形式实现的。必须要在源库和副本上安装插件。源库和副本有不同的插件。插件安装后，可通过与之相关的系统变量对其进行控制。只有安装了相关插件，这些系统变量才可用。



#### 1)检查 have_dynamic_loading 系统变量的值是否为 YES。二进制发行版应支持动态加载。

```
mysql> show variables like '%have_dynamic_loading%';
+----------------------+-------+
| Variable_name        | Value |
+----------------------+-------+
| have_dynamic_loading | YES   |
+----------------------+-------+
1 row in set (0.00 sec)
```

#### 2)源库安装

```
mysql> INSTALL PLUGIN rpl_semi_sync_master SONAME 'semisync_master.so';
Query OK, 0 rows affected, 1 warning (0.02 sec)
```

#### 3)从库安装

```
mysql> INSTALL PLUGIN rpl_semi_sync_slave SONAME 'semisync_slave.so';
Query OK, 0 rows affected, 1 warning (0.01 sec)
```

#### 4)确认安装是否成功

```
mysql> SELECT PLUGIN_NAME, PLUGIN_STATUS
    ->        FROM INFORMATION_SCHEMA.PLUGINS
    ->        WHERE PLUGIN_NAME LIKE '%semi%';
+----------------------+---------------+
| PLUGIN_NAME          | PLUGIN_STATUS |
+----------------------+---------------+
| rpl_semi_sync_master | ACTIVE        |
| rpl_semi_sync_slave  | ACTIVE        |
+----------------------+---------------+
2 rows in set (0.01 sec)
```

#### 5）修改配置文件，使得半同步生效

```
#在配置文件中添加
#主
rpl_semi_sync_master_enabled=1

#从
rpl_semi_sync_slave_enabled=1
```

#### 6）查看是否生效

```
SHOW STATUS LIKE 'Rpl_semi_sync%';
```

![image-20240126160817826](\img\springBoot\image-20240126160817826.png)



### 2、搭建主从服务器

（注意：克隆的虚拟机，要修改mysql的uuid   删除该文件`/usr/local/mysql/data/auto.cnf`）

#### 1）配置时钟服务器，同步数据库时间

```
yum -y install chrony
//注释pool 2.centos.pool.ntp.org iburst
sed -i 's/^pool pool.ntp.org iburst/#&/' /etc/chrony.conf
//添加三个阿里云NTP的服务器
echo -e "server ntp1.aliyun.com iburst\nserver ntp2.aliyun.com iburst\nserver ntp3.aliyun.com iburst" | sudo tee -a /etc/chrony.conf


systemctl restart chronyd
chronyc sourcestats -v

systemctl enable chronyd --now  #设为开机自启
```

####  2）数据同步方式

通过以下命令可以查看当前数据库的binglog模式show global variables like %binlog format%;
/etc/my.cnf配置文件设置参数如下:[myslqd]
10g-bin=mysq1-bin
#语句模式#binlog format="STATEMENT"#行模式
#binlog format="ROW"
#binlog format="MIXED'
#自动模式
binlog日志三种工作模式:STATEMENT:基于语句的复制。在服务器上执行sq1语句，在从服务器上执行同样的语句,mysq1默认采用基于语句的复制。基于行的复制。把改变的内容复制过去，而不是把命命在从服务器上执行一遍。ROW:混合类型的复制。默认采用基于语句的复制，一旦发现基于语句无法精确复制时，就会采用基于行的复制。
MIXED:
Binlog 复制模式:
异步复制(Asynchronous replication)全同步复制(Fu1ly synchronous replication)
半同步复制(Semisynchronous replication)
MGR 组复制(MySQL Group Replication，简称MGR是半同步复制改进版本)并行复制 无损复制 mysq15.7之后的版本



#### 3)修改配置文件

```
server-id = 2 #服务器 id，随意，但要唯一

log-bin=mysql-bin #开启二进制日志

binlog-format=mixed #指定日志格式

log_bin = /usr/local/mysql/data/mysql-bin.log

read-only = 1 #[可选] 0（默认）表示读写（主机），1表示只读（从机）

binlog_expire_logs_seconds = 2592000  #设置日志文件保留的时长，单位是秒

max_binlog_size = 100M #控制单个二进制日志大小。此参数的最大和默认值是1GB

replicate_do_db = work #待同步的数据库日志

replicate_ignore_db = mysql,sys #不同步的数据库日志
```



#### 4）重启查看状态

```
mysql> show master status\G
*************************** 1. row ***************************
             File: mysql-bin.000001
         Position: 156
     Binlog_Do_DB: 
 Binlog_Ignore_DB: 
Executed_Gtid_Set: 
1 row in set (0.00 sec)
```

#### 5)创建数据复制用户

```
#注意ip是从服务器的ip
CREATE USER 'copyuser'@'192.168.171.159' IDENTIFIED WITH mysql_native_password BY '123456';

#给主从复制账号授权
grant replication slave on *.* to 'copyuser'@'192.168.171.159';


flush privileges;

```

```
mysql> show master status\G
*************************** 1. row ***************************
             File: mysql-bin.000001
         Position: 854
     Binlog_Do_DB: 
 Binlog_Ignore_DB: 
Executed_Gtid_Set: 
1 row in set (0.00 sec)
```



#### 6)从服务器指定主服务器

```
stop slave
```

```
change master to master_host='192.168.171.151',master_user='copyuser',master_password='123456',master_log_file='mysql-bin.000001',master_log_pos=854;
```



```
flush privileges;

start slave;
```

#### 7)查看状态

```
mysql> show slave status\G
*************************** 1. row ***************************
               Slave_IO_State: Waiting for source to send event
                  Master_Host: 192.168.171.151
                  Master_User: copyuser
                  Master_Port: 3306
                Connect_Retry: 60
              Master_Log_File: mysql-bin.000001
          Read_Master_Log_Pos: 854
               Relay_Log_File: localhost-relay-bin.000002
                Relay_Log_Pos: 324
        Relay_Master_Log_File: mysql-bin.000001
             Slave_IO_Running: Yes
            Slave_SQL_Running: Yes
              Replicate_Do_DB: 
          Replicate_Ignore_DB: 
           Replicate_Do_Table: 
       Replicate_Ignore_Table: 
      Replicate_Wild_Do_Table: 
  Replicate_Wild_Ignore_Table: 
                   Last_Errno: 0
                   Last_Error: 
                 Skip_Counter: 0
          Exec_Master_Log_Pos: 854
              Relay_Log_Space: 537
              Until_Condition: None
               Until_Log_File: 
                Until_Log_Pos: 0
           Master_SSL_Allowed: No
           Master_SSL_CA_File: 
           Master_SSL_CA_Path: 
              Master_SSL_Cert: 
            Master_SSL_Cipher: 
               Master_SSL_Key: 
        Seconds_Behind_Master: 0
Master_SSL_Verify_Server_Cert: No
                Last_IO_Errno: 0
                Last_IO_Error: 
               Last_SQL_Errno: 0
               Last_SQL_Error: 
  Replicate_Ignore_Server_Ids: 
             Master_Server_Id: 1
                  Master_UUID: 2af8759b-bc04-11ee-9c94-000c299d0072
             Master_Info_File: mysql.slave_master_info
                    SQL_Delay: 0
          SQL_Remaining_Delay: NULL
      Slave_SQL_Running_State: Replica has read all relay log; waiting for more updates
           Master_Retry_Count: 86400
                  Master_Bind: 
      Last_IO_Error_Timestamp: 
     Last_SQL_Error_Timestamp: 
               Master_SSL_Crl: 
           Master_SSL_Crlpath: 
           Retrieved_Gtid_Set: 
            Executed_Gtid_Set: 
                Auto_Position: 0
         Replicate_Rewrite_DB: 
                 Channel_Name: 
           Master_TLS_Version: 
       Master_public_key_path: 
        Get_master_public_key: 0
            Network_Namespace: 
1 row in set, 1 warning (0.00 sec)

```

​       显示  Slave_IO_Running: Yes，Slave_SQL_Running: Yes，则数据同步成功。



#### 8)验证

![image-20240126165944020](\img\springBoot\image-20240126165944020.png)

![image-20240126170016514](\img\springBoot\image-20240126170016514.png)

![image-20240126171320473](\img\springBoot\image-20240126171320473.png)

![image-20240126170204089](\img\springBoot\image-20240126170219386.png)



成功！！！！！！！
