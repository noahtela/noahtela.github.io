---
layout:     post
title:      "k8s-jenkins"
subtitle:   " \"linux\""
date:       2024-4-17 10:25:49
author:     "yangsir"
header-img: "img/bg-material.jpg"
catalog: true
tags:
    - 笔记
    - k8s
    - 云原生




---

> “Jenkins实现持续集成和持续部署”


<p id = "build"></p>

实验环境：

| ip              | 操作系统 | 主机名  |
| --------------- | -------- | ------- |
| 192.168.171.217 | centos7  | jenkins |



## jenkins安装



准备实验环境

```
systemctl stop firewalld
iptables -F
setenforce 0
```

### 1、安装jdk

```shell
上传tar包到/opt目录下

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
tar -zxvf /opt/jdk-8u171-linux-x64.tar.gz -C /usr/local/ >&/dev/null
ln -s /usr/local/jdk1.8.0_171 /usr/local/jdk
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

![image-20240425083236432](\img\springBoot\image-20240425083236432.png)



### 2、jenkins安装方式一

Jenkins安装方式一：war包

先安装tomcat将jenkins.war直接放到webapps目录下

通过 `java -jar jenkins.war --httpPort=8080`命令直接运行

官方仓库：https://pkg.jenkins.io/redhat-stable/

### 3、Jenkins安装方式二：rpm方式（推荐）

![image-20240425085801844](\img\springBoot\image-20240425085801844.png)

```shell
rpm -ivh jenkins-2.164.1-1.1.noarch.rpm
```

启动jenkins

```shell
/etc/init.d/jenkins start
```

如果是tar包安装的jdk，可能会报错，jenkins启动脚本未扫描到java,所以要手动添加jdk的安装路径

```shell
vi /etc/init.d/jenkins
```

![image-20240425092248321](\img\springBoot\image-20240425092248321.png)

```
systemctl daemon-reload
systemctl start jenkins
```

![image-20240425092713197](\img\springBoot\image-20240425092713197.png)

```shell
[root@jenkins ~]# rpm -ql jenkins
/etc/init.d/jenkins         # 启动文件
/etc/logrotate.d/jenkins    # 日志分割配置文件
/etc/sysconfig/jenkins      # jenkins主配置文件
/usr/lib/jenkins           # 存放war包目录
/usr/lib/jenkins/jenkins.war # war 包 
/usr/sbin/rcjenkins        # 命令
/var/cache/jenkins        # war包解压目录 jenkins网页代码目录
/var/lib/jenkins           # jenkins 工作目录
```



配置文件说明

```shell
[root@jenkins ~]# grep "^[a-Z]" /etc/sysconfig/jenkins
JENKINS_HOME="/var/lib/jenkins"	#jenkins工作目录
JENKINS_JAVA_CMD=""
JENKINS_USER="jenkins"			# jenkinx启动用户
JENKINS_JAVA_OPTIONS="-Djava.awt.headless=true"
JENKINS_PORT="8080"				# 端口
JENKINS_LISTEN_ADDRESS=""
JENKINS_HTTPS_PORT=""
JENKINS_HTTPS_KEYSTORE=""
JENKINS_HTTPS_KEYSTORE_PASSWORD=""
JENKINS_HTTPS_LISTEN_ADDRESS=""
JENKINS_DEBUG_LEVEL="5"
JENKINS_ENABLE_ACCESS_LOG="no"
JENKINS_HANDLER_MAX="100"		# 最大连接
JENKINS_HANDLER_IDLE="20"
JENKINS_ARGS=""
```



浏览器访问测试

![image-20240425092858354](\img\springBoot\image-20240425092858354.png)

获取密码

```shell
cat /var/lib/jenkins/secrets/initialAdminPassword
```



安装插件

**来到这个页面后，不要着急点**

![image-20240425101014311](\img\springBoot\image-20240425101014311.png)



修改插件下载源

```
cd /var/lib/jenkins/updates
sed -i 's/http:\/\/updates.jenkins-ci.org\/download/https:\/\/mirrors.tuna.tsinghua.edu.cn\/jenkins/g' default.json && sed -i 's/http:\/\/www.google.com/https:\/\/www.baidu.com/g' default.json
```

![image-20240425101223570](\img\springBoot\image-20240425101223570.png)



