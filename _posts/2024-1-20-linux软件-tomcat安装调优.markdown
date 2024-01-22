---
layout:     post
title:      "linux软件-tomcat安装调优"
subtitle:   " \"linux\""
date:       2024-1-20 16:08:12
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

# linux软件-tomcat安装调优



部署环境：

 MySQL-Connector-Java：         mysql-connector-java-5.1.47

 Tomcat：                                    apache-tomcat-8.5.42

 JDK：                                         jdk-8u171-linux-x64

 MySQL：                                    mysql-5.7.26



## 一、安装JDK



### 1、卸载openjdk

 安装之前需要查看下系统是否安装了openjdk，如果安装了openjdk，请先卸载，否则安装不了oracle官方的jdk

```shell
yum remove java-* -y
```

### 2、下载（或上传）所需安装包

![image-20240119162114879](/img\springBoot\image-20240119162114879.png)

### 3、解压JDK

```shell
tar -zxvf jdk-8u171-linux-x64.tar.gz -C /usr/local/
```

```shell
ln -s /usr/local/jdk1.8.0_171 /usr/local/jdk
```



### 4、配置JDK环境变量



```shell
vim /etc/profile #在文件最后加入一下行

 JAVA_HOME=/usr/local/jdk

 PATH=$JAVA_HOME/bin:$PATH

 CLASSPATH=$JAVA_HOME/jre/lib/ext:$JAVA_HOME/lib/tools.jar

 export PATH JAVA_HOME CLASSPATH
```

```shell
source /etc/profile #重载环境变量
```

### 5、查看java环境变量

```shell
java -version
```

![image-20240119162507529](/img\springBoot\image-20240119162507529.png)

## 二、安装Tomcat

### 1、解压软件包

```shell
 tar -zxvf apache-tomcat-8.5.42.tar.gz -C /usr/local/
```



### 2、做连接

```shell
ln -s /usr/local/apache-tomcat-8.5.42 /usr/local/tomcat
```



### 3、Tomcat个目录文件用途

   |---bin：存放启动和关闭tomcat执行脚本；

   |---conf ：存放Tomcat服务器的各种全局配置文件，其中最重要的是server.xml和web.xml；

   |---lib： 存放Tomcat运行需要的库文件（jar），包含Tomcat使用的jar文件。unix平台此目录下的任何文件都被加到Tomcat的classpath中；

   |---logs：存放Tomcat执行时的LOG文件；

   |---webapps：Tomcat的主要Web发布目录，默认情况下把Web应用文件放于此目录，即供外界访问的web资源的存放目录；

   |--- webapps/ROOT：tomcat的家目录

   |--- webapps/ROOT/ index.jsp：Tomcat的默认首页文件

   |---work：存放jsp编译后产生的class文件或servlet文件存放

   |---temp：存放Tomcat运行时所产生的临时文件

### 4、Tomcat启动脚本

```shell
vim /etc/init.d/tomcat


#!/bin/bash
# Tomcat init script for Linux
# chkconfig: 2345 96 14
# discription: The Apache Tomcat Server/JSP container
JAVA_HOME=/usr/local/jdk/
CATALINA_HOME=/usr/local/tomcat
start_tomcat=$CATALINA_HOME/bin/startup.sh       #tomcat启动文件
stop_tomcat=$CATALINA_HOME/bin/shutdown.sh     #tomcat关闭文件

start() {
        echo -n "Starting tomcat: "
        ${start_tomcat}
        echo "tomcat start ok."
}
stop() {
        echo -n "Shutting down tomcat: "
        ${stop_tomcat}
        echo "tomcat stop ok."
}

# See how we were called

case "$1" in
  start)
        start
        ;;
  stop)
        stop
        ;;
  restart)
        stop
        sleep 5
        start
        ;;
  *)
        echo "Usage: $0 {start|stop|restart}"
esac
exit 0



chmod +x /etc/init.d/tomcat  #添加执行权限
```





### 5、建立系统服务文件

```shell
vim /lib/systemd/system/tomcat.service

[Unit]
Description=tomcat
After=network.target

[Service]
Type=forking
Environment=JAVA_HOME=/usr/local/jdk1.8.0_171/
Environment=CATALINA_HOME=/usr/local/tomcat
ExecStart=/etc/init.d/tomcat start
ExecStop=/etc/init.d/tomcat stop
ExecRestart=/etc/init.d/tomcat restart
PrivateTmp=true

[Install]
WantedBy=multi-user.target



systemctl daemon-reload  #重载service文件
```

