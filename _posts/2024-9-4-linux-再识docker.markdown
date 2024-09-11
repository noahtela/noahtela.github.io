---
layout:     post
title:      "linux-再识docker"
subtitle:   " \"linux\""
date:       2024-9-4 9:22:12
author:     "yangsir"
header-img: "img/bg-1.jpg"
catalog: true
tags:
    - 笔记
    - Python
    - GUI编程


---

> “Yeah It's on. ”


<p id = "build"></p>

# 再识Docker

## 容器化技术实现的前置

- 操作系统的 NameSpace 隔离系统资源技术，通过隔离网络、PID 进程、系统信号量、文件系统挂载、主机名与域名, 来实现在同一宿主机系统中，运行不同的容器，而每个容器之间相互隔离，运行互不干扰。
- 使用系统的 Cgroups 系统资源配额功能, 限制资源包括: CPU、Memory、Blkio(块设备)、Network。
- 通过 OverlayFS 数据存储技术, 实现容器镜像的物理存储与新建容器存储。



## Docker镜像存储-OverlayFS和联合挂载技术

OverlayFS 通过三个目录：lower 目录、upper 目录、以及 work 目录实现,其中 lower 目录可以是多个, upper 目录为可以进行读写操作的目录, work 目录为工作基础目录,挂载后内容会被清空,且在使用过程中其内容用户不可见,最后联合挂载完成给用户呈现的统一视图称为
merged 目录。



### 1、Docker中镜像存储

Docker中的镜像采用分层构建设计，每个层可以称之为“layer”，这些layer被存放在/var/lib/docker//目录下。这里的storage-driver可以有很多种如:AUFS、OverlayFS、VFS、Brtfs等。可以通过docker info命令查看存储驱动。中的镜像采用分层构建设计，每个层可以称之为“layer”，这些layer被存放在/var/lib/docker/目录下。这里的storage-driver可以有很多种如:AUFS、OverlayFS、VFS、Brtfs等。可以通过docker info命令查看存储驱动。
![image-20240904101214794](\img\springBoot\image-20240904101214794.png)

（注: 通常ubuntu类的系统默认采用的是AUFS，centos7.1+系列采用的是OverlayFS。）

### 2、OverlayFS

(1) 简介
OverlayFS是一种堆叠文件系统。它依赖并建立在其它的文件系统之上（例如ext4fs和xfs等等）。并不直接参与磁盘空间结构的划分，仅仅将原来底层文件系统中不同的目录进行“合并”，然后向用户呈现，这也就是联合挂载技术。

对比AUFS，OverlayFS更高效，简便，故而使用率更高。但是在Linux系统中，其内核为Docker提供的存储驱动OverlayFS目前只有两种：overlay和overlay2。overlay2是相对于overlay的一种改进，在inode利用率方面比overlay更有效。但是overlay有环境需求：docker版本17.06.02+，宿主机文件系统需要时ext4或xfs格式。



(2) overlayfs联合挂载：
overlayfs通过三个目录：lower目录、upper目录、以及work目录实现，其中lower目录可以是多个，work目录为工作基础目录，挂载后内容会被清空，且在使用过程中其内容用户不可见，最后联合挂载完成给用户呈现的统一视图称为为merged目录。docker使用overlay文件系统来构建和管理镜像与容器的磁盘结构。overlay文件系统分为lowerdir、upperdir、merged， 对外统一展示为merged，upperdir和lower的同名文件会被upperdir覆盖。



### 3、举个栗子

```shell
1.创建文件
[root@master ~]# mkdir /lower{1..3}
[root@master ~]# mkdir /upper /work /merged
2.挂载文件系统
[root@master ~]# mount -t overlay overlay -o lowerdir=/lower1:/lower2:/lower3,upperdir=/upper,workdir=/work /merged
#其中lower1:lower2:lower3 表示不同的lower层目录，不同的目录使用":"分隔，层次关系依次为lower1>lower2>lower3 （注：多lower层功能支持在Linux-4.0合入，Linux-3.18版本只能指定一个lower dir；然后upper和work目录分别表示upper层目录和文件系统挂载后用于存放临时和间接文件的工作基目录（work base dir），最后的merged目录就是最终的挂载点目录。），若执行顺利，执行以上命令完成之后，overlayfs就成功挂载到merged目录下了。

3.查看挂载
[root@master ~]# mount | grep merged
4.在/upper 目录中写入文件,在 merged 中可以显示
[root@master /]# touch /upper/upper.txt
[root@master /]# ll /merged/
total 0
-rw-r--r-- 1 root root 0 Mar 14 02:17 upper.txt
5. 在 merged 中写入文件, 实际存储到了/uppper
[root@master /]# touch /merged/d.txt
[root@master /]# ll /upper/
total 0
-rw-r--r-- 1 root root 0 Mar 14 02:19 d.txt
注:如果没有 upperdir， merged 是只读的
[root@node-2 overlay2]# umount /merged
[root@node-2 overlay2]# mount -t overlay overlay -o lowerdir=/lower1:/lower2 /merged
[root@master /]# touch /merged/c.txt
touch: cannot touch ‘/merged/c.txt’: Read-only file system
```



