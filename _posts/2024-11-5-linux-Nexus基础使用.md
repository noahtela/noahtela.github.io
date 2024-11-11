---
layout:     post
title:      "linux-Nexus基础使用"
subtitle:   " \"Nexus\""
date:       2024-11-5 11:31:12
author:     "yangsir"
header-img: "img/bg-material.jpg"
catalog: true
tags:
    - 笔记
    - Linux
    - Nexus


---

> “Yeah It's on. ”


<p id = "build"></p>

# Nexus从搭建到入门

本文章中涉及到的镜像和软件`https://www.123865.com/s/gIBcVv-zijN3?提取码:X17u`

## 一、搭建本地测试环境

### 1、环境准备

| 操作系统  | IP              |
| --------- | --------------- |
| windows11 | 192.168.105.196 |
| centos7   | 192.168.105.118 |

**ps**:centos7作为Nexus私服服务器，配置要求较高，最低配置**4H4G**



### 2、环境初始化

Centos7本地搭建参照[linux-虚拟机安装centos及网络配置 - Xavier的博客 | Xavier Blog (830sir.top)](https://blogs.830sir.top/2023/12/09/虚拟机安装centos及网络配置/)

Docker安装参照[linux软件-docker - Xavier的博客 | Xavier Blog (830sir.top)](https://blogs.830sir.top/2024/02/17/linux软件-docker/)



### 3、Docker运行Nexus

(1)导入Nexus镜像

```
docker load -i nexus3.tar
```

ps:可能会丢失镜像名和tag号，这样就需要自己打tag号

```
docker tag <IMAGE ID> sonatype/nexus3:latest
```

(2)启动容器

```
docker run -itd --name nexus3 -p 8083:8081 sonatype/nexus3
```

(3)查看日志

```
docker logs -f <CONTAINER ID>
```

当日志中出现**Nexus Successfully started**时 表示启动成功

## 二、Nexus基本操作

### 1、访问Nexus

#### (1)关闭虚拟机防火墙

ps:本地测试环境可行,生产环境或暴露在公网的服务器**谨慎操作**

```shell
#关闭防火墙，关闭iptables,系统安全等级调为最低
setenforce 0
iptables -F
systemctl stop firewalld
systemctl disable firewalld >&/dev/null
systemctl stop NetworkManager
systemctl disable NetworkManager >&/dev/null
```

#### (2)浏览器访问

访问`http://IP:8083/`

### 2、创建npm库

(1)查看admin密码，登录到admin账户

进入到容器中查看admin密码

```shell
docker exec -it <CONTAINER ID> /bin/bash
cat /nexus-data/admin.password
```

账号:admin

密码:刚才cat出来的密码

![image-20241105113420108](D:\blogs\img\linux\image-20241105113420108.png)



(2)创建npm库

登录完成后，点击左上角设置图标，进入管理页面

![image-20241105114246215](D:\blogs\img\linux\image-20241105114246215.png)

点击左侧菜单栏`Repositories`,我们可以看到很多已经配置好的仓库，一般只使用开源jar就够用了，如果需要建立自定义仓库，点击`Create repository`创建新的仓库。

![image-20241105114336431](D:\blogs\img\linux\image-20241105114336431.png)

首先我们要创建几个常用的代理源，用于常用开源npm模块的拉取。

![image-20241105114451537](D:\blogs\img\linux\image-20241105114451537.png)



补全信息

ps:国内常用代理地址

taobao `https://registry.npm.taobao.org/`

npm-npmjs `https://registry.npmjs.org`

![image-20241105114602536](D:\blogs\img\linux\image-20241105114602536.png)

创建`npm-hosted` ,用于发布个人开发的npm组件。

![image-20241105114902009](D:\blogs\img\linux\image-20241105114902009.png)

![image-20241105115143847](D:\blogs\img\linux\image-20241105115143847.png)

创建`npm-public` ,用于把几个仓库组组合在一起公开连接使用。

![image-20241105115236981](D:\blogs\img\linux\image-20241105115236981.png)

![image-20241105115453197](D:\blogs\img\linux\image-20241105115453197.png)

说明:

- npm-proxy：可以代理npmjs和淘宝镜像
- npm-hosted：用于上传、自定义和个人开发的npm组件
- npm-public：仓库分组，把几个仓库组组合在一起使用。

仓库类型

- Group：这是一个仓库聚合的概念，用户仓库地址选择Group的地址，即可访问Group中配置的，用于方便开发人员自己设定的仓库
- maven-public就是一个Group类型的仓库，内部设置了多个仓库，访问顺序取决于配置顺序，3.x默认Releases，Snapshots， Central，当然你也可以自己设置。
- Hosted：私有仓库，内部项目的发布仓库，专门用来存储我们自己生成的jar文件
- 3rd party：未发布到公网的第三方jar (3.x去除了)
- Snapshots：本地项目的快照仓库
- Releases： 本地项目发布的正式版本
- Proxy：代理类型，从远程中央仓库中寻找数据的仓库（可以点击对应的仓库的Configuration页签下Remote Storage属性的值即被代理的远程仓库的路径），如可配置阿里云maven仓库
- Central：中央仓库
- Apache Snapshots：Apache专用快照仓库(3.x去除了)

