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
