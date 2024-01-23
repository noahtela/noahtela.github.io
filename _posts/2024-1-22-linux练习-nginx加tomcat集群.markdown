---

  layout:     post
title:      "linux练习-nginx和tomcat集群"
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

# linux练习-nginx和tomcat集群

本实验成功搭建了一个由DNS、Nginx以及双节点Tomcat组成的高性能集群系统，实现了复杂的多域名到单一IP地址的解析策略。每个域名的访问结果独立且各异，充分体现了虚拟主机的多样性和灵活性。在处理大规模并发请求时，该集群架构采用负载均衡技术，显著提升了系统的吞吐率。同时，通过实现Tomcat的session共享，保证了用户在任何一个节点的访问体验的连贯性。借助故障转移和冗余备份等高可用策略，保证了服务的连续性和可靠性，从而确保了业务的稳定运行。



## 一、nginx反向代理

反向代理是代理服务器接受互联网上的连接请求，然后将请求转发给内部网络上的服务器，并将从服务器上得到的结果返回给互联网上请求连接的客户端，此时代理服务器对外就表现为一个反向代理服务器。

在nginx中，反向代理主要用于将请求转发到不同的后端服务器，以实现负载均衡和高可用等功能。例如，当用户请求一个网页时，nginx反向代理可以将请求转发到多个服务器，以分散服务器的负载，并提高网站的响应速度。

![image-20240123105050001](\img\springBoot\image-20240123105050001.png)

（正向代理）

![image-20240123105122781](\img\springBoot\image-20240123105122781.png)

（反向代理）



## 二、Nginx + Tomcat 构筑Web服务器集群的负载均衡

### 1、Nginx的安装补充

#### 1）Nginx的upstream负载的5种策略

##### a、轮询(默认)

**轮询**：每个请求按时间顺序逐一分配到不同的后端服务器，如果后端某台服务器宕机，故障系统被自动剔除，使用户访问不受影响。Weight 指定轮询权值，Weight值越大，分配到的访问机率越高，主要用于后端每个服务器性能不均的情况下。

##### b、ip_hash

**ip_hash** ：每个请求按访问IP的hash结果分配，这样来自同一个IP的访客固定访问一个后端服务器，有效解决了动态网页存在的session共享问题。当然如果这个节点不可用了，会发到下个节点，而此时没有session同步的话就注销掉了。

##### c、least_conn

**least_conn** ：请求被发送到当前活跃连接最少的real server上。会考虑weight的值。

##### d、url_hash

**url_hash** ：此方法按访问url的hash结果来分配请求，使每个url定向到同一个后端服务器，可以进一步提高后端缓存服务器的效率。Nginx本身是不支持url_hash的，如果需要使用这种调度算法，必须安装Nginx的hash软件包nginx_upstream_hash。

##### e、fair(第三方)

**fair** ：这是比上面两个更加智能的负载均衡算法。此种算法可以依据页面大小和加载时间长短智能地进行负载均衡，也就是根据后端服务器的响应时间来分配请求，响应时间短的优先分配。Nginx本身是不支持fair的，如果需要使用这种调度算法，必须下载Nginx的upstream_fair模块。



#### 2）环境拓展

上传压缩包

![image-20240123110840529](\img\springBoot\image-20240123110840529.png)

```shell
tar zxf ngx_cache_purge-2.3.tar.gz  -C  /usr/local/src/
tar zxf master.tar.gz  -C  /usr/local/src/

 cd /usr/local/src/nginx-1.22.1/
 # 预编译
 ./configure --prefix=/usr/local/nginx --user=www --group=www --with-http_realip_module --with-http_ssl_module --with-http_gzip_static_module --http-client-body-temp-path=/var/tmp/nginx/client --http-proxy-temp-path=/var/tmp/nginx/proxy --http-fastcgi-temp-path=/var/tmp/nginx/fcgi  --with-http_dav_module --with-http_stub_status_module --with-http_addition_module --with-http_sub_module --with-http_flv_module --with-http_mp4_module --with-pcre --with-http_flv_module --add-module=../ngx_cache_purge-2.3 --add-module=../nginx-goodies-nginx-sticky-module-ng-08a395c66e42
 
# 注意更改用户名和组
```

```shell
# 编译安装
make && make install
```

重启nginx



### 2、tomcat安装

tomcat安装及优化 略



### 3、DNS解析

![image-20240123111531282](\img\springBoot\image-20240123111531282.png)

（study.zheng）

![image-20240123111643648](\img\springBoot\image-20240123111643648.png)

(study.fan)

![image-20240123111803751](\img\springBoot\image-20240123111803751.png)

(/etc/named.conf)

### 4、修改nginx配置文件

```
http {
    include       mime.types;
    default_type  application/octet-stream;

    #log_format  main  '$remote_addr - $remote_user [$time_local] "$request" '
    #                  '$status $body_bytes_sent "$http_referer" '
    #                  '"$http_user_agent" "$http_x_forwarded_for"';

    #access_log  logs/access.log  main;

    sendfile        on;
    tcp_nopush     on;

    #keepalive_timeout  0;
    keepalive_timeout  65;
    upstream ys {
    sticky;
    ip_hash;
    server 192.168.171.154:8080;
    server 192.168.171.155:8080;
    }

    #gzip  on;

    server {
        listen       80;
        server_name  www.study.com;
        #access_log  logs/host.access.log  main;

        location / {
                proxy_pass http://ys;
                proxy_set_header Host $http_host;
        }
}
 server {
        listen       80;
        server_name  map.study.com;

        location / {
            root   html;
            index  index.html index.htm;
            proxy_set_header Host $http_host;
        }
    }
}
```



## 三、实现效果

### 1、两个域名同时指向一个ip

![image-20240123113221144](\img\springBoot\image-20240123113221144.png)

### 2、两个域名访问结果不同

![image-20240123121313892](\img\springBoot\image-20240123121313892.png)

### 3、实现负载均衡



![image-20240123121446935](\img\springBoot\image-20240123121446935.png)
