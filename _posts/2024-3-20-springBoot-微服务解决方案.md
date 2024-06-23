---

layout:     post
title:      "springBoot-微服务解决方案"
subtitle:   " \"springBoot\""
date:       2024-3-20 10:25:49
author:     "yangsir"
header-img: "img/bg-material.jpg"
catalog: true
tags:
    - 笔记
    - springBoot
    - 微服务




---

> “微服务架构期末作业解决方案”


<p id = "build"></p>



# 微服务



## 前言

本教程适用于centos7，centos7安装教程及网络配置参照[linux-虚拟机安装centos及网络配置 - Xavier的博客 | Xavier Blog (830sir.top)](https://blog.830sir.top/2023/12/09/虚拟机安装centos及网络配置/)

本教程涉及到的安装包、docker镜像、系统镜像：`https://www.123pan.com/s/gIBcVv-JzyN3.html`提取码:WD3G

**2024年6月6日 起 Docker国内镜像源失效**

## 实验环境

| 操作系统 | ip             | 主机名 | 作用                                |
| -------- | -------------- | ------ | ----------------------------------- |
| centos7  | 192.168.171.21 | yms1   | 运行docker、docker-compose、jenkins |
| centos7  | 192.168.171.22 | yms2   | git服务器                           |

### 软件版本

| 软件           | 版本   |
| -------------- | ------ |
| JDK            | 17     |
| mvn            | 3.8.6  |
| Docker         | 26.1.4 |
| Docker-compose | 2.27.1 |



## 一、准备虚拟机

![image-20240621200605779](\img\springBoot\image-20240621200605779.png)

![image-20240621200640395](\img\springBoot\image-20240621200640395.png)



## 二、安装JDK(yms1)

### 1、将安装包上传至/opt目录下

```bash
cd /opt
```

![image-20240621201052929](\img\springBoot\image-20240621201052929.png)

### 2、编辑运行JDK安装脚本

```bash
vi java.sh
```



```bash
#复制下面shell脚本到java.sh中


#!/bin/bash
# -------------------------------------------------
# Script Name: java.sh
# Author: Xavier
# Date: 2024-03-12
# -------------------------------------------------

# 检查用户是否为root
if [ "$(id -u)" != "0" ]; then
        echo "脚本运行中需要root权限，请提权后重试" 1>&2
        return 1
fi
clear
echo -e "\033[31m __   __                      _      \033[0m"
echo -e "\033[31m \ \ / /_ _ _ __   __ _   ___(_)_ __ \033[0m"
echo -e "\033[31m  \ V / _\` | '_ \ / _\` | / __| | '__|\033[0m"
echo -e "\033[31m   | | (_| | | | | (_| | \__ \ | |   \033[0m"
echo -e "\033[31m   |_|\__,_|_| |_|\__, | |___/_|_|   \033[0m"
echo -e "\033[31m                  |___/             \033[0m"
sleep 2
#检测/opt目录下是否有jdk的安装包
jdk_files=$(find /opt -maxdepth 1 -name "*jdk*.tar.gz")

# 检查变量是否为空
if [ -z "$jdk_files" ]; then
        echo "未发现.tar.gz格式的jdk安装包，请将安装包放在/opt目录下再次运行脚本"
        return 1
fi

echo "环境检查完毕"
echo "正在安装jdk..."
# 查找已安装的Java包
installed_java_pkgs=$(rpm -qa | grep -E '^java-|^jdk-')

# 如果没有找到Java包,则跳过删除步骤
if [ -z "$installed_java_pkgs" ]; then
        echo "没有发现已安装的Java包"
else
        # 逐个删除Java包
        for pkg in $installed_java_pkgs; do
                rpm -e --nodeps "$pkg"
        done
fi
tar -zxvf /opt/jdk-17.0.10_linux-x64_bin.tar.gz -C /usr/local/ >&/dev/null
ln -s /usr/local/jdk-17.0.10 /usr/local/jdk
setenforce 0
iptables -F
systemctl stop firewalld
systemctl disable firewalld >&/dev/null
systemctl stop NetworkManager
systemctl disable NetworkManager >&/dev/null
touch /opt/temp.txt
echo 'JAVA_HOME=/usr/local/jdk' >>/opt/temp.txt
echo 'PATH=$JAVA_HOME/bin:$PATH' >>/opt/temp.txt
echo 'CLASSPATH=$JAVA_HOME/jre/lib/ext:$JAVA_HOME/lib/tools.jar' >>/opt/temp.txt
echo 'export PATH JAVA_HOME CLASSPATH' >>/opt/temp.txt
sed -i -e '$r /opt/temp.txt' /etc/profile
rm -f /opt/temp.txt
source /etc/profile #重载环境变量
echo "jdk安装完成"
```

```bash
source java.sh
```

![sacadscfscascsdcsdcsac](\img\springBoot\sacadscfscascsdcsdcsac.png)

## 三、安装MVN(yms1)

### 1、上传maven至/opt

### 2、解压

```bash
tar -zxvf apache-maven-3.8.6-bin.tar.gz

mv apache-maven-3.8.6 maven
```

### 3、编辑环境变量

```bash
echo 'MAVEN_HOME=/opt/maven' | sudo tee -a /etc/profile
echo 'export PATH=${MAVEN_HOME}/bin:${PATH}' | sudo tee -a /etc/profile
source /etc/profile
```

### 4、验证安装结果

```bash
[root@yms1 opt]# mvn -version
Apache Maven 3.8.6 (84538c9988a25aec085021c365c560670ad80f63)
Maven home: /opt/maven
Java version: 17.0.10, vendor: Oracle Corporation, runtime: /usr/local/jdk-17.0.10
Default locale: zh_CN, platform encoding: UTF-8
OS name: "linux", version: "3.10.0-1160.108.1.el7.x86_64", arch: "amd64", family: "unix"
```

![2024-06-22 000337](\img\springBoot\2024-06-22 000337.png)

## 四、安装docker（两台都需要）

Docker安装请参照（**因为国内镜像源关闭，所以不用设置ustc的镜像，省略2.2**）：[linux软件-docker - Xavier的博客 | Xavier Blog (830sir.top)](https://blog.830sir.top/2024/02/17/linux软件-docker/)

![2024-06-22 000444](\img\springBoot\2024-06-22 000444.png)

## 五、安装docker-compose（yms1）

### 1、将docker-compose包上传至/opt目录下

### 2、修改权限

```
mv docker-compose-linux-x86_64 docker-compose
chmod +x /opt/docker-compose
mv /opt/docker-compose /usr/bin/docker-compose
```

### 3、验证安装结果

```
[root@yms1 opt]# docker-compose -v
Docker Compose version v2.27.1
```

![2024-06-22 000528](\img\springBoot\2024-06-22 000528.png)

## 六、docker中搭建私有仓库并上传JAVA:8-jre镜像

### 1、上传`registry.tar`至/opt目录下

```shell
[root@yms1 opt]# ll
总用量 212080
-rw-r--r--. 1 root root   8676320 6月  21 22:15 apache-maven-3.8.6-bin.tar.gz
-rw-r--r--. 1 root root      2117 6月  21 22:00 java.sh
-rw-r--r--. 1 root root 182487685 6月  21 20:10 jdk-17.0.10_linux-x64_bin.tar.gz
drwxr-xr-x. 6 root root        99 6月  21 22:17 maven
-rw-r--r--. 1 root root  25994752 6月  21 22:45 registry.tar
```

### 2、开启docker服务

```shell
systemctl start docker
```



### 3、导入镜像

```shell
[root@yms1 opt]# docker load -i /opt/registry.tar 
aedc3bda2944: Loading layer [==================================================>]   7.63MB/7.63MB
7908a7c94e63: Loading layer [==================================================>]  792.6kB/792.6kB
b2533b493853: Loading layer [==================================================>]  17.55MB/17.55MB
8135430299ba: Loading layer [==================================================>]  3.584kB/3.584kB
e8d8a60f42b5: Loading layer [==================================================>]  2.048kB/2.048kB
Loaded image: registry:latest
```

### 4、启动私有仓库容器

```shell
docker run -di --name=registry -p 5000:5000 registry
```

### 5、打开浏览器验证

输入地址http://192.168.171.21:5000/v2/_catalog看到`{"repositories":[]}` 表示私有仓库搭建成功并且内容为空

![2024-06-22 000557](\img\springBoot\2024-06-22 000557.png)

### 6、修改daemon.json

```
vi /etc/docker/daemon.json
```

添加以下内容，保存退出。（**注意替换IP**）

```
{"insecure-registries":["192.168.171.21:5000"]}  
```

此步用于让 docker信任私有仓库地址

### 7、重启docker 服务

```shell
systemctl restart docker
```

### 8、上传java.tar至/opt目录下，导入镜像

```shell
docker load -i /opt/java.tar 
```



```shell
[root@yms1 opt]# docker load -i /opt/java.tar 
9c742cd6c7a5: Loading layer [==================================================>]  129.2MB/129.2MB
03127cdb479b: Loading layer [==================================================>]   11.3MB/11.3MB
293d5db30c9f: Loading layer [==================================================>]  19.31MB/19.31MB
9b55156abf26: Loading layer [==================================================>]  156.5MB/156.5MB
b626401ef603: Loading layer [==================================================>]  11.74MB/11.74MB
53a0b163e995: Loading layer [==================================================>]  3.584kB/3.584kB
6b5aaff44254: Loading layer [==================================================>]  209.8MB/209.8MB
Loaded image: 192.168.222.100:5000/java:8-jre
```

### 9、标记此镜像为私有仓库的镜像（替换第二个IP成自己的IP）

```shell
docker tag 192.168.222.100:5000/java:8-jre 192.168.171.21:5000/java:8-jre
```

```shell
[root@yms1 opt]# docker images
REPOSITORY                  TAG       IMAGE ID       CREATED         SIZE
registry                    latest    d6b2c32a0f14   8 months ago    25.4MB
192.168.171.21:5000/java    8-jre     b273004037cc   22 months ago   526MB
192.168.222.100:5000/java   8-jre     b273004037cc   22 months ago   526MB
```

### 10、再次启动私服容器

```shell
docker start registry
```



### 11、上传标记的镜像（注意镜像名）

```shell
docker push 192.168.171.21:5000/java:8-jre
```

```shell
[root@yms1 opt]# docker push 192.168.171.21:5000/java:8-jre
The push refers to repository [192.168.171.21:5000/java]
6b5aaff44254: Pushed 
53a0b163e995: Pushed 
b626401ef603: Pushed 
9b55156abf26: Pushed 
293d5db30c9f: Pushed 
03127cdb479b: Pushed 
9c742cd6c7a5: Pushed 
8-jre: digest: sha256:3af2ac94130765b73fc8f1b42ffc04f77996ed8210c297fcfa28ca880ff0a217 size: 1795
```

### 12、浏览器验证

![2024-06-22 000629](\img\springBoot\2024-06-22 000629.png)

## 七、准备代码和镜像

### 1、修改配置文件

四个配置文件中的ip全都替换成自己的IP，**四个pom文件的私有仓库地址更改为自己的**

![2024-06-22 000656](\img\springBoot\2024-06-22 000656.png)

### 2、添加Dockerfile文件（注意替换IP,注意文件位置）

<img src="\img\springBoot\2024-06-22 000743.png" alt="2024-06-22 000743" style="zoom: 67%;" />

```shell
FROM 192.168.171.21:5000/java:8-jre
MAINTAINER yms <yangmaos@qq.com>

ADD ./target/microservice-eureka-server-0.0.1-SNAPSHOT.jar /app/microservice-eureka-service.jar
CMD ["java", "-Xmx200m", "-jar", "/app/microservice-eureka-service.jar"]

EXPOSE 8761
```

```shell
FROM 192.168.171.21:5000/java:8-jre
MAINTAINER yms <123456@qq.com>

ADD ./target/microservice-gateway-zuul-0.0.1-SNAPSHOT.jar /app/microservice-gateway-zuul.jar
CMD ["java", "-Xmx200m", "-jar", "/app/microservice-gateway-zuul.jar"]

EXPOSE 8050
```

```shell
FROM 192.168.171.21:5000/java:8-jre
MAINTAINER yms <123456@qq.com>

ADD ./target/microservice-orderservice-0.0.1-SNAPSHOT.jar /app/microservice-orderservice.jar
CMD ["java", "-Xmx200m", "-jar", "/app/microservice-orderservice.jar"]

EXPOSE 7900
```

```shell
FROM 192.168.171.21:5000/java:8-jre
MAINTAINER yms <123456@qq.com>
ADD ./target/microservice-userservice-0.0.1-SNAPSHOT.jar \
     /app/microservice-userservice.jar
CMD ["java", "-Xmx200m", "-jar", "/app/microservice-userservice.jar"]

EXPOSE 8030
```

### 3、添加docker-compose.xml（注意替换ip和数据库密码）

![2024-06-22 000825](\img\springBoot\2024-06-22 000825.png)

```shell
version: "3"
services:
  mysql:
    image: mysql:5.6
    restart: on-failure
    ports:
      - 3306:3306
    volumes:
      - microservice-mysql:/var/lib/mysql
    networks:
      - microservice-net
    environment:
      MYSQL_ROOT_PASSWORD: 215830 #改成刚才配置文件里填的，不然连不上数据库
      MYSQL_DATABASE: microservice_mallmanagement

  eureka-server:
    image: 192.168.171.21:5000/microservice-eureka-server:0.0.1-SNAPSHOT
    restart: on-failure
    ports:
      - 8761:8761
    networks:
      - microservice-net

  gateway-zuul:
    image: 192.168.171.21:5000/microservice-gateway-zuul:0.0.1-SNAPSHOT
    restart: on-failure
    ports:
      - 8050:8050
    networks:
      - microservice-net
    depends_on:
      - eureka-server

  order-service:
    image: 192.168.171.21:5000/microservice-orderservice:0.0.1-SNAPSHOT
    restart: on-failure
    ports:
      - 7900:7900
    networks:
      - microservice-net
    depends_on:
      - mysql
      - eureka-server

  user-service:
    image: 192.168.171.21:5000/microservice-userservice:0.0.1-SNAPSHOT
    restart: on-failure
    ports:
      - 8030:8030
    networks:
      - microservice-net
    depends_on:
      - mysql
      - eureka-server
networks:
  microservice-net:
volumes:
  microservice-mysql:
```

### 4、打压缩包上传至虚拟机

```shell
mkdir /wfw

cd /wfw
```

上传至/wfw

```shell
[root@yms1 wfw]# ll
总用量 123424
-rw-r--r--. 1 root root 126382165 6月  21 23:20 wfw.zip
```

解压

如果没有`unzip`，先下载

```shell
yum -y install unzip
```

```shell
unzip wfw.zip
```

### 5、使用mvn工具构建镜像

```shell
[root@yms1 wfw]# ll
总用量 123428
drwxr-xr-x. 11 root root      4096 6月  21 23:16 Mymallmanagement
-rw-r--r--.  1 root root 126382165 6月  21 23:20 wfw.zip
```



```shell
cd Mymallmanagement
```

构建

```
mvn install
```

静静等待。。。

![2024-06-22 000859](\img\springBoot\2024-06-22 000859.png)

构建完成

验证

![2024-06-22 001016](\img\springBoot\2024-06-22 001016.png)

![2024-06-22 001051](\img\springBoot\2024-06-22 001051.png)

## 八、使用docker-compose工具启动项目

### 1、上传mysql镜像至/opt目录下

导入mysql镜像

```shell
docker load -i /opt/mysql.tar 
```



```shell
[root@yms1 ~]# cd /opt/
[root@yms1 opt]# docker load -i /opt/mysql.tar 
2b83e5699838: Loading layer [==================================================>]  58.51MB/58.51MB
2e1029557391: Loading layer [==================================================>]  338.4kB/338.4kB
d414fdead0b9: Loading layer [==================================================>]  10.46MB/10.46MB
4085e588967d: Loading layer [==================================================>]  4.176MB/4.176MB
7ea96a4e341b: Loading layer [==================================================>]  2.048kB/2.048kB
e3dce1c82d4e: Loading layer [==================================================>]   41.4MB/41.4MB
2612088e90f6: Loading layer [==================================================>]  26.11kB/26.11kB
eba393347f89: Loading layer [==================================================>]  3.584kB/3.584kB
7c5a5c1986b1: Loading layer [==================================================>]  192.7MB/192.7MB
49a1ca1cd2b8: Loading layer [==================================================>]  17.92kB/17.92kB
7137327a7221: Loading layer [==================================================>]  1.536kB/1.536kB
Loaded image: mysql:5.6
```

### 2、使用docker-compose启动项目

回到项目目录

```shell
cd /wfw/Mymallmanagement
```

```
docker-compose up -d
```

 ![2024-06-22 001243](\img\springBoot\2024-06-22 001243.png)

### 3、查看容器

启动成功

![2024-06-22 001127](\img\springBoot\2024-06-22 001127.png)

### 4、浏览器访问

启动成功

![2024-06-22 001159](\img\springBoot\2024-06-22 001159.png)





# 接下来进行搭建gogs+jenkins实现自动化部署

## 一、删除mvn构建的镜像和docker-compose启动的容器

### 1、删除容器

```shell
docker stop $(docker ps -aq)
```

```shell
docker rm $(docker ps -aq)
```

### 2、删除镜像(别删多了)

```shell
[root@yms1 ~]# docker images
REPOSITORY                                       TAG              IMAGE ID       CREATED         SIZE
192.168.171.21:5000/microservice-userservice     0.0.1-SNAPSHOT   a6d1c347596d   14 hours ago    569MB
192.168.171.21:5000/microservice-orderservice    0.0.1-SNAPSHOT   79b0e305efec   14 hours ago    570MB
192.168.171.21:5000/microservice-gateway-zuul    0.0.1-SNAPSHOT   ce161d59eb84   14 hours ago    565MB
192.168.171.21:5000/microservice-eureka-server   0.0.1-SNAPSHOT   0796ea3afa07   14 hours ago    566MB
registry                                         latest           d6b2c32a0f14   8 months ago    25.4MB
192.168.171.21:5000/java                         8-jre            b273004037cc   23 months ago   526MB
192.168.222.100:5000/java                        8-jre            b273004037cc   23 months ago   526MB
mysql                                            5.6              dd3b2a5dcb48   2 years ago     303MB
```

```shell
#删除命令
docker rmi [IMAGE ID]
```

```shell
#剩下以下镜像
[root@yms1 ~]# docker images
REPOSITORY                  TAG       IMAGE ID       CREATED         SIZE
registry                    latest    d6b2c32a0f14   8 months ago    25.4MB
192.168.171.21:5000/java    8-jre     b273004037cc   23 months ago   526MB
192.168.222.100:5000/java   8-jre     b273004037cc   23 months ago   526MB
mysql                       5.6       dd3b2a5dcb48   2 years ago     303MB
```

### 3、重新启动私有仓库容器并上传java:jre-8

```shell
docker run -di --name=registry -p 5000:5000 registry
```

```shell
docker push 192.168.171.21:5000/java:8-jre  #注意替换IP
```





## 二、搭建git服务器(yms2)

**以为大多数同学访问不到GitHub，这里使用Gogs私有化部署替代GitHub**

### 1、在第二台机器上上传gogs

上传至/opt目录下

```shell
[root@yms2 opt]# ll
总用量 98068
drwx--x--x. 4 root root        28 6月  22 13:28 containerd
-rw-r--r--. 1 root root 100420608 6月  22 13:52 gogs.tar
```

### 2、导入镜像

```shell
[root@yms2 opt]# docker load -i /opt/gogs.tar 
f4111324080c: Loading layer [==================================================>]   7.35MB/7.35MB
f07033ba396b: Loading layer [==================================================>]   25.7MB/25.7MB
130e071c9d92: Loading layer [==================================================>]   2.56kB/2.56kB
66f61b79df21: Loading layer [==================================================>]  2.048kB/2.048kB
fe74d5fb5c7d: Loading layer [==================================================>]  39.94kB/39.94kB
0b870346258a: Loading layer [==================================================>]  64.99MB/64.99MB
8954213ede8a: Loading layer [==================================================>]  2.307MB/2.307MB
Loaded image: gogs/gogs:latest
```

### 3、启动容器

```shell
# 创建并运行一个容器，将宿主机的10022端口映射到容器的22端口，将宿主机的13000端口映射到容器的3000端口，10022端口和13000端口可以根据自己的情况修改
docker run -d -p 10022:22 -p 13000:3000 -v /var/gogs:/data gogs/gogs
```

### 4、浏览器访问ip:13000，进行初始化设置，比如我这里是`http://192.168.171.22:13000`

#### a.gogs目前支持3种数据库：MySQL、PostgreSQL、SQLite3，**如果没有可用的数据库，可以选择SQLite3**，如果想安装Mysql，请参照[linux软件-mysql8.0 - Xavier的博客 | Xavier Blog (830sir.top)](https://blog.830sir.top/2024/01/26/linux软件-mysql8/)

![2024-06-22 154950](\img\springBoot\2024-06-22 154950.png)

这里使用SQLite3做演示

![2024-06-22 155100](\img\springBoot\2024-06-22 155100.png)

#### b、应用基本设置

![2024-06-22 155153](\img\springBoot\2024-06-22 155153.png)

#### c、管理员账号设置

![2024-06-22 155214](\img\springBoot\2024-06-22 155214.png)

#### d、立即安装

![2024-06-22 155245](\img\springBoot\2024-06-22 155245.png)

### 5、创建仓库上传代码

![2024-06-22 155324](\img\springBoot\2024-06-22 155324.png)

![2024-06-22 155355](\img\springBoot\2024-06-22 155355.png)



![2024-06-22 155445](\img\springBoot\2024-06-22 155445.png)打开**代码文件夹**初始化本地仓库并上传代码（**windows上没安装Git的，请自行百度。。**)

在该文件夹下打开git命令行

![2024-06-22 155511](\img\springBoot\2024-06-22 155511.png)

![2024-06-22 155543](\img\springBoot\2024-06-22 155543.png)

**依次执行以下命令**（注意仓库地址）

```shell
git init
git add .   #注意有个点
git commit -m "first commit"
git remote add origin http://192.168.171.22:13000/root/Mymallmanagement.git #注意替换ip
git push -u origin master
```

![2024-06-22 155639](\img\springBoot\2024-06-22 155639.png)

第一次提交代码可能需要验证密码，账号密码就是刚才创建的管理员账号密码

![2024-06-22 155746](\img\springBoot\2024-06-22 155746.png)

刷新浏览器

![2024-06-22 155809](\img\springBoot\2024-06-22 155809.png)

上传成功

## 三、搭建jenkins(yms1)

### 1、上传war包至/opt/jenkins目录下

```shell
mkdir /opt/jenkins
cd /opt/jenkins
```

```shell
[root@yms1 jenkins]# ll
总用量 91216
-rw-r--r--. 1 root root 93404074 6月  22 14:41 jenkins.war
```

```shell
chmod +x /opt/jenkins/jenkins.war
```



### 2、启动jenkins

下载字体库

```
yum install dejavu-sans-fonts
yum install fontconfig
fc-cache --force
```

```shell
java -jar /opt/jenkins/jenkins.war --httpPort=9090
```

![2024-06-22 155846](\img\springBoot\2024-06-22 155846.png)

### 3、浏览器访问

http://192.168.171.21:9090/

![2024-06-22 155920](\img\springBoot\2024-06-22 155920.png)



![2024-06-22 155940](\img\springBoot\2024-06-22 155940.png)



![2024-06-22 160020](\img\springBoot\2024-06-22 160020.png)



**点击安装**

![2024-06-22 160101](\img\springBoot\2024-06-22 160101.png)



点击**保存并完成**

![2024-06-22 160148](\img\springBoot\2024-06-22 160148.png)



点击**现在不要**



**安装插件**

![2024-06-22 160228](\img\springBoot\2024-06-22 160228.png)

![2024-06-22 160324](\img\springBoot\2024-06-22 160324.png)

将原有地址https://updates.jenkins.io/update-center.json替换为清华源https://mirrors.tuna.tsinghua.edu.cn/jenkins/updates/update-center.json



**安装maven插件**

![2024-06-22 160401](\img\springBoot\2024-06-22 160401.png)



![2024-06-22 160430](\img\springBoot\2024-06-22 160430.png)



**继续下载Git插件**

![2024-06-22 160802](\img\springBoot\2024-06-22 160802.png)

配置Java环境和Mvn环境

![2024-06-22 160510](\img\springBoot\2024-06-22 160510.png)



![2024-06-22 160556](\img\springBoot\2024-06-22 160556.png)



![2024-06-22 160627](\img\springBoot\2024-06-22 160627.png)





### 4、创建工程

![2024-06-22 160653](\img\springBoot\2024-06-22 160653.png)



![2024-06-22 160719](\img\springBoot\2024-06-22 160719.png)

![2024-06-22 160901](\img\springBoot\2024-06-22 160901.png)



![2024-06-22 160926](\img\springBoot\2024-06-22 160926.png)

![2024-06-22 160948](\img\springBoot\2024-06-22 160948.png)



```shell
docker-compose up -d
```

保存退出

### 5、构建项目

![2024-06-22 161026](\img\springBoot\2024-06-22 161026.png)



查看控制台输出

![2024-06-22 161150](\img\springBoot\2024-06-22 161150.png)



![2024-06-22 161253](\img\springBoot\2024-06-22 161253.png)



![2024-06-22 161325](\img\springBoot\2024-06-22 161325.png)



构建完成

### 6、访问验证

![2024-06-22 161419](\img\springBoot\2024-06-22 161419.png)



**至此，gogs+jenkins实现自动化部署已实现**



## 常见问题

### jenkins初始化时，报错 提示字体问题的解决方法

安装字体库

```shell
yum install dejavu-sans-fonts
yum install fontconfig
fc-cache --force
```



### jenkins初始化后，如何重新初始化？

jenkins在初始化之后，会在家目录下生成一个.jenkins的隐藏文件

```shell
cd 
ls -a
rm -rf .jenkins
```

删除这个文件之后，即可再次构建。
