---
layout:     post
title:      "linux-hadoop完全分布式集群搭建"
subtitle:   " \"linux\""
date:       2024-3-16 17:22:49
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

# hadoop完全分布式集群搭建



| 操作系统 | IP              | 角色   |
| -------- | --------------- | ------ |
| centos 7 | 192.168.171.181 | master |
| centos 7 | 192.168.171.182 | node1  |
| centos 7 | 192.168.171.183 | node2  |



系统镜像：`https://www.123pan.com/s/gIBcVv-2BVN3.html提取码:p8Ai`

JDK安装包：`https://www.123865.com/s/gIBcVv-lETO3`提取码:YwCN

Hadoop安装包：`https://www.123865.com/s/gIBcVv-7ETO3`提取码:WkCk

实验说明：本实验用到三台机器，两台从服务器，一台主服务器，**只需要把三台机器配置好免密登录即可运行接下来的脚本**

以下脚本将会为你更改主机名、安装jdk和hadoop及其初始化



## 一、从服务器(两台)

将jdk安装包上传至 **/opt**目录下

新建脚本

```shell
vi 	java.sh
```



复制下面shell脚本到java.sh中

```shell
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

运行脚本

```shell
source java.sh
```



## 二、主服务器



```shell
vi hadoop.sh
```



```shell
#!/bin/bash

# -------------------------------------------------
# Script Name: hadoop.sh
# Author: Xavier
# Date: 2024-03-12
# -------------------------------------------------
clear
sed -i '/^SELINUX=/s/enforcing/disabled/' /etc/selinux/config
echo "本脚本适用于centos 7系统"
echo "使用前请确保主机间已实现免密登录"
echo "本脚本运行过程中可为你个性化设置主机名,安装JDK,安装及初始化hadoop集群"
read -p "Do you want to continue? (yes/no): " choice

case "$choice" in
yes | y | Y | YES)
	echo "Continuing script execution..."
	;;
no | n | N | NO)
	echo "Exiting script..."
	return 1
	;;
*)
	echo "Invalid choice. Exiting script..."
	return 1
	;;
esac

# 检查用户是否为root
if [ "$(id -u)" != "0" ]; then
	echo "脚本运行中需要root权限，请提权后重试" 1>&2
	return 1
fi
setenforce 0
iptables -F
systemctl stop firewalld
systemctl disable firewalld >&/dev/null
systemctl stop NetworkManager
systemctl disable NetworkManager >&/dev/null
#获取本机ip
ip=$(ip addr show ens33 | grep 'inet ' | awk '{print $2}' | cut -d/ -f1)
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

if [ $java_status -eq 0 ]; then
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

echo "本机IP为 $ip"
echo "请为本机设置主机名:"
read hostname
if [ -z "$hostname" ]; then
	# 如果用户没有输入，设置默认主机名为hadoop1
	hostname="hadoop1"
	echo "设置默认主机名为hadoop1"
fi
while true; do
	echo "请输入第一台从结点主机ip"
	read node_one_ip
	if [ -z "$node_one_ip" ]; then
		echo -e "\033[31m 注意 \033[0m"
	else
		break
	fi
done
while true; do
	echo "请输入第一台从结点主机名"
	read node_one_name
	if [ -z "$node_one_name" ]; then
		echo -e "\033[31m 注意 \033[0m"
	else
		break
	fi
done

while true; do
	echo "请输入第二台从结点主机ip"
	read node_two_ip
	if [ -z "$node_two_ip" ]; then
		echo -e "\033[31m 注意 \033[0m"
	else
		break
	fi
done
while true; do
	echo "请输入第二台从结点主机名"
	read node_two_name
	if [ -z "$node_two_name" ]; then
		echo -e "\033[31m 注意 \033[0m"
	else
		break
	fi
done
echo "正在将ip和hostname添加至/etc/hosts"
echo "$ip $hostname" >>/etc/hosts
echo "$node_one_ip $node_one_name" >>/etc/hosts
echo "$node_two_ip $node_two_name" >>/etc/hosts
systemctl restart network
echo "主机名已修改为$hostname"

echo "正在安装jdk..."
rpm -qa | grep java | xargs rpm -e >&/dev/null
tar -zxvf /opt/jdk-8u171-linux-x64.tar.gz -C /usr/local/ >&/dev/null
ln -s /usr/local/jdk1.8.0_171 /usr/local/jdk

touch /opt/temp.txt
echo 'JAVA_HOME=/usr/local/jdk' >>/opt/temp.txt
echo 'PATH=$JAVA_HOME/bin:$PATH' >>/opt/temp.txt
echo 'CLASSPATH=$JAVA_HOME/jre/lib/ext:$JAVA_HOME/lib/tools.jar' >>/opt/temp.txt
echo 'export PATH JAVA_HOME CLASSPATH' >>/opt/temp.txt
sed -i -e '$r /opt/temp.txt' /etc/profile
rm -f /opt/temp.txt
source /etc/profile #重载环境变量
echo "jdk安装完成"

