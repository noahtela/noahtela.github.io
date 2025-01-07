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

[TOC]



## 一、nginx简介

### 1、什么是nginx

Nginx（发音为 "engine x"）是一个开源的、高性能的、稳定的、简洁的、轻量级的HTTP和反向代理服务器，也是一个IMAP/POP3/SMTP代理服务器。Nginx是由伊戈尔·赛索耶夫为了解决C10K问题而开发的。C10K问题是指服务器同时连接数达到一万个以上的问题。Nginx是一种面向性能设计的服务器，相较于Apache、lighttpd具有占有内存少，稳定性高等优点。

Nginx可以作为一个HTTP服务器运行，其设计的目的是提供低内存使用和高并发连接。Nginx也可以作为反向代理和负载均衡器，为HTTP, HTTPS, SMTP, POP3,和 IMAP协议提供服务。

Nginx使用异步事件驱动的方式来处理请求，而不是依赖于常规的线程或进程模型，这在保证性能的同时，也保证了良好的伸缩性和资源使用效率。同时也是一个非常高效的反向代理、负载平衡。但是Nginx并不支持cgi方式运行，原因是可以减少因此带来的一些程序上的漏洞。所以必须使用FastCGI方式来执行PHP程序

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



## 五、Nginx运行CPU亲和力



在第四步中，只是设置了多进程，并没有真正榨干所有CPU

### 1、4核4线程配置

在/usr/local/nginx/conf/nginx.conf中全局配置中添加

```shell
  worker_cpu_affinity 0001 0010 0100 1000;
```

上面的配置表示：4核CPU，开启4个进程。0001表示开启第一个cpu内核， 0010表示开启第二个cpu内核，依次类推；有多少个核，就有几位数，1表示该内核开启，0表示该内核关闭。

![5](\img\springBoot\image-20240117155439464.png)

(我这里是两核)



#### 2、那么如果我是4线程的CPU，我只想跑两个进程呢？



```shell
worker_processes 2;
worker_cpu_affinity 0101 1010;
```

表示第一个进程在第一个和第三个cpu上运行，第二个进程在第二个和第四个cpu上运行，两个进程分别在这两个组合上轮询！



扩展：

```
2核CPU，开启2个进程
worker_processes2;
worker_cpu_affinity  01  10;
2核CPU，开启4进程
worker_processes 4;
worker_cpu_affinity  01  10  01  10;
2核CPU，开启8进程
worker_processes8;
worker_cpu_affinity  01 10 01 10 01 10 01 10;
8核CPU，开启2进程
worker_processes2;
worker_cpu_affinity  10101010 01010101;
```

### 3、自动绑定

在nginx1.9版本之后，可以支持自动绑定

```shell
 worker_cpu_affinity auto;
```



## 六、nginx最多可以打开的文件数



### 1、设置nginx最大可打开文件数

 在nginx.conf文件全局配置中添加

```
worker_rlimit_nofile 102400;
```

 当一个nginx进程打开的最多文件数目，理论值应该是最多打开文件数（ulimit -n）与nginx进程数相除，但是nginx分配请求并不是那么均匀，所以最好与`ulimit -n`的值保持一致。

![image-20240118161559123](\img\springBoot\image-20240118161559123.png)

### 2、修改系统可以打开的最大文件数

修改linux的软硬限制文件`/etc/security/limits.conf`

 在文件尾部添加如下代码：

```shell
* soft nofile 655350
* hard nofile 655350
```

![image-20240118162146782](\img\springBoot\image-20240118162146782.png)



七、Nginx事件处理模型

```shell
# vim /usr/local/nginx/conf/nginx.conf
events {
    use epoll;
    worker_connections  65535;   #单个进程允许客户端最大并发连接数
}
```

添加`use epoll`  nginx采用epoll事件模型，处理效率高

worker_connections是**单个worker进程允许客户端最大连接数**，这个数值一般根据服务器性能和内存来制定，实际最大值就是worker进程数乘以work_connections

实际我们填入一个65535，足够了，这些都算并发值，一个网站的并发达到这么大的数量，也算一个大站了！

## 八、http主体优化

### 1、开启高效传输模式

```shell
 # vim /usr/local/nginx/conf/nginx.conf
 http{
 	sendfile        on;
     tcp_nopush     on; 
 }
```

 **sendfile    on;**

 开启高效文件传输模式，sendfile指令指定nginx是否调用sendfile函数来输出文件，当nginx是一个静态文件服务器的时候，开启sendfile配置项能大大提高nginx的性能。

 **tcp_nopush   on;**

 必须在sendfile开启模式才有效，防止网络阻塞，积极的减少网络报文段的数量（将响应头和响应体两部分一起发送，而不一个接一个的发送。）

