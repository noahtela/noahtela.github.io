---
layout:     post
title:      "linux软件-mysql拓展知识二"
subtitle:   " \"linux\""
date:       2024-2-29 17:22:49
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

# MySQL拓展知识二



## 一、数据导入与导出



 使用mysql提供的mysqldump工具来导入导出数据库，可以实现数据库的备份和还原。



### 1、导出数据库 (备份数据库)

```
mysqldump -u 用户名 -p 数据库名 > 导出的文件名
```



### 2、导入数据库 (还原数据库)

（1）在shell命令行执行导入命令，不推荐,暴露了密码,一般这种方法用于脚本自动化执行比较多



```
mysqldump -u 用户名 -p 数据库名 < 导出的文件名
```



（2） 进入到mysql数据库，添加sql文件，推荐使用此方法,安全



```
 mysql> create database HA2;
 mysql> use HA2;
 mysql> source /root/HA.sql     #sql脚本的路径
 mysql> show tables;
```



## 二、mysql二进制日志



mysql的二进制日志记录着数据库的所有增、删、改等操作日志(前提是要在自己的服务器上开启binlog)，还包括了这些操作的执行时间。为了显示这些二进制内容，我们可以使用mysqlbinlog命令来查看



 Binlog的用途：

 1：主从同步

 2：恢复数据库



 执行`show variables like 'log_bin%';`查看binlog是否开启



 通过编辑my.cnf中的log-bin选项可以开启二进制日志，形式如下：

```
 log-bin [=DIR/[filename]]

 例如：log-bin=/data/mysql/log/mysql-bin
```

 其中，DIR参数指定二进制文件的存储路径；filename参数指定二级制文件的文件名，其形式为filename.number，number的形式为000001、000002、……等。每次重启mysql服务或运行mysql> flush logs;都会生成一个新的二进制日志文件，这些日志文件的number会不断地递增。除了生成上述的文件外还会生成一个名为filename.index的文件，这个文件中存储所有二进制日志文件的清单又称为二进制文件的索引。

 开启binary log功能：

 修改/etc/my.cnf配置文件，添加如下内容：

```
 log-bin=/data/mysql/log/mysql_bin

 server-id=1
```



查看binlog日志文件列表：



```
show binary logs;
```





查看当前使用的二进制文件及日志文件中事件当前位置：

```
show master status
```





重新开始一个新的日志文件：(可用于数据恢复)

```
flush logs;
```







 利用二进制日志可实现基于时间点和位置的恢复。例如，由于误操作删除了一行数据，这时完全恢复是没有用的，因为日志里面还存在误操作的语句，我们需要的是恢复到误操作前的状态，然后跳过误操作的语句，再恢复后面操作的语句。

 先对test_db数据库执行mysqldump做完全备份：

 \# mysqldump -uroot -p123456 --single-transaction --flush-logs -B test_db > /opt/mysql_backup/test_db_$(date +%Y%m%d%H%M%S).sql

 生成的编号是000019的二进制文件用于保存完全备份之后的数据库操作的记录。                               

 假定需要往数据库中插入两条数据，但由于误操作，两条插入语句中间删除了一条数据，而这条数据是不应该删除的。

 delete from student_info where 学号=2;

 

 mysql> insert into stu values(120,'li20');

 mysql> delete from stu where sid=104;

 mysql> insert into stu values(121,'li21');

 mysql> select * from stu;