![image-20240904102952448](\img\springBoot\image-20240904102952448.png)





## Docker C/S 模式



Docker 客户端和服务端是使用 Socket 方式连接，主要有以下几种方式：

1. 本地的 socket 文件 unix:///var/run/docker/sock （默认）
2. tcp://host:prot （演示）
3. fd://socketfd

### Docker 默认连接方式

未启动的状态, 说明 Docker 在默认情况下使用本地的 var/run/docker.sock 连接

```shell
[root@master ~]# service docker stop
[root@ master ~]# docker info
Client:
Debug Mode: false
Server:
ERROR: Cannot connect to the Docker daemon at unix:///var/run/docker.sock. Is the docker daemon running?
errors pretty printing info
```



### 设置 Docker 远程使用 TCP 的连接方式

```shell
#打开 sock 与 tcp 连接方式

[root@ master ~]# vim /usr/lib/systemd/system/docker.service
ExecStart=/usr/bin/dockerd -H fd:// --containerd=/run/containerd/containerd.sock
#修改为:
ExecStart=/usr/bin/dockerd -H tcp://0.0.0.0:2375 -H unix://var/run/docker.sock -H fd:// --containerd=/run/containerd/containerd.sock
```

重启docker

![image-20240904105146000](\img\springBoot\image-20240904105146000.png)
![image-20240904105242083](\img\springBoot\image-20240904105242083.png)



## Docker 应用程序运行条件

1. 计算机硬件: CPU、内存、磁盘、显卡、网卡(物理机/虚拟机)。
2. 支持运行 Docker 的操作系统 (NS、Cgroups、OverlayFS)。
3. 安装 Docker 服务，并且能够正常运行。
4. 需要可以运行在 Docker 里面的镜像, 镜像来自本地、docker hub、远程私有仓库。
5. 在镜像加载需要运行的程序（最终目的）。

## Docker网络模式基本介绍

Docker 单机网络模式分为以下几种：

1. bridge NetWork，启动容器时使用--net=bridge参数指定，默认设置。
2. Host NetWork ，启动容器时使用--net=host参数指定。
3. None NetWork， 启动容器时使用--net=none参数指定。
4. Container NetWork，启动容器时使用--net=container:NAME_or_ID参数指定。

### host模式

如果启动容器的时候使用host 模式，那么这个容器将不会获得一个独立的Network Namespace，而是和宿主机共用一个Network Namespace。容器将不会虚拟出自己的网卡，配置自己的IP 等，而是使用宿主机的IP 和端口。但是容器的其他方面，如文件系统、进程列表等还是和宿主机隔离的。

### bridge 模式

bridge 模式是Docker 默认的网络设置，此模式会为每一个容器分配Network Namespace、设置IP等，并将一个主机上的Docker 容器连接到一个虚拟网桥上。
当Docker进程启动时，会在主机上创建一个名为docker0的虚拟网桥，此主机上启动的Docker容器会连接到这个虚拟网桥上。虚拟网桥(根据MAC地址进行数据交换)的工作方式和物理交换机类似，这样主机上的所有容器就通过交换机连在了一个二层网络中。从docker0子网中分配一个IP给容器使用，并设置docker0的IP地址为容器的默认网关。在主机上创建一对虚拟网卡veth pair设备，Docker将veth pair设备的一端放在新创建的容器中，并命名为eth0（容器的网卡），另一端放在主机中，以vethxxx这样类似的名字命名，并将这个网络设备加入到docker0网桥中。可以通过brctl show命令查看。如果不写--net参数，就是bridge模式。使用docker run -p时，docker实际是在iptables做了DNAT规则，实现端口转发功能。可以使用iptables -t nat -vnL查看。



### container 模式

Container 模式指定新创建的容器和已经存在的一个容器共享一个Network Namespace，而不是和宿主机共享。新创建的容器不会创建自己的网卡，配置自己的IP，而是和一个指定的容器共享IP、端口范围等。同样，两个容器除了网络方面，其他的如文件系统、进程列表等还是隔离的。两个容器的进程可以通过localhost 网卡设备通信。



### none 模式

使用none 模式，Docker 容器拥有自己的Network Namespace，但是，并不为Docker 容器进行任何网络配置。也就是说，这个Docker 容器没有网卡、IP、路由等信息。需要我们自己为Docker 容器添加网卡、配置IP 等。

## 容器网络操作