### 2、长连接超时时间

主要目的是保护服务器资源，Cpu,内存，控制连接数，因为建立连接也是需要消耗资源的

```shell
# vim /usr/local/nginx/conf/nginx.conf
keepalive_timeout  65
```

 服务器将会在这个时间后关闭连接，长连接可以减少重建连接的开销，如果设置时间过长，用户又多，长时间保持连接会占用大量资源。

### 3、限制文件上传大小

```
# vim /usr/local/nginx/conf/nginx.conf
client_max_body_size 10m;  #在40行添加
```

### 4、location匹配

#### 1）location匹配

 Nginx的location通过指定模式来与客户端请求的URI相匹配，location可以把网站的不同部分,定位到不同的处理方式上，基本语法如下：

 **location [=|~|~\*|^~] pattern {**

 **……**

 **}**  

 **注：中括号中为修饰符，即指令模式。Pattern****为url****匹配模式**

 = 表示做精确匹配，即要求请求的地址和匹配路径完全相同

 ~：正则匹配，区分大小写

 ~*：正则匹配”不区分大小写

 注：nginx支持正则匹配，波浪号（~）表示匹配路径是正则表达式，加上*变成~*后表示大小写不敏感

^~：指令用于字符前缀匹配。



##### A、精准匹配

= 用于精确字符匹配（模式），不能使用正则，区分大小写。

eg:

（1）精准匹配,浏览器输入ip地址/text.html,定位到服务器/var/www/html/text.html文件

 location = /text.html { 

  root /var/www/html;  

  index text.html;

 }

（2） 匹配命中的location，使用rewrite指令，用于转发。可以理解命中了就重定向到rewrite后面的url即可。

 location = /demo {  

   rewrite ^ http://google.com;

 }

##### B、正常匹配

 正常匹配的指令为空，即没有指定匹配指令的即为正常匹配。其形式类似 /XXX/YYY.ZZZ正常匹配中的url匹配模式可以使用正则，不区分大小写。

 location /demo {  

   rewrite ^ http://google.com;

 }

 上述模式指的是匹配/demo的url，下面的都能匹配

   http://192.168.33.10/demo

   http://192.168.33.10/demo/

   http://192.168.33.10/demo/aaa

   http://192.168.33.10/demo/aaa/bbb

   http://192.168.33.10/demo/AAA

   http://192.168.33.10/demoaaa

   http://192.168.33.10/demo.aaa

 正常匹配和前缀匹配的差别在于优先级。前缀的优先级高于正常匹配。



##### c、全匹配

 全匹配与正常匹配一样，没有匹配指令，匹配的url模式仅一个斜杠/

 location / {  

   rewrite ^ http://google.com;

 }

 匹配任何查询，因为所有请求都已 / 开头。但是正则表达式规则和一些较长的字符串将被优先查询匹配。

## 九、日志切割优化



创建日志切割脚本

```shell
# cd /usr/local/nginx/logs/
# vim cut_nginx_log.sh

#!/bin/bash
date=$(date +%F -d -1day)
cd /usr/local/nginx/logs
if [ ! -d cut ] ; then
        mkdir cut
fi
mv access.log cut/access_$(date +%F -d -1day).log
mv error.log cut/error_$(date +%F -d -1day).log
/usr/local/nginx/sbin/nginx -s reload
tar -jcvf cut/$date.tar.bz2 cut/*
rm -rf cut/access* && rm -rf cut/error*
find -type f -mtime +10 | xargs rm -rf


# 添加周期性计划任务
# chmod +x cut_nginx_log.sh  添加可执行权限

```





## 十、目录文件访问限制



 主要用在禁止目录下指定文件被访问，当然也可以禁止所有文件被访问！一般什么情况下用？比如是有存储共享，这些文件本来都只是一些下载资源文件，那么这些资源文件就不允许被执行，如sh,py,pl,php等等

####  禁止访问images下面的php程序文件

 注意:这段配置文件一定要放在下面配置的前面才可以生效。

 

```
    location ~* ^/upload/.*\.(php|php5)$

​     {

​     deny all;

​     }

 

​     location ~ .php$ {

​     try_files $uri /404.html;

​     fastcgi_pass 127.0.0.1:9000;

​     fastcgi_index index.php;

​     fastcgi_param SCRIPT_FILENAME $document_root$fastcgi_script_name;

​     include fastcgi_params;

​     }
```

##### 修改配置文件

```shell


# vim /usr/local/nginx/conf/nginx.conf

​     location ~ ^/images/.*\.(php|php5|sh|py|pl)$ {

​       deny all;

​     }
```

#### 对目录进行访问限制

##### 创建2个目录

```
mkdir -p /usr/local/nginx/html/{aa,bb}
```



##### 创建测试文件

