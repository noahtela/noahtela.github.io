---
layout:     post
title:      "linux软件-Redis(三)"
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

> “本节主要介绍Redis的基础命令 ”


<p id = "build"></p>

# linux软件-Redis(三)

# Reids命令

## 一、Redis常规命令

```
[root@node2 ~]# redis-cli -h 192.168.109.54 -p 6379

192.168.109.54:6379> set myname "berry" #插入一条记录
OK

192.168.109.54:6379> get myname   #获取myname key的值
"berry"

192.168.109.54:6379> set foo bar
OK

192.168.109.54:6379> get foo
"bar"

192.168.109.54:6379> keys *     #查看所有key
1) "myname"
2) "foo"

```

## 二、键的遵循

-  可以使用ASCII字符
- 键的长度不要过长，键的长度越长则消耗的空间越多
- 在同一个库中（名称空间），键的名称不得重复，如果复制键的名称，实际上是修改键中的值
- 在不同的库中（名称空间），键的同一个名称可以重复
- 键可以实现自动过期

## 三、Redis命令详解

### 1、set

SET key value [expiration EX seconds|PX milliseconds] [NX|XX]

**命令 键 值 [EX 过期时间，单位秒]**

**NX：如果一个键不存在，才创建并设定值，否则不允许设定**

**XX：如果一个键存在则设置建的值，如果不存在则不创建并不设置其值**

示例：

```
192.168.109.54:6379> set cjk aaa  
OK
192.168.109.54:6379> set cjk bbb NX   
(nil)             #反回提示一个没能执行的操作
192.168.109.54:6379> get cjk
"aaa"
192.168.109.54:6379> set foo abc XX  #设置foo key值，foo之前存在
OK              #修改成功
192.168.109.54:6379> get foo
"abc"
```

### 2、get

```
GET key
  summary: Get the value of a key
  since: 1.0.0
  group: string
```

### 3、APPEND

```
APPEND key value
  summary: Append a value to a key
  since: 2.0.0
  group: string
```

示例：

```shell
append添加键中的值（在原有键中附加值的内容）：
192.168.109.54:6379> append cjk fda
(integer) 6
192.168.109.54:6379> get cjk
"aaafda"
```

### 4、获取指定键中的值的字符串的长度

```
192.168.109.54:6379> strlen cjk
(integer) 6
```

### 5、删除键

```
192.168.109.54:6379> del cjk
(integer) 1
192.168.109.54:6379> get cjk
(nil)
```

## 四、列表的操作

 键指向一个列表，而列表可以理解为是一个字符串的容器，列表是有众多元素组成的集合，可以在键所指向的列表中附加一个值

- LPUSH //在键所指向的列表前面插入一个值（左边加入）
- RPUSH //在键所指向的列表后面附加一个值（右边加入）
- LPOP //在键所指向的列表前面弹出一个值（左边弹出）
- RPOP //在键所指向的列表后面弹出一个值（右边弹出）
- LINDEX //根据索引获取值，指明索引位置进行获取对应的值
- LSET //用于修改指定索引的值为指定的值
