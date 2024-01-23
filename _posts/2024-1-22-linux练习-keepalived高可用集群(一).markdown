---

layout:     post
title:      "linux练习-keepalived高可用集群(一)"
subtitle:   " \"linux\""
date:       2024-1-23 17:25:49
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

# keepalived高可用集群(一)



Linux高可用集群(High Availability Cluster)简称HA: pacemaker keepalived

## 一、什么是高可用集群

​    高可用集群就是当某一个节点或服务器发生故障时，另一个节点能够自动且立即向外提供服务，即将有故障节点上的资源转移到另一个节点上去，这样另一个节点有了资源既可以向外提供服务。高可用集群是用于单个节点发生故障时，能够自动将资源、服务进行切换，这样可以保证服务一直在线。在这个过程中，对于客户端来说是透明的。

 

## 二、高可用集群的衡量标准

高可用集群一般是通过系统的可靠性(reliability)和系统的可维护性(maintainability)来衡量的。通常用平均无故障时间（MTTF）来衡量系统的可靠性，用平均维护 时间（MTTR）来衡量系统的可维护性。因此，一个高可用集群服务可以这样来定义：HA=MTTF/(MTTF+MTTR)*100%。

一般高可用集群的标准有如下几种：

99%：表示 一年宕机时间不超过4天

99.9% ：表示一年宕机时间不超过10小时

99.99%： 表示一年宕机时间不超过1小时

99.999% ：表示一年宕机时间不超过6分钟

 

## 三、高可用集群的三种方式

### （1）、主从方式（非对称）

这种方式组建的高可用集群通常包含2个节点和一个或多个服务器，其中一台作为主节点（active），另一台作为备份节点（standy）。备份节点随时都在检测主节点的健康状况，当主节点发生故障时，服务会自动切换到备份节点上以保证服务正常运行。

这种方式下的高可用集群其中的备份节点平时不会启动服务，只有发生故障时才会启动

 

### （2）、对称方式

这种方式一般包含2个节点和一个或多个服务，其中每一个节点都运行着不同的服务且相互作为备份，两个节点互相检测对方的健康状况，这样当其中一个节点发生故障时，该节点上的服务会自动切换到另一个节点上去。这样可以保证服务正常运行。可用性会相对降低

 

### （3）、多机方式

这种集群包含多个节点和多个服务。每一个节点都可能运行或不运行服务，每台服务器都监视着几个指定的服务，当其中的一个节点发生故障时，会自动切换到这组服务器中的一个节点上去。



## 四、keepalived理论工作原理

keepalived是以VRRP协议为基础实现的，VRRP全称Virtual Router Redundancy Protocol，即虚拟路由冗余协议。

虚拟路由冗余协议是实现路由器高可用的协议，即将N台提供相同功能的路由器组成一个路由器组，这个组里面有一个master和多个backup，master上面有一个对外提供服务的vip（该路由器所在局域网内其他机器的默认路由为该vip），master会发组播 广播 或单播，当backup收不到vrrp包时就认为master宕机，这时就需要根据VRRP的优先级来选举一个backup成为master。这样的话就可以保证路由器的高可用了。

 

keepalived 工作在osi的三层、四层和七层原理

Layer3：工作在三层时，keepalived会定期向热备组中的服务器发送一个ICMP数据包，来判断某台服务器是否故障，如果故障则将这台服务器从热备组移除。

Layer4：工作在四层时，keepalived以TCP端口的状态判断服务器是否故障，比如检测mysql 3306端口，如果故障则将这台服务器从热备组移除

Layer7：工作在七层时，keepalived根据用户设定的策略判断服务器上的程序是否正常运行，如果故障则将这台服务器从热备组移除

## 五、两台nginx服务器实现简单的高可用集群

两台nginx服务器实现简单的高可用集群，解决nginx单点故障问题

### 1、下载keepalived-2.0.20.tar.gz

```shell
wget https://www.keepalived.org/software/keepalived-2.0.20.tar.gz
```



### 2、安装依赖

```shell
yum install -y openssl openssl-devel libnl3-devel.x86_64 libnfnetlink-devel.x86_64 ipvsadm
```

### 3、解压并安装

```shell
tar xvf keepalived-2.0.20.tar.gz
```

```shell
cd keepalived-2.0.20
#编译安装
./configure && make && make install
```

### 4、复制配置文件并启动

```shell
mkdir /etc/keepalived
cp /usr/local/etc/keepalived/keepalived.conf /etc/keepalived/
cp /usr/local/etc/sysconfig/keepalived /etc/sysconfig/
cp /usr/local/sbin/keepalived /usr/sbin/
keepalived -v
```

也可以使用systemctl方式启动keepalived服务

```
systemctl stop keepalived.service
systemctl start keepalived.service
```

### 5、编辑配置文件

```shell
#vi /etc/keepalived/keepalived.conf

#主
global_defs {
       router_id haweb_1
       }
vrrp_sync_group VGM {
       group {
       VI_HA
       }
}
vrrp_instance VI_HA {
      state MASTER
      interface ens33
      lvs_sync_daemon_inteface ens33
      virtual_router_id 51
      priority 90   #权值范围1-255,越大越高
      advert_int 5
      authentication {
          auth_type PASS
          auth_pass zhangbin
          }
      virtual_ipaddress {
          192.168.171.160/24 dev ens33
          }
}

```

```shell
#vi /etc/keepalived/keepalived.conf

#从
global_defs {
       router_id haweb_1
       }
vrrp_sync_group VGM {
       group {
       VI_HA
       }
}
vrrp_instance VI_HA {
      state SLAVE  
      interface ens33
      lvs_sync_daemon_inteface ens33
      virtual_router_id 51
      priority 80   
      advert_int 5
      authentication {
          auth_type PASS
          auth_pass zhangbin
          }
      virtual_ipaddress {
          192.168.171.160/24 dev ens33
          }
}

```



![image-20240123170042646](\img\springBoot\image-20240123170042646.png)



主服务器宕机后

![image-20240123172008740](\img\springBoot\image-20240123172008740.png)

至此 一个简单的keepalived高可用集群搭建完成。
