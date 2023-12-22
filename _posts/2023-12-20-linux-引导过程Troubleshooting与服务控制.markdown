---
  layout:     post
title:      "引导过程Troubleshooting与服务控制"
subtitle:   " \"linux\""
date:       2023-12-22 14:28:12
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

# 引导过程Troubleshooting与服务控制



## 一、操作系统的引导过程



**linux引导过程：开机自检、MBR引导、GRUB菜单、加载linux内核、init进程初始化**

### 1.1、开机自检

根据主板BIOS的设置，对硬件设备进行初步检测，检测成功后移交系统控制权。

BIOS基本输出系统主要功能：

- 加电自检
- 硬件初始化
- 引导操作系统（BOOT）

系统在没有进入之前，会先使用/boot

### 1.2、MBR引导（446B）

当从本机硬盘中启动系统时，首先根据硬盘**第一个扇区**中MBR的位置，将系统控制权传递给包含操作系统引导文件的分区（/boot），或者直接根据MBR记录中的引导信息调用启动菜单（如GRUB菜单）

### 1.3、GRUB菜单

![image-20231222134924880](\img\springBoot\image-20231222134924880.png)



![image-20231222135009394](\img\springBoot\image-20231222135009394.png)

对于 Linux 操作系统来说，GRUB (GRand Unified Bootloader. 统一启动加载器) 是使用最为广泛的多系统引导器程序。系统控制权传递给 GRUB 以后，将会显示启动菜单给用户选择，并根据所选项(或采用默认值)加载 Linux 内核文件，然后将系统控制权转交给内核。需要注意的是，CentOS 7 采用的是 GRUB2 启动引导器



### 1.4、加载Linux内核文件

Linux 内核是一个预先编译好的特殊二进制文件，介于各种硬件资源与系统程序之间,负责资源分配与调度。内核接过系统控制权以后，将完全掌控整个 Linux 操作系统的运行过程。在Cent0S 系统中，默认的内核文件位于“/boot/vmlinuz-3.10.0-514.e17.x86 64”。

linux不代表系统，是一个内核



### 1.5、init进程初始化

为了完成进一步的系统引导过程，Linux 内核首先将系统中的/sbin/init 程序加载到内存中运行(运行中的程序称为进程)，init 进程负责完成系列的系统初始化过程、 最后等
待用户进行登录。系统中第一个启动的进程。

![image-20231222140438801](\img\springBoot\image-20231222140438801.png)

## 二、系统初始化进程及文件

### 2.1、init进程

Linux 操作系统中的进程使用数字进行标记，每个进程的身份标记号称为 PID。在引导Liux 操作系统的过程中,/sbin/init 是内核第一个加载的程序，因此 init 进程对应的 PID号总是为 1。
init 进程运行以后将陆续执行系统中的其他程序，不断生成新的进程，这些进程称为init 进程的子进程。反过来说，init 进程是这些进程的父进程，当然，这些子进程也可以进一步生成各自的子进程，依次不断繁衍下去，最终构成一棵枝繁叶茂的进程树， 共同为用户提供服务。

### 2.2、systemd

Systemd 是 Linux 操作系统的一种 nit 软件，CentOS7 系统中采用了全新的 Systemd启动方式，取代了传统的 SysVinit。Systemd 启动方式使系统初始化时诸多服务**并行**启动，大大增高了开机效率。Cent0S7 系统中"/sbin/init"是"/lib/systemd/systemd"的链接文件,换言之，CentOS7 系统中运行的第一个 init 进程是"/lib/systemd/systemd"。



systemd 守护进程负责 Linux 的系统和服务systemctl 用于控制 Systemd 管理的系统和服务状态.

eg：systemctl restart network.service

.service 描述一个系统文件

在/lib/systemd/system中可以查看

.socket 描述一个进程间通信的套接字



**Systemd的目标与Sysvinit的运行级别**

| 运行级别 | systemd的target   | 说明                                                   |
| -------- | ----------------- | ------------------------------------------------------ |
| **0**    | poweroff.target   | 关机状态，使用该级别时将会关闭主机                     |
| 1        | rescue.target     | 单用户模式，不需要密码验证即可登陆系统，多用于系统维护 |
| 2        | multi-user.target | 用户定义/域特定运行级别。默认等同于3，但是不支持网络   |
| **3**    | multi-user.target | 字符界面的完整多用户模式，大多数服务器主机运行在此级别 |
| 4        | multi-user.target | 用户定义/域特定运行级别。默认等同于3                   |
| **5**    | graphical.target  | 图形界面的多用户模式，提供了图形桌面操作环境           |
| **6**    | roboot.target     | 重新启动。使用该级别时将会重启主机                     |



## 三、服务控制及优化启动过程



### 3.1、Linux中服务的管理方式

systemctl start dhcpd

systemctl stop dhcpd



### 3.2、系统服务控制

systemctl 控制类型 服务名称[.service]

对于大多数系统服务来说:常见的几种控制类型如下所述。

- start (启动): 运行指定的系统服务程序，实现服务功能。
- stop (停止): 终止指定的系统服务程序，关闭相应的功能。
- restart (重启): 先退出，再重新运行指定的系统服务程序。
- reload (重载): 不退出服务程序，只是刷新配置，在某些服务中与 restart 的操作相同
- status(查看状态): 查看指定的系统服务的运行状态及相关信息

### 3.3、切换运行级别



#### 查看系统的target

```shell
runlevel
```

查看系统启动时默认运行的target

```
systemctl get-default
```

修改默认

```
systemctl set-default 
```







扩展：不登录更改root密码

一、重启Linux系统主机当出现引导界面时，按下键盘上的e键进入内核编辑界面

二、在 linux16 参数这行的最后面追加“rd.break”参数，然后按下 Ctrl + X 组合键来运行修 改过的内核程序

![image-20231222154611425](\img\springBoot\image-20231222154611425.png)





大约 30 秒过后，进入到系统的紧急求援模式



依次输入以下命令，等待系统重启操作完毕，然后就可以使用新密码来登录 Linux 系统了。

```shell
mount -o remount,rw /sysroot 
chroot /sysroot 
passwd 
touch /.autorelabel 
exit
reboot
```

