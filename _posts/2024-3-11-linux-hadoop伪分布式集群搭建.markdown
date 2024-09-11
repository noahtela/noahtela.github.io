---
layout:     post
title:      "linux-hadoop伪分布式集群搭建"
subtitle:   " \"linux\""
date:       2024-3-11 17:22:49
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

# hadoop伪分布式集群搭建





实验准备

```
centos 7
jdk1.8
hadoop3.3.4
主机设置为hadoop1
ssh设置密钥登录

这里默认已经完成了jdk的安装、主机名设置以及ssh设置密钥登录
```



## 搭建集群



### 1、hadoop压缩包上传解压

```shell
tar -zxvf hadoop-3.3.4.tar.gz -C /usr/local/hadoop/servers
```

`/usr/local/hadoop/servers`为你指定的hadoop的工作目录



### 2、设置环境变量

```shell
# vi /etc/profile

export HADOOP_HOME=/usr/local/hadoop/servers/hadoop-3.3.4
#注意修改路径
export PATH=$PATH:$HADOOP_HOME/bin:$HADOOP_HOME/sbin

```



### 3、进入hadoop工作目录的`etc/hadoop`下，开始修改配置文件

```
cd /usr/local/hadoop/servers/hadoop-3.3.4/etc/hadoop/
```

![image-20240311145948468](\img\springBoot\image-20240311145948468.png)

#### 	（1）修改hadoop-env.sh

```shell
#vi hadoop-env.sh,在最后一行添加

export JAVA_HOME=/usr/local/jdk          #这里要修改为自己jdk的路径
export HDFS_NAMENODE_USER=root           #这行设置HDFS的NameNode用户为root
export HDFS_DATANODE_USER=root           #这行设置HDFS的DataNode用户为root
export HDFS_SECONDARYNAMENODE_USER=root  #这行设置HDFS的SecondaryNameNode用户为root
export YARN_RESOURCEMANAGER_USER=root    #这行设置YARN的ResourceManager用户为root
export YARN_NODEMANAGER_USER=root        #这行设置YARN的NodeManager用户为root
```



#### （2）修改hdfs-site.xml

```shell
#vi hdfs-site.xml
#在<configuration>标签内，下同

<property>
<name>dfs.replication</name>
<value>1</value>
</property>
<property>
<name>dfs.namenode.secondary.http-address</name>
<value>hadoop1:9868</value>
</property>
```



#### （3）修改core-site.xml

```shell
#vi core-site.xml

<property>
<name>fs.defaultFS</name>
<value>hdfs://hadoop1:9000</value>
</property>
<property>
<name>hadoop.tmp.dir</name>
<value>/export/data/hadoop-wfb-3.3.0</value> #如果要重新初始化，要先删除这个文件
</property>
<property>
<name>hadoop.http.staticuser.user</name>
<value>root</value>
</property>
<property>
<name>hadoop,proxyuser.root.hosts</name>
<value>*</value>
</property>
<property>
<name>hadoop,proxyuser.root.groups</name>
<value>*</value>
</property>
<property>
<name>fs.trash.interval</name>
<value>1440</value>
</property>
```

#### （4）修改mapred-site.xml



```shell
#vi mapred-site.xml

<property>
        <name>mapreduce.framework.name</name>
        <value>yarn</value>
</property>
<property>
        <name>mapreduce.jobhistory.address</name>
        <value>hadoop1:10020</value>
</property>
<property>
        <name>mapreduce.jobhistory.webapp.address</name>
        <value>hadoop1:19888</value>
</property>
<property>
        <name>yarn.app.mapreduce.am.env</name>
        <value>HADOOP_MAPRED_HOME=/usr/local/hadoop/servers/hadoop-3.3.4</value>
</property>
<property>
        <name>mapreduce.map.env</name>
        <value>HADOOP_MAPRED_HOME=/usr/local/hadoop/servers/hadoop-3.3.4</value>
</property>
<property>
<name>mapreduce.reduce.eny</name>
<value>HADOOP_MAPRED_HOME=/usr/local/hadoop/servers/hadoop-3.3.4</value>
</property>
```



