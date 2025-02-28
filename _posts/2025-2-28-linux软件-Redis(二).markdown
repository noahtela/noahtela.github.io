---
layout:     post
title:      "linux软件-Redis(二)"
subtitle:   " \"linux\""
date:       2025-2-28 16:28:12
author:     "yangsir"
header-img: "img/bg-material.jpg"
catalog: true
tags:
    - 笔记
    - linux学习
    - 云原生


---

> “本节主要介绍Redis的安装及基本配置 ”


<p id = "build"></p>

# linux软件-Redis(二)



（Redis官网地址 https://redis.io）

![image-20250228142114530](\img\linux\image-20250228142114530.png)

## 安装Redis

### 一、上传软件包

![image-20250228143445066](\img\linux\image-20250228143445066.png)

```
tar -zxvf redis-5.0.5.tar.gz
```



### 二、安装gcc

```shell
yum install -y gcc
```





### 三、编译安装

```
cd redis-5.0.5

make

cd src/

make install PREFIX=/usr/local/redis
```

![image-20250228150314874](\img\linux\image-20250228150314874.png)



### 四、移动配置文件到指定位置

```
cd ..
mkdir /usr/local/redis/etc
cp redis.conf /usr/local/redis/etc/
```



### 五、启动redis服务

```
/usr/local/redis/bin/redis-server /usr/local/redis/etc/redis.conf
```

![image-20250228150625780](\img\linux\image-20250228150625780.png)



 以上警告信息的解决方法：

 执行ulimit -n查看当前用户打开的最大文件数

```
ulimit -n
```

![image-20250228150705201](\img\linux\image-20250228150705201.png)

 修改/etc/security/limits.conf文件，在文件末尾添加下面的两行：

```
*                soft    nofile         10032
*                hard    nofile         10032
```

修改/etc/pam.d/login文件，在文件末尾添加下面的内容：

```
session    required     /usr/lib64/security/pam_limits.so
```

 重新登录使修改生效

 在/etc/sysctl.conf文件中添加下面的两行内容：

```
net.core.somaxconn = 511

vm.overcommit_memory = 1
```

 执行sysctl -p使内核参数修改生效

 执行下面的命令：

```
echo never > /sys/kernel/mm/transparent_hugepage/enabled
```

 再次启动redis服务：

```
/usr/local/redis/bin/redis-server /usr/local/redis/etc/redis.conf
```

![image-20250228151313626](\img\linux\image-20250228151313626.png)

注：默认redis服务是在前台终端运行

### 六、后台启动redis

默认情况，Redis不是在后台运行，我们需要把redis放在后台运行

```
#vim /usr/local/redis/etc/redis.conf

daemonize yes   #修改no为yes
bind 127.0.0.1 192.168.109.54   #默认监控127.0.0.1 添加本机IP 192.168.109.54


/usr/local/redis/bin/redis-server /usr/local/redis/etc/redis.conf
```


七、停止redis实例

```
/usr/local/redis/bin/redis-cli shutdown
```

或

```
pkill redis-server
```

### 七、/usr/local/redis/bin目录下的几个文件是什么

- redis-benchmark：redis性能测试工具，测试Redis在你的系统及你的配置下的读写性能
- redis-check-aof：检查aof日志的工具
- redis-check-rdb：检查rdb日志的工具
- redis-cli：连接用的客户端
- redis-server：redis服务进程

### 八、添加path环境变量

```
ln -s /usr/local/redis/bin/* /usr/local/bin/
```

### 九、添加开机自启动

```
chmod +x /etc/rc.d/rc.local
echo " /usr/local/redis/bin/redis-server /usr/local/redis/etc/redis.conf" >> /etc/rc.d/rc.local
```

### 十、配置文件详解

