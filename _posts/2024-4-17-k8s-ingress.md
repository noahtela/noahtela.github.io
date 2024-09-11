---
layout:     post
title:      "k8s-ingress"
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

> “ingress”


<p id = "build"></p>

实验环境：

| ip              | 操作系统 | 主机名 |
| --------------- | -------- | ------ |
| 192.168.171.151 | centos7  | master |
| 192.168.171.152 | centos7  | node1  |
| 192.168.171.154 | centos7  | node2  |



# 一、Ingress 介绍 

Ingress 在 Kubernetes 中是一种用于管理入站网络流量的 API 资源。它允许你将外部流量引导到 Kubernetes 集群内部的服务。简单来说，Ingress 充当了集群内服务和外部网络之间的“门户”，通过定义规则来控制流量的路由。这些规则通常基于域名和路径，并且可以配置负载均衡、SSL 终止等功能。通过 Ingress，可以将外部流量安全地引导到 Kubernetes 集群内的应用程序，从而实现灵活的网络流量管理。

**ingress的功能**

1. **路由规则**: Ingress 允许你定义基于 HTTP/HTTPS 请求的路由规则，根据请求的主机名、路径等条件将流量引导到不同的后端服务。
2. **负载均衡**: Ingress 可以在多个后端服务之间均衡分配流量，以确保服务的高可用性和性能。
3. **SSL 终止**: Ingress 可以在集群外部终止 SSL/TLS 加密，解密传入的加密流量并将其转发到后端服务，减轻了后端服务的负担。
4. **名称和虚拟主机支持**: Ingress 允许为不同的域名或虚拟主机配置不同的后端服务，实现多租户或多项目的支持。
5. **路径基础的路由**: 可以基于请求的路径将流量路由到不同的后端服务，允许在同一主机上托管多个服务。
6. **HTTP 请求重定向**: Ingress 支持 HTTP 请求的重定向，可以将请求从一个 URL 重定向到另一个 URL，实现 URL 重定向、URL 重写等功能。
7. **基于注释的配置**: 通过注释，可以对 Ingress 资源进行更细粒度的配置，如设置代理超时、自定义负载均衡策略等。

**核心概念**

- ingress：kubernetes中的一个对象，作用是定义请求如何转发到service的规则
- ingress controller：具体实现反向代理及负载均衡的程序，对ingress定义的规则进行解析，根据配置的规则来实现请求转发，实现方式有很多，比如Nginx, Contour, Haproxy等等



它们之间的一些区别和联系：

- **定义**: Ingress 是一个 Kubernetes API 资源，是集群中的一种配置对象；而 Ingress Controller 是一个独立于 Kubernetes 的控制器，负责实际处理流量的转发。
- **功能**: Ingress 定义了路由规则和流量管理的配置；而 Ingress Controller 负责根据这些配置来实现实际的流量控制。
- **多样性**: Kubernetes 可以有多个 Ingress Controller 实现，比如常用的有 Nginx Ingress Controller、Traefik、HAProxy 等。每个 Ingress Controller 可能具有不同的功能和性能特点，以及适用于不同场景的配置选项。
- **灵活性**: Ingress 控制器的选择提供了灵活性，可以根据需要选择最适合特定需求的控制器，并在集群中同时使用多个不同的 Ingress Controller。

Ingress 定义了配置规则，而 Ingress Controller 则负责实际的流量控制。

**使用 Ingress Controller 代理 k8s 内部应用的流程** 

（1）部署 Ingress controller，我们 ingress controller 使用的是 nginx 

（2）创建 Service，用来分组 pod 

（3）创建 Pod 应用，可以通过控制器创建 pod 

（4）创建 Ingress http，测试通过 http 访问应用 

（5）创建 Ingress https，测试通过 https 访问应用 



# 二、基于 keepalive 实现 nginx-ingress-controller 高可用



Ingress-controller 根据 Deployment+ nodeSeletor+pod 反亲和性方式部署在 k8s 指定的两个 work 节点，nginx-ingress-controller 这个 pod 共享宿主机 ip，然后通过 keepalive+nginx实现 nginx-ingress-controller 高可用 。

```
[root@master ~]# kubectl label node node1 kubernetes.io/ingress=nginx
[root@master ~]# kubectl label node node2 kubernetes.io/ingress=nginx
```



## 上传镜像

```
[root@node1 ~]# docker load -i kube-webhook-certgen-v1.1.0.tar.gz
[root@node1 ~]# docker load -i ingress-nginx-controllerv1.1.0.tar.gz
[root@node2 ~]# docker load -i kube-webhook-certgen-v1.1.0.tar.gz
[root@node2 ~]# docker load -i ingress-nginx-controllerv1.1.0.tar.gz
```



```
[root@master ~]# vim ingress-deploy.yaml 


[root@master ~]# kubectl apply -f ingress-deploy.yaml
```

```
[root@master ~]# kubectl get pods -n ingress-nginx -o wide
```

![image-20240417102852139](\img\springBoot\image-20240417102852139.png)



## 通过 keepalive+nginx 实现 nginx-ingress-controller 高可用



在 node1 和 node2 上安装nginx和keepalive

```
yum install nginx keepalived nginx-mod-stream -y
```

配置nginx

```
vim /etc/nginx/nginx.conf
```

配置keepalived

```
vim /etc/keepalived/keepalived.conf
```

编写检查脚本

```
vim  /etc/keepalived/check_nginx.sh

#!/bin/bash

A=$(ps -C nginx --no-header | wc -l)
if [ $A -eq 0 ]
then
	systemctl start nginx
	sleep 2

	if [ $(ps -C nginx --no-header | wc -l) -eq 0 ]
	then
		systemctl stop keepalived
	fi
fi
```

```
chmod +x  /etc/keepalived/check_nginx.sh
```

启动nginx和keepalived

```
systemctl daemon-reload
systemctl enable nginx keepalived
systemctl start nginx
systemctl start keepalived
ip a | grep 199
```

![image-20240417104535459](\img\springBoot\image-20240417104535459.png)



## 测试 Ingress HTTP 代理 k8s 内部站点

1.部署后端 tomcat 服务

```
[root@master ~]# vim ingress-demo.yaml
[root@master ~]# kubectl apply -f ingress-demo.yaml 
[root@master ~]# kubectl get pods
```

![image-20240417105044316](\img\springBoot\image-20240417105044316.png)

2.编写 ingress 规则

```
[root@master ~]# vim ingress-myapp.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: ingress-myapp
  namespace: default
  annotations:
    kubernetes.io/ingress.class: "nginx"
spec:
  rules: #定义后端转发的规则
  - host: tomcat.lucky.com #通过域名进行转发
    http:
      paths:
      - path: / #配置访问路径，如果通过 url 进行转发，需要修改；空默认为访问的路径为"/"
        pathType: Prefix
        backend: #配置后端服务
          service:
            name: tomcat
            port:
              number: 8080
  - host: tomcat.lucky.com1 #通过域名进行转发（写第二个规则）
    http:
      paths:
      - path: / #配置访问路径，如果通过 url 进行转发，需要修改；空默认为访问的路径为"/"
        pathType: Prefix
        backend: #配置后端服务
          service:
            name: tomcat
            port:
              number: 8080
```

```
kubectl apply -f ingress-myapp.yaml
```

测试

![image-20240417105257270](\img\springBoot\image-20240417105257270.png)

![image-20240417105649839](\img\springBoot\image-20240417105649839.png)