#### （5）修改yarn-site.xml

```shell
#vi yarn-site.xml

<property>
<name>yarn.resourcemanager.hostname</name>
<value>hadoop1</value>
</property>
<property>
<name>yarn.nodemanager.aux-services</name>
<value>mapreduce_shuffle</value>
</property>
<property>
<name>yarn.nodemanager.pmem-check-enabled</name>
<value>false</value>
</property>
<property>
<name>yarn.nodemanager.vmem-check-enabled</name>
<value>false</value>
</property>
<property>
<name>yarn.1og-aggregation-enable</name>
<value>true</value>
</property>
<property>
<name>yarn.log.server.url</name>
<value>http://hadoop1:19888/jobhistory/logs</value>
</property>
<property>
<name>yarn.log-aggregation.retain-seconds</name>
<value>604800</value>
</property>
```





### 4、初始化hadoop集群



```shell
hdfs namenode -format
```

![image-20240311152239173](\img\springBoot\image-20240311152239173.png)



出现`successfully formatted`则表示初始化成功



### 5、启动与关闭集群



启动

```
start-all.sh
```

![image-20240311152438613](\img\springBoot\image-20240311152438613.png)



检查是否启动成功

```
jps
```

![image-20240311152511020](\img\springBoot\image-20240311152511020.png)

出现相同数样的服务，则表示启动成功



关闭集群服务

```
 stop-all.sh 
```





## 一键安装脚本

使用说明：

```
脚本适用于centos 7
适用纯净环境中,脚本将会自动安装jdk和hadoop
脚本运行中可自定义主机名

把安装包和脚本上传至虚拟机/opt目录下
jdk版本:jdk-8u171-linux-x64.tar.gz
hadoop版本:hadoop-3.3.4.tar.gz
使用root
cd /opt
运行脚本
source hadoop_install.sh
```



