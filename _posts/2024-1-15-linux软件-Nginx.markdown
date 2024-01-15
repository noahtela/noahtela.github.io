---
  layout:     post
title:      "linux软件-Nginx"
subtitle:   " \"linux\""
date:       2024-1-15 17:18:49
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

# linux软件-Nginx



## 一、nginx简介

### 1、什么是nginx

Nginx（发音为 "engine x"）是一个开源的、高性能的、稳定的、简洁的、轻量级的HTTP和反向代理服务器，也是一个IMAP/POP3/SMTP代理服务器。Nginx是由伊戈尔·赛索耶夫为了解决C10K问题而开发的。C10K问题是指服务器同时连接数达到一万个以上的问题。Nginx是一种面向性能设计的服务器，相较于Apache、lighttpd具有占有内存少，稳定性高等优点。

Nginx可以作为一个HTTP服务器运行，其设计的目的是提供低内存使用和高并发连接。Nginx也可以作为反向代理和负载均衡器，为HTTP, HTTPS, SMTP, POP3,和 IMAP协议提供服务。

Nginx使用异步事件驱动的方式来处理请求，而不是依赖于常规的线程或进程模型，这在保证性能的同时，也保证了良好的伸缩性和资源使用效率。同时也是一个非常高效的反向代理、负载平衡。但是Nginx并不支持cgi方式运行，原因是可以减少因此带来的一些程序上的漏洞。所以必须使用FastCGI方式来执行PHP程序

总的来说，Nginx是一个在高并发环境下表现出色的Web服务器软件，它的设计使得它在处理大量并发连接时，仍能保持低内存使用和高性能。

### 2、nginx的二开——Tengine

Tengine是由阿里巴巴的高性能Web服务器项目。它基于开源的Nginx，是Nginx的一个扩展，并且完全兼容Nginx。Tengine在Nginx的基础上添加了许多高级功能和特性，以满足阿里巴巴大规模网站的需求。

Tengine的主要特性包括：

1. 动态模块加载：Tengine可以动态加载和卸载Nginx模块，而无需重新编译整个程序。
2. 高级反向代理和负载均衡功能：Tengine支持基于会话保持、URL哈希、最小连接数等算法的反向代理和负载均衡。
3. 高效的I/O事件模型：Tengine采用了异步非阻塞的I/O事件模型，可以处理大量的并发连接。
4. 强大的请求处理能力：Tengine支持大量并发连接，并且在高并发环境下仍能保持低内存使用和高性能。
5. 安全性：Tengine提供了防止DDoS攻击的功能，可以有效防止恶意攻击。
6. 其他功能：Tengine还有许多其他功能，如Gzip压缩、页面缓存、SSL/TLS支持、HTTP/2支持等。



## 二、nginx的安装与配置



### 1、查看网站服务器配置

```shell
curl -I 网站URL
```

![image-20240115163303838](\img\springBoot\image-20240115163303838.png)

很明显的注意到，百度的nginx服务器版本号被修改了

出于安全，nginx在编译前要进行优化，目的是更改源码，隐藏软件名称和版本号

### 2、安装nginx

#### 1)官网下载最新稳定版

```shell
wget https://nginx.org/download/nginx-1.24.0.tar.gz
```

#### 2)解压

```shell
tar -zxvf nginx-1.24.0.tar.gz -C /usr/local/src/
```

#### 3)更改源码隐藏软件名称和版本号

文件解压在`/usr/local/src/nginx-1.24.0/`中，该文件下src/core/nginx.h是主要配置文件

<img src="\杨sir\AppData\Roaming\Typora\typora-user-images\image-20240115164126931.png" alt="image-20240115164126931" style="zoom: 50%;" />

```shell
#define NGINX_VERSION    "8.8.8"  #修改版本号

#define NGINX_VER     "web/" NGINX_VERSION #修改服务器名称
```

另外，在`src/http/ngx_http_header_filter_module.c`中**修改HTTP头信息中的server字段，防止回显具体版本号**

![image-20240115164540929](\img\springBoot\image-20240115164540929.png)

（大概在49行）



#### 4）安装nginx依赖包

```shell
yum install -y gcc gcc-c++ autoconf automake zlib zlib-devel openssl openssl-devel  pcre pcre-devel
```