#安装hadoop
echo "开始安装hadoop..."
tar -zxvf /opt/hadoop-3.3.4.tar.gz -C /usr/local/ >&/dev/null
ln -s /usr/local/hadoop-3.3.4 /usr/local/hadoop

echo 'export HADOOP_HOME=/usr/local/hadoop' >>/opt/temp.txt
echo 'export PATH=$PATH:$HADOOP_HOME/bin:$HADOOP_HOME/sbin' >>/opt/temp.txt
sed -i -e '$r /opt/temp.txt' /etc/profile
rm -f /opt/temp.txt

source /etc/profile #重载环境变量

#安装完成，开始修改配置文件

echo 'export JAVA_HOME=/usr/local/jdk          #这里要修改为自己jdk的路径' >>/opt/temp.txt
echo 'export HDFS_NAMENODE_USER=root           #这行设置HDFS的NameNode用户为root' >>/opt/temp.txt
echo 'export HDFS_DATANODE_USER=root           #这行设置HDFS的DataNode用户为root' >>/opt/temp.txt
echo 'export HDFS_SECONDARYNAMENODE_USER=root  #这行设置HDFS的SecondaryNameNode用户为root' >>/opt/temp.txt
echo 'export YARN_RESOURCEMANAGER_USER=root    #这行设置YARN的ResourceManager用户为root' >>/opt/temp.txt
echo 'export YARN_NODEMANAGER_USER=root        #这行设置YARN的NodeManager用户为root' >>/opt/temp.txt
sed -i -e '$r /opt/temp.txt' /usr/local/hadoop/etc/hadoop/hadoop-env.sh
rm -f /opt/temp.txt

# core-site.xml
file_path="/usr/local/hadoop/etc/hadoop/core-site.xml"
temp_file="temp.xml"
cat >$temp_file <<EOL
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

#hdfs-site.xml
file_path="/usr/local/hadoop/etc/hadoop/hdfs-site.xml"
temp_file="temp.xml"
cat >$temp_file <<EOL
<property>
<name>dfs.replication</name>
<value>2</value>
</property>
<property>
<name>dfs.namenode.secondary.http-address</name>
<value>$node_one_name:9868</value>
</property>
EOL
sudo sed -i "/<configuration>/r $temp_file" $file_path
rm -f $temp_file

# mapred-site.xml
file_path="/usr/local/hadoop/etc/hadoop/mapred-site.xml"
cat >$temp_file <<EOL
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
        <value>HADOOP_MAPRED_HOME=${HADOOP_HOME}</value>
</property>
<property>
        <name>mapreduce.map.env</name>
        <value>HADOOP_MAPRED_HOME=${HADOOP_HOME}</value>
</property>
<property>
<name>mapreduce.reduce.eny</name>
<value>HADOOP_MAPRED_HOME=${HADOOP_HOME}</value>
</property>
EOL
sudo sed -i "/<configuration>/r $temp_file" $file_path
rm -f $temp_file

file_path="/usr/local/hadoop/etc/hadoop/yarn-site.xml"
cat >$temp_file <<EOL
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

> /usr/local/hadoop/etc/hadoop/workers
echo "$node_one_name" >>/usr/local/hadoop/etc/hadoop/workers
echo "$node_two_name" >>/usr/local/hadoop/etc/hadoop/workers

scp -r /usr/local/hadoop root@$node_one_ip:/usr/local/hadoop
scp -r /etc/profile root@$node_one_ip:/etc/profile
scp -r /etc/hosts root@$node_one_ip:/etc/hosts
scp -r /usr/local/hadoop root@$node_two_ip:/usr/local/hadoop
scp -r /etc/profile root@$node_two_ip:/etc/profile
scp -r /etc/hosts root@$node_two_ip:/etc/hosts


#初始化集群
hdfs namenode -format

echo -e "\033[31m 集群初始化成功 \033[0m"
echo -e "\033[31m jdk安装目录：/usr/local/jdk \033[0m"
echo -e "\033[31m hadoop安装目录：/usr/local/hadoop \033[0m"
echo -e "\033[31m 请重启所有主机后继续\033[0m"
echo -e "\033[31m 请重启所有主机后继续\033[0m"
echo -e "\033[31m 请重启所有主机后继续\033[0m"
echo -e "\033[31m 主节点启动:start-dfs.sh  start-yarn.sh \033[0m"
```



运行脚本

```shell
source hadoop.sh
```



按照提示输入主机名和从服务器的ip及主机名

脚本执行完成后重启，在主节点使用`start-all.sh`命令，即可启动集群

![image-20240318103757420](\img\springBoot\image-20240318103757420.png)

![image-20240316172148821](\img\springBoot\image-20240316172148821.png)