```
echo 'aa' > /usr/local/nginx/html/aa/index.html

echo 'bb' > /usr/local/nginx/html/bb/index.html
```



##### 配置目录拒绝访问

```
# vim /usr/local/nginx/conf/nginx.conf

​     location /aa/    { return 404 ; }

​     location /bb/    { return 403 ; }
```



##### 1.1.4、 重载nginx

```shell
 nginx -s reload
```

## 十一、IP和301优化

 有时候，我们发现访问网站的时候，使用IP也是可以得，我们可以把这一层给屏蔽掉，让其直接反馈给403,也可以做跳转

### 1、修改配置文件

```
vim /usr/local/nginx/conf/nginx.conf

  server {

​    listen 80;

​    server_name www.benet.com benet.com;

​     if ($host = 192.168.1.11) {

​       rewrite ^ http://www.baidu.com;

​     }

​    }
```

### 2、403反馈的做法

```
# vim /usr/local/nginx/conf/nginx.conf

server {

​    listen 80;

​    server_name www.benet.com benet.com;

​    if ($host = 192.168.1.11) {

​     return 403;

​    }
```

### 3、配置301跳转

```
vim /usr/local/nginx/conf/nginx.conf

server {

  listen    80;

  root     html;

  server_name www.qingniao.com qingniao.com;

  if ($host = qingniao.com ) {

​    rewrite ^/(.*)$ http://www.qingniao.com/$1 permanent;

  }
```



## 十二、防盗链



 防止别人直接从你网站引用图片等链接，消耗了你的资源和网络流量，那么我们的解决办法由几种：

   1：水印，品牌宣传，你的带宽，服务器足够

   2：防火墙，直接控制，前提是你知道IP来源

   3：防盗链策略

### 1、  直接给予404的错误提示

```shell


# vim /usr/local/nginx/conf/nginx.conf

​    location / {

​      root  html;

​      index index.html index.htm;

​    } #直接在第一个localtion下面填写以下内容

​     location ~* \.(jpg|gif|swf|flv|wma|wmv|asf|mp3|mmf|zip|rar)$ {

​        root html;

​        valid_referers none blocked \*.qingniao.com qingniao.com;

​        if ($invalid_referer) {

​            return 404;

​        }

​        expires   365d;

​    }
```

```
#### 1.1、   设置图片，来做rewrite跳转



# vim /usr/local/nginx/conf/nginx.conf

location ~* \.(jpg|gif|swf|flv|wma|wmv|asf|mp3|mmf|zip|rar)$ {

​    root html;

​    valid_referers none blocked *.qingniao.com qingniao.com;

​        if ($invalid_referer) {

​         rewrite ^/ http://www.qingniao.com/img/test.png;

​         #return 302 http://www.qingniao.com/img/test.png;

​        }

​        expires   365d;

}
```

 **location** ~* **\.**(**jpg**|**gif**|**png**|**swf**|**flv**|**wma**|**wmv**|**asf**|**mp3**|**mmf**|**zip**|**rar**)$ { #需要防盗的资源

 **valid_referers** **none** **blocked** *.**qingniao.com** **qingniao.com**; #**这是可以盗链的域名或IP****地址，一般情况可以把google，baidu，sogou，soso，bing，feedsky等域名放进来

 **none** **意思是不存在的Referer头(表示空的，也就是直接访问，比如直接在浏览器打开一个图片)**

 **blocked** **意为根据防火墙伪装Referer头，如：“Referer: XXXXXXX”。**

 **server_names** **为一个或多个服务器的列表，0.5.33版本以后可以在名称中使用“\*”通配符。**

## 十四、内部身份验证

### 1、   配置认证

```
 [root@cong11 ~]# vim /usr/local/nginx/conf/nginx.conf

​     location /bbs/ {

​        auth_basic "haha";

​        auth_basic_user_file /usr/local/nginx/conf/passwd;

​    }

 

autoindex实例：

​    location /upload/ {

​      autoindex on;

​      \#index index.php

​      auth_basic "这是一个身份认证测试站点";

​      auth_basic_user_file /usr/local/nginx/conf/passwd;

​     }
```

### 2、用户创建

 

```shell
yum -y install httpd-tools #安装htpasswd工具

htpasswd -cb /usr/local/nginx/conf/passwd aaa 123

Adding password for user aaa

-c 创建passwdfile.如果passwdfile 已经存在,那么它会重新写入并删去原有内容.

-b 命令行中一并输入用户名和密码而不是根据提示输入密码，可以看见明文，不需要交互

chmod 400 /usr/local/nginx/conf/passwd 

chown www /usr/local/nginx/conf/passwd

```

### 3、创建目录

```shell
mkdir /usr/local/nginx/html/bbs
```