```shell
#以下为shell脚本代码,复制下来命名为hadoop_install.sh

#!/bin/bash

# -------------------------------------------------
# Script Name: hadoop_install.sh
# Author: Xavier
# Date: 2024-03-12
# -------------------------------------------------

# 检查用户是否为root
if [ "$(id -u)" != "0" ]; then
   echo "脚本运行中需要root权限，请提权后重试" 1>&2
   return 1
fi
#获取本机ip
ip=$(ip addr show ens33| grep 'inet ' | awk '{print $2}' | cut -d/ -f1)
clear
echo -e "\033[31m __   __                      _      \033[0m"
echo -e "\033[31m \ \ / /_ _ _ __   __ _   ___(_)_ __ \033[0m"
echo -e "\033[31m  \ V / _\` | '_ \ / _\` | / __| | '__|\033[0m"
echo -e "\033[31m   | | (_| | | | | (_| | \__ \ | |   \033[0m"
echo -e "\033[31m   |_|\__,_|_| |_|\__, | |___/_|_|   \033[0m"
echo -e "\033[31m                  |___/             \033[0m"
sleep 2


#检测是否已安装Java
rpm -qa | grep java >&/dev/null
java_status="$?"

#检测/opt目录下是否有jdk的安装包
jdk_files=$(find /opt -maxdepth 1 -name "*jdk*.tar.gz")
#检测/opt目录下是否有hadoop的安装包
hadoop_files=$(find /opt -maxdepth 1 -name "*hadoop*.tar.gz")
# 检查变量是否为空
if [ -z "$jdk_files" ]; then
    echo "未发现.tar.gz格式的jdk安装包，请将安装包放在/opt目录下再次运行脚本"
    return 1
fi
if [ -z "$hadoop_files" ]; then
    echo "未发现.tar.gz格式的hadoop安装包，请将安装包放在/opt目录下再次运行脚本"
    return 1
fi
echo "环境检查完毕"

if [ $java_status -eq 0 ]
then
while true; do
    # 询问用户是否继续
    echo "检查到已经安装了jdk，继续安装则会删除已有jdk环境？(yes/no)"
    read answer

    # 如果用户输入 "yes"，则跳出循环
    if [ "$answer" = "yes" ]; then
        break
    # 如果用户输入 "no"，则结束脚本
    elif [ "$answer" = "no" ]; then
        echo "Exiting."
        return 1
    else
        # 如果用户输入了其他内容，打印一条消息，然后继续循环
        echo "Invalid input. Please enter 'yes' or 'no'."
    fi
done
fi
# 提示用户输入主机名
echo "请输入主机名:"

# 读取用户输入
read hostname

# 检查用户是否有输入
if [ -z "$hostname" ]
then
  # 如果用户没有输入，设置默认主机名为hadoop1
  hostname="hadoop1"
fi
echo "配置ssh免密登录"
# 检查~/.ssh/id_rsa是否存在
if [ ! -f ~/.ssh/id_rsa ]; then
    echo "SSH密钥不存在，正在生成..."
    ssh-keygen -t rsa -b 4096 -N "" -f ~/.ssh/id_rsa
else
    echo "SSH密钥已存在."
fi

# 将公钥添加到~/.ssh/authorized_keys以实现免密登录
cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys

# 设置适当的权限
chmod 700 ~/.ssh
chmod 600 ~/.ssh/authorized_keys

echo "免密登录配置完成。"
#开始安装jdk

echo "正在安装jdk..."
rpm -qa | grep java | xargs rpm -e >&/dev/null
tar -zxvf /opt/jdk-8u171-linux-x64.tar.gz -C /usr/local/ >&/dev/null
ln -s /usr/local/jdk1.8.0_171 /usr/local/jdk

touch /opt/temp.txt
echo 'JAVA_HOME=/usr/local/jdk' >> /opt/temp.txt
echo 'PATH=$JAVA_HOME/bin:$PATH' >> /opt/temp.txt
echo 'CLASSPATH=$JAVA_HOME/jre/lib/ext:$JAVA_HOME/lib/tools.jar' >> /opt/temp.txt
echo 'export PATH JAVA_HOME CLASSPATH' >> /opt/temp.txt
sed -i -e '$r /opt/temp.txt' /etc/profile
rm -f /opt/temp.txt
source /etc/profile #重载环境变量
echo "jdk安装完成"

echo "主机名正在修改为$hostname  ..."
echo "$ip $hostname" >> /etc/hosts
systemctl restart network
echo "主机名已修改为$hostname"

#安装hadoop
echo "开始安装hadoop..."
tar -zxvf /opt/hadoop-3.3.4.tar.gz -C /usr/local/ >&/dev/null
ln -s /usr/local/hadoop-3.3.4 /usr/local/hadoop

echo 'export HADOOP_HOME=/usr/local/hadoop' >> /opt/temp.txt
echo 'export PATH=$PATH:$HADOOP_HOME/bin:$HADOOP_HOME/sbin' >> /opt/temp.txt
sed -i -e '$r /opt/temp.txt' /etc/profile
rm -f /opt/temp.txt

source /etc/profile #重载环境变量

#安装完成，开始修改配置文件

echo 'export JAVA_HOME=/usr/local/jdk          #这里要修改为自己jdk的路径' >> /opt/temp.txt
echo 'export HDFS_NAMENODE_USER=root           #这行设置HDFS的NameNode用户为root' >> /opt/temp.txt
echo 'export HDFS_DATANODE_USER=root           #这行设置HDFS的DataNode用户为root' >> /opt/temp.txt
echo 'export HDFS_SECONDARYNAMENODE_USER=root  #这行设置HDFS的SecondaryNameNode用户为root' >> /opt/temp.txt
echo 'export YARN_RESOURCEMANAGER_USER=root    #这行设置YARN的ResourceManager用户为root' >> /opt/temp.txt
echo 'export YARN_NODEMANAGER_USER=root        #这行设置YARN的NodeManager用户为root' >> /opt/temp.txt

sed -i -e '$r /opt/temp.txt' /usr/local/hadoop/etc/hadoop/hadoop-env.sh
rm -f /opt/temp.txt


#hdfs-site.xml
file_path="/usr/local/hadoop/etc/hadoop/hdfs-site.xml"
temp_file="temp.xml"
cat > $temp_file <<EOL
<property>
<name>dfs.replication</name>
<value>1</value>
</property>
<property>
<name>dfs.namenode.secondary.http-address</name>
<value>$hostname:9868</value>
</property>
EOL
sudo sed -i "/<configuration>/r $temp_file" $file_path
rm -f $temp_file


# core-site.xml
file_path="/usr/local/hadoop/etc/hadoop/core-site.xml"
temp_file="temp.xml"
cat > $temp_file <<EOL
<property>
<name>fs.defaultFS</name>
<value>hdfs://$hostname:9000</value>
</property>
<property>
<name>hadoop.tmp.dir</name>
<value>/export/data/hadoop-wfb-3.3.0</value>
</property>
<property>
<name>hadoop.http.staticuser.user</name>
<value>root</value>
</property>
<property>
<name>hadoop.proxyuser.root.hosts</name>
<value>*</value>
</property>
<property>
<name>hadoop.proxyuser.root.groups</name>
<value>*</value>
</property>
<property>
<name>fs.trash.interval</name>
<value>1440</value>
</property>
EOL
sudo sed -i "/<configuration>/r $temp_file" $file_path
rm -f $temp_file




# mapred-site.xml
file_path="/usr/local/hadoop/etc/hadoop/mapred-site.xml"
cat > $temp_file <<EOL
<property>
        <name>mapreduce.framework.name</name>
        <value>yarn</value>
</property>
<property>
        <name>mapreduce.jobhistory.address</name>
        <value>$hostname:10020</value>
</property>
<property>
        <name>mapreduce.jobhistory.webapp.address</name>
        <value>$hostname:19888</value>
</property>
<property>
        <name>yarn.app.mapreduce.am.env</name>
        <value>HADOOP_MAPRED_HOME=/usr/local/hadoop/servers/hadoop-3.3.4</value>
</property>
<property>
        <name>mapreduce.map.env</name>
        <value>HADOOP_MAPRED_HOME=/usr/local/hadoop/servers/hadoop-3.3.4</value>
</property>
<property>
<name>mapreduce.reduce.eny</name>
<value>HADOOP_MAPRED_HOME=/usr/local/hadoop/servers/hadoop-3.3.4</value>
</property>
EOL
sudo sed -i "/<configuration>/r $temp_file" $file_path
rm -f $temp_file



file_path="/usr/local/hadoop/etc/hadoop/yarn-site.xml"
cat > $temp_file <<EOL
<property>
<name>yarn.resourcemanager.hostname</name>
<value>$hostname</value>
</property>
<property>
<name>yarn.nodemanager.aux-services</name>
<value>mapreduce_shuffle</value>
</property>
<property>
<name>yarn.nodemanager.pmem-check-enabled</name>
<value>false</value>
</property>
<property>
<name>yarn.nodemanager.vmem-check-enabled</name>
<value>false</value>
</property>
<property>
<name>yarn.1og-aggregation-enable</name>
<value>true</value>
</property>
<property>
<name>yarn.log.server.url</name>
<value>http://$hostname:19888/jobhistory/logs</value>
</property>
<property>
<name>yarn.log-aggregation.retain-seconds</name>
<value>604800</value>
</property>
EOL
sudo sed -i "/<configuration>/r $temp_file" $file_path
rm -f $temp_file

#初始化集群
hdfs namenode -format

echo "集群初始化完成"
echo "启动集群命令：start-all.sh"
echo "关闭集群命令：stop-all.sh"
echo "检查集群命令：jps"	
```

