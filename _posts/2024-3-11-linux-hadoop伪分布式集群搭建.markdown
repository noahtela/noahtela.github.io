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