```
#是否作为守护进程运行

daemonize yes

#如以后台进程运行，则需指定一个pid，默认为/var/run/redis.pid

pidfile redis.pid

#绑定主机IP，默认值为127.0.0.1

#bind 127.0.0.1

#Redis默认监听端口

port 6379

#客户端闲置多少秒后，断开连接，默认为300（秒）

timeout 300

#日志记录等级，有4个可选值，debug，verbose（默认值），notice，warning

loglevel verbose

#指定日志输出的文件名，默认值为stdout，也可设为/dev/null屏蔽日志

logfile stdout

#可用数据库数，默认值为16，默认数据库为0

databases 16

#保存数据到disk的策略

#当至少有一条Key数据被改变时，900秒刷新到disk一次

save 900 1

#当至少有10条Keys数据被改变时，300秒刷新到disk一次

save 300 10

#当至少有1w条keys数据被改变时，60秒刷新到disk一次

save 60 10000

#当dump .rdb数据库的时候是否压缩数据对象

rdbcompression yes

#存储和加载rdb文件时校验

rdbchecksum yes    

#本地数据库文件名，默认值为dump.rdb

dbfilename dump.rdb

#后台存储错误停止写。

stop-writes-on-bgsave-error yes 

#本地数据库存放路径，默认值为 ./

dir /var/lib/redis/

########### Replication #####################

#Redis的复制配置

# replicaof <masterip> <masterport> 当本机为从服务时，设置主服务的IP及端口

# masterauth <master-password> 当本机为从服务时，设置主服务的连接密码

#连接密码

# requirepass foobared

#最大客户端连接数，默认不限制

# maxclients 128

#最大内存使用设置，达到最大内存设置后，Redis会先尝试清除已到期或即将到期的Key，当此方法处理后，一旦到达最大内存设置，将无法再进行写入操作。

# maxmemory <bytes>

#是否在每次更新操作后进行日志记录，如果不开启，可能会在断电时导致一段时间内的数据丢失。因为redis本身同步数据文件是按上面save条件来同步的，所以有的数据会在一段时间内只存在于内存中。默认值为no

appendonly no

#更新日志文件名，默认值为appendonly.aof

#appendfilename

#更新日志条件，共有3个可选值。no表示等操作系统进行数据缓存同步到磁盘，always表示每次更新操作后调用fsync()将数据写到磁盘，everysec表示每秒同步一次（默认值）。

# appendfsync always

appendfsync everysec

# appendfsync no

#当slave失去与master的连接，或正在拷贝中，如果为yes，slave会响应客户端的请求，数据可能不同步甚至没有数据，如果为no，slave会返回错误"SYNC with master in progress"

replica -serve-stale-data yes

#如果为yes，slave实例只读，如果为no，slave实例可读可写。

replica -read-only yes    

# 在slave和master同步后（发送psync/sync），后续的同步是否设置成TCP_NODELAY . 假如设置成yes，则redis会合并小的TCP包从而节省带宽，但会增加同步延迟（40ms），造成master与slave数据不一致  假如设置成no，则redis master会立即发送同步数据，没有延迟

repl-disable-tcp-nodelay no

#如果master不能再正常工作，那么会在多个slave中，选择优先值最小的一个slave提升为master，优先值为0表示不能提升为master。

replica-priority 100
#### LIMITS ####

maxclients 10000    #客户端并发连接数的上限是10000，到达上限，服务器会关闭所有新连接并返回错误"max number of clients reached"

maxmemory 15G    #设置最大内存，到达上限，服务器会根据驱逐政策(eviction policy)删除某些键值，如果政策被设置为noeviction，那么redis只读，对于增加内存的操作请求返回错误。

#### APPEND ONLY MODE ####

appendonly no    #redis默认采用快照(snapshotting)异步转存到硬盘中，它是根据save指令来触发持久化的，当Redis异常中断或停电时，可能会导致最后一些写操作丢失。AOF(Append Only File，只追加文件)可以提供更好的持久性，结合apendfsync指令可以把几分钟的数据丢失降至一秒钟的数据丢失，它通过日志把所有的操作记录下来，AOF和RDB持久化可以同时启动。

appendfilename appendonly.aof    #指定aof的文件名。

apendfsync always|everysec|no    #调用fsync()写数据到硬盘中，always是每一次写操作就马上同步到日志中，everysec是每隔一秒强制fsync，no是不调用fsync()，让操作系统自己决定何时同步。

no-appendfsync-on-rewrite no    #如果为yes，当BGSAVE或BGREWRITEAOF指令运行时，即把AOF文件转写到RDB文件中时，会阻止调用fsync()。

auto-aof-rewrite-percentage 100

auto-aof-rewrite-min-size 64mb    #Redis会将AOF文件最初的大小记录下来，如果当前的AOF文件的大小增加100%并且超过64mb时，就会自动触发Redis改写AOF文件到RDB文件中，如果auto-aof-rewrite-percentage为0表示取消自动rewrite功能。

#### LUA SCRIPTING ####

lua-time-limit 5000    #一个Lua脚本最长的执行时间为5000毫秒（5秒），如果为0或负数表示无限执行时间。

#### SLOW LOG ####

slowlog-log-slower-than 10000    #当某个请求执行时间（不包括IO时间）超过10000微妙（10毫秒），把请求记录在慢日志中 ，如果为负数不使用慢日志，如果为0强制记录每个指令。

slowlog-max-len 128    #慢日志的最大长度是128，当慢日志超过128时，最先进入队列的记录会被踢出来，慢日志会消耗内存，你可以使用SLOWLOG RESET清空队列回收这些内存。

#### ADVANCED CONFIG ####

hash-max-ziplist-entries 512

hash-max-ziplist-value 64    #较小的hash可以通过某种特殊的方式进行编码，以节省大量的内存空间，我们指定最大的条目数为512，每个条目的最大长度为64。

list-max-ziplist-entries 512

list-max-ziplist-value 64    #同上。

zset-max-ziplist-entries 128

zset-max-ziplist-value 64    #同上。

activerehashing yes    #重新哈希the main Redis hash table(the one mapping top-level keys to values)，这样会节省更多的空间。

client-output-buffer-limit normal 0 0 0    #对客户端输出缓冲进行限制可以强迫那些就不从服务器读取数据的客户端断开连接。对于normal client，第一个0表示取消hard limit，第二个0和第三个0表示取消soft limit，normal client默认取消限制，因为如果没有寻问，他们是不会接收数据的。

client-output-buffer-limit slave 256mb 64mb 60    #对于slave client和MONITER client，如果client-output-buffer一旦超过256mb，又或者超过64mb持续60秒，那么服务器就会立即断开客户端连接。

client-output-buffer-limit pubsub 32mb 8mb 60    #对于pubsub client，如果client-output-buffer一旦超过32mb，又或者超过8mb持续60秒，那么服务器就会立即断开客户端连接。

#### INCLUDES ####

include /path/to/conf    #包含一些可以重用的配置文件。

hz 10  #Redis 调用内部函数来执行后台task，比如关闭已经timeout连接，删除过期的keys并且永远不会被访问到的，执行频率根据 hz 后面的值来确定。在Redis 比较空闲的时候，提高这个值，能充分利用CPU，让Redis相应速度更快，可取范围是1-500 ，建议值为 1--100

aof-rewrite-incremental-fsync yes  # 当子进程重写AOF文件，以下选项开启时，AOF文件会每产生32M数据同步一次。这有助于更快写入文件到磁盘避免延迟

################ VIRTUAL MEMORY ###########

#是否开启VM功能，默认值为no

vm-enabled no

# vm-enabled yes

#虚拟内存文件路径，默认值为/tmp/redis.swap，不可多个Redis实例共享

vm-swap-file /tmp/redis.swap

#将所有大于vm-max-memory的数据存入虚拟内存,无论vm-max-memory设置多小,所有索引数据都是内存存储的 (Redis的索引数据就是keys),也就是说,当vm-max-memory设置为0的时候,其实是所有value都存在于磁盘。默认值为0。

vm-max-memory 0

vm-page-size 32

vm-pages 134217728

vm-max-threads 4

############# ADVANCED CONFIG ###############

glueoutputbuf yes

hash-max-zipmap-entries 64

hash-max-zipmap-value 512

#是否重置Hash表

activerehashing yes
```



### 十一、redis数据存储

 redis的存储分为内存存储、磁盘存储和log文件三部分，配置文件中有三个参数对其进行配置。

 **save seconds updates:save**配置，指出在多长时间内，有多少次更新操作，就将数据同步到数据文件。这个可以多个条件配合，比如默认配置文件中的设置，就设置了三个条件。

 **appendonly yes/no :appendonly**配置，指出是否在每次更新操作后进行日志记录，如果不开启，可能会在断电时导致一段时间内的数据丢失。因为redis本身同步数据文件是按上面的save条件来同步的，所以有的数据会在一段时间内只存在于内存中。

 **appendfsync no/always/everysec :appendfsync**配置，no表示等操作系统进行数据缓存同步到磁盘，always表示每次更新操作后调用fsync()将数据写到磁盘，everysec表示每秒同步一次。



### 十二、redis认证设置

1)修改redis.conf配置文件

```shell
vim /usr/local/redis/etc/redis.conf

# requirepass foobared		//启用此项，并指定密码即可
requirepass password
```

2)重启redis

```
redis-cli shutdown
redis-server /usr/local/redis/etc/redis.conf
```





## 下节预告-Redis基本命令