#### 5)预编译

创建nginx运行账户www并加入到www组，不允许www用户直接登录系统

```shell
groupadd www 
useradd -g www www -s /sbin/nologin 
```



```shell
./configure --prefix=/usr/local/nginx --with-http_dav_module --with-http_stub_status_module --with-http_addition_module --with-http_sub_module --with-http_flv_module --with-http_mp4_module --with-pcre --with-http_ssl_module --with-http_gzip_static_module --user=www --group=www
```

(重复编译需要执行make clean可以删除临时文件)

说明：

--with-http_dav_module  # nginx 编译时通过加入“--with-http_dav_module”可以启用对WebDav协议的支持。WebDAV （Web-based Distributed Authoring and Versioning） 一种基于 HTTP 1.1协议的通信协议。它扩展了HTTP 1.1，在GET、POST、HEAD等几个HTTP标准方法以外添加了一些新的方法，使应用程序可直接对Web Server直接读写，并支持写文件锁定(Locking)及解锁(Unlock)，还可以支持文件的版本控制。即ngx_http_dav_module模块用于通过 WebDAV 协议进行文件管理自动化。该模块处理 HTTP 和 WebDAV 的 PUT、DELETE、MKCOL、COPY 和 MOVE 方法。

--with-http_stub_status_module #获取Nginx的状态统计信息

--with-http_addition_module  #向响应内容中追加内容，比如想在站点底部追加一个js或者css，可以使用这个模块来实现，即模块ngx_http_addition_module在响应之前或者之后追加文本内容。

--with-http_sub_module  # ngx_http_sub_module模块是一个过滤器，它修改网站响应内容中的字符串，比如你想把响应内容中的‘iuwai’全部替换成‘aaaaa‘这个模块已经内置在nginx中，但是默认未安装，需要安装需要加上配置参数：--with-http_sub_module

--with-http_flv_module   #该ngx_http_flv_module模块为Flash视频（FLV）文件提供伪流服务器端支持。它会根据指定的 start 参数来指定跳过多少字节，并在返回数据前面附上 FLV 头。

location ~ \.flv$ {

  flv;

}

curl localhost/index.flv?start=10

该请求的意思是，从视频文件 index.flv 第10个字节开始读取返回，并在返回的数据上附上 FLV 头。

--with-http_mp4_module   #模块提供了对 MP4 视频的播放支持，相关的扩展名 .mp4 .m4v .m4a。

--with-http_ssl_module     #启用ngx_http_ssl_module

--with-pcre  # 支持正则表达式

#### 6）编译安装

```shell
make && make install
```

#### 7)启动nginx

```shell
/usr/local/nginx/sbin/nginx
```

#### 8)测试

```shell
curl  -I 192.168.171.153
```

![image-20240115165327555](\img\springBoot\image-20240115165327555.png)

#### 9)网站测试

![image-20240115165512078](\img\springBoot\image-20240115165512078.png)



## 三、修改nginx运行账号

### 1、查看当前运行账户

```shell
[root@localhost ~]# ps aux | grep nginx
root      26321  0.0  0.0  46052  1144 ?        Ss   16:15   0:00 nginx: master process /usr/local/nginx/sbin/nginx
www       26322  0.0  0.1  46500  2128 ?        S    16:15   0:00 nginx: worker process
root     115789  0.0  0.0 112824   980 pts/2    S+   16:57   0:00 grep --color=auto nginx

```

(此处已经更改过了)

### 2、创建nginx程序账号

```shell
 useradd -M -s /sbin/nologin www
```

### 3、修改nginx运行账号

```shell
vim /usr/local/nginx/conf/nginx.conf
```

![image-20240115170219130](\img\springBoot\image-20240115170219130.png)

修改为`user www;`

### 4、添加path变量

```shell
 ln -s /usr/local/nginx/sbin/* /usr/local/bin/
```

### 5、重启nginx



### 7、生成服务启动脚本

```shell
vim /etc/init.d/nginx
```