```
 +-----+-------+

 | sid | NAME |

 +-----+-------+

 | 101 | aiden |

 | 102 | li1  |

 | 103 | li2  |

 | 120 | li120 |

 | 121 | li121 |

 +-----+-------+
  5 rows in set (0.00 sec)
```

 上面的模拟的误操作是删除了sid=104的记录

 编号为000019的二进制文件中保存了正确的插入语句，同时也保存了不应该执行的删除语句。

 使用mysqlbinlog命令可以查看binlog文件中误删除语句delete的开始时间/位置和结束时间/位置

 [root@localhost ~]# mysqlbinlog -v /data/mysql/log/mysql-bin.000019

 我的当前实例为binlog.000007

 mysqlbinlog --no-defaults -v /usr/local/mysql/data/binlog.000007

 ………省略内容

 \# at 490

 \#190710 14:16:21 server id 1 end_log_pos 567 CRC32 0xae536a14     Query   thread_id=29 exec_time=0  error_code=0

 SET TIMESTAMP=1562739381/*!*/;

 BEGIN

 /*!*/;

 \# at 567

 \#190710 14:16:21 server id 1 end_log_pos 619 CRC32 0x29035efe    Table_map: `test_db`.`stu` mapped to number 988

 \# at 619

 \#190710 14:16:21 server id 1 end_log_pos 663 CRC32 0xe0915a8f    Delete_rows: table id 988 flags: STMT_END_F

 BINLOG '

 tYIlXRMBAAAANAAAAGsCAAAAANwDAAAAAAMAB3Rlc3RfZGIAA3N0dQACAw8ClgAA/l4DKQ==

 tYIlXSABAAAALAAAAJcCAAAAANwDAAAAAAEAAgAC//xoAAAAA2xpM49akeA=

 '/*!*/;

 \### DELETE FROM `test_db`.`stu`

 \### WHERE

 \###  @1=104

 \###  @2='li3'

 \# at 663

 \#190710 14:16:21 server id 1 end_log_pos 694 CRC32 0xea437bf6    Xid = 9145

 COMMIT/*!*/;

 通过mysqlbinlog命令所显示的结果可以看到误操作delete的开始位置是at 490，开始时间是#190710 14:16:21，结束位置是at 663，结束时间是#190710 14:16:21。

 下面演示基于位置的恢复方法。

 [root@localhost ~]# mysqladmin -uroot -p123456 flush-logs  //分割日志

 mysql> set sql_log_bin=0;   //临时关闭binlog功能

 先使用mysql命令进行完全备份的回复操作：

 \# mysql -uroot -p123456 test_db < /opt/mysql_backup/test_db_20190709222604.sql

 登录数据库查看完全备份的数据恢复结果

 mysql> use test_db;

 mysql> select * from stu;

```
 +-----+-------+

 | sid | NAME |

 +-----+-------+

 | 101 | aiden |

 | 102 | li1  |

 | 103 | li2  |

 | 104 | li3  |

 +-----+-------+

 4 rows in set (0.00 sec)
```

 接下来使用二进制日志继续恢复数据

 1、基于时间点的恢复，就是将某个起始时间的二进制日志导入数据库中，从而跳过某个发生错误的时间点实现数据的恢复，使用mysqlbinlog加上--stop-datetime选项，表示从二进制日志中读取指定时间之前的日志事件，后面误操作的语句不执行，--start-datetime选项表示从二进制日志中读取指定时间之后的日志事件。

 需要注意的是，二进制文件中保存的日期格式需要调整为用”-”和”:”分隔。

 使用基于时间点的恢复，可能会出现在一个时间点里同时存在正确的操作和存在错误操作。所以基于位置是一种更为精确的恢复方式。

 2、基于位置的恢复

 --start-position  从二进制日志中读取指定position 事件位置作为开始。

 --stop-position  从二进制日志中读取指定position 事件位置作为事件截至。

 上面的误操作delete开始位置是at 490，结束位置是at 663。**（开始结束位置反向写）**

\# mysqlbinlog --stop-position=490 /data/mysql/log/mysql-bin.000019 | mysql -uroot -p123456

\# mysqlbinlog --start-position=663 /data/mysql/log/mysql-bin.000019 | mysql -uroot -p123456

 

如下例为为忽略默认字符集设置：

mysqlbinlog --no-defaults --stop-position=527 /usr/local/mysql/data/binlog.000007 | mysql -uroot -p

mysqlbinlog --no-defaults --start-position=708 /usr/local/mysql/data/binlog.000007 | mysql -uroot -p

 查看恢复结果：

 mysql> select * from stu;

```
 +-----+-------+

 | sid | NAME |

 +-----+-------+

 | 101 | aiden |

 | 102 | li1  |

 | 103 | li2  |

 | 104 | li3  |

 | 120 | li120 |

 | 121 | li121 |

 +-----+-------+

 6 rows in set (0.00 sec)
```

 从上面显示可以看出数据恢复到正常状态。

 mysql> set sql_log_bin=1;
