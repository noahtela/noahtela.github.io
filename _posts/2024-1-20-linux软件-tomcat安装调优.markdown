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