```shell
#!/bin/bash
# chkconfig: 2345 99 20
# description: Nginx Service Control Script
PROG="/usr/local/nginx/sbin/nginx"
PIDF="/usr/local/nginx/logs/nginx.pid"
case "$1" in
start)
netstat -anplt |grep ":80" &> /dev/null && pgrep "nginx" &> /dev/null
if [ $? -eq 0 ]
then
echo "Nginx service already running."
else
     $PROG -t &> /dev/null
if [ $? -eq 0 ] ; then 
       $PROG
echo "Nginx service start success."
else
     $PROG -t
fi
fi
   ;;
stop)
netstat -anplt |grep ":80" &> /dev/null && pgrep "nginx" &> /dev/null
if [ $? -eq 0 ]
then
kill -s QUIT $(cat $PIDF)
echo "Nginx service stop success." 
else
echo "Nginx service already stop"
fi
   ;;
restart)
    $0 stop
    $0 start
    ;;
status)
netstat -anplt |grep ":80" &> /dev/null && pgrep "nginx" &> /dev/null
if [ $? -eq 0 ]
then
echo "Nginx service is running."
else
echo "Nginx is stop."
fi
  ;; 
reload)
netstat -anplt |grep ":80" &> /dev/null && pgrep "nginx" &> /dev/null
if [ $? -eq 0 ]
then
    $PROG -t &> /dev/null
if [ $? -eq 0 ] ; then
kill -s HUP $(cat $PIDF)
echo "reload Nginx config success."
else
      $PROG -t
fi
else
echo "Nginx service is not run." 
fi
    ;;
  *)
echo "Usage: $0 {start|stop|restart|reload}"
exit 1
esac

```



代码解释：

1. `#!/bin/bash`: 这是一个shell脚本的开头，定义了脚本的解释器是bash。 
2.  `# chkconfig: 2345 99 20`: 这是一个注释，为chkconfig工具提供了运行级别和启动/停止优先级的信息。 
3. `# description: Nginx Service Control Script`: 这是一个注释，描述了这个脚本的作用。
4.  `PROG="/usr/local/nginx/sbin/nginx"`: 定义了一个变量，代表Nginx的执行文件路径。 
5. `PIDF="/usr/local/nginx/logs/nginx.pid"`: 定义了一个变量，代表Nginx的PID文件路径。 
6. case "$1" in`: 开始一个case语句，根据第一个命令行参数的值执行不同的操作。 `
7.  `7-22. `start) ... ;;`: 如果第一个命令行参数是"start"，则执行这个部分的代码，用于启动Nginx服务。 `
8.  `8-10. `netstat -anplt |grep ":80" &> /dev/null && pgrep "nginx" &> /dev/null`: 检查是否有进程正在监听80端口并且进程名为"nginx"。 `
9.  `11-16. `if [ $? -eq 0 ] ... fi`: 如果上一行的命令成功执行（即Nginx服务已经在运行），则输出"Nginx service already running."，否则尝试启动Nginx服务。 `
10.  `23-32. `stop) ... ;;`: 如果第一个命令行参数是"stop"，则执行这个部分的代码，用于停止Nginx服务。 `
11.  `33-34. `restart) ... ;;`: 如果第一个命令行参数是"restart"，则执行这个部分的代码，用于重启Nginx服务。`
12.  ` 35-42. `status) ... ;;`: 如果第一个命令行参数是"status"，则执行这个部分的代码，用于查看Nginx服务的状态。 `
13.  `43-52. `reload) ... ;;`: 如果第一个命令行参数是"reload"，则执行这个部分的代码，用于重新加载Nginx的配置文件。`
14.  ` 53-55. `*) ... ;;`: 如果第一个命令行参数不是上述任何一个值，执行这个部分的代码，输出使用方法并退出脚本。 `
15.  `56. `esac`: 结束case语句。

### 8、配置服务开机自启动

```shell
chmod +x /etc/init.d/nginx  #给脚本添加可执行权限
chkconfig --add nginx    #把nginx添加为系统服务
chkconfig nginx on      #把nginx添加开机自启动

```

## 四、设置Nginx运行进程个数

根据cpu个数，修改进程数，cpu个数可以用`top`命令查看

 在nginx.conf的全局设置中修改nginx的进程数

![image-20240115171642716](\img\springBoot\image-20240115171642716.png)

修改完成后重载nginx
