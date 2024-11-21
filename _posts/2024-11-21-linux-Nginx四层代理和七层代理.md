---
layout:     post
title:      "linux-Nginx四层代理和七层代理"
subtitle:   " \"nginx\""
date:       2024-11-21 15:34:12
author:     "yangsir"
header-img: "img/bg-material.jpg"
catalog: true
tags:
    - 笔记
    - Linux
    - iptables
    - 防火墙


---

> “Yeah It's on. ”


<p id = "build"></p>

# Nginx四层代理和七层代理



**七层代理**和**四层代理**是两种常见的网络负载均衡技术，分别基于OSI模型的**应用层（Layer 7）\**和\**传输层（Layer 4）**，它们各自适用不同的场景。

## **七层代理（Layer 7 Proxy）**

#### **特点**

- 基于内容分发：
  - 可以根据URL、域名、路径、HTTP头部信息、Cookie等内容将请求分发到不同的后端服务器。
  - 适用于需要精准内容路由的场景。
- 协议支持：
  - 主要支持HTTP/HTTPS等基于应用层的协议。
- 灵活性：
  - 可以实现A/B测试、按路径或用户类型分流等功能。
  - 支持复杂的规则配置，比如将特定的API流量发送到某一组服务器。
- 资源消耗较高：
  - 需要解析和分析应用层数据，因此对CPU和内存的消耗比四层代理高



#### **典型应用场景：**

- 根据请求路径将静态文件请求（如图片、CSS、JS）发送到CDN或缓存服务器。
- 将移动端和PC端流量分发到不同的后端服务。
- 实现灰度发布或蓝绿部署，通过七层代理路由流量到不同版本的服务。
- 基于用户地理位置、设备类型分流。

#### **常见工具：**

- **Nginx**（HTTP代理）
- **HAProxy**（HTTP模式）
- **Traefik**
- **Envoy**
- **AWS ALB（Application Load Balancer）**



## **四层代理（Layer 4 Proxy）**

四层代理工作在OSI模型的**传输层**，基于TCP/UDP协议，通过监听IP地址和端口来转发流量。



#### **特点：**

- 基于网络流量分发：
  - 不关心应用层数据，只根据IP地址、端口号和传输协议（TCP/UDP）来进行负载均衡。
  - 不解析请求内容，性能更高，延迟更低。
- 协议无关性：
  - 可以处理HTTP、HTTPS、WebSocket、数据库连接、文件传输等任意基于TCP/UDP的协议。
- 资源消耗较低：
  - 不需要解析应用层协议，转发效率高，更适合高并发场景。

#### **局限性：**

- 不能基于请求内容分发流量，例如无法根据URL、域名或Cookie进行分流。
- 不支持高级功能（如A/B测试、按路径分流等）。

#### **典型应用场景：**

- 数据库负载均衡（如MySQL主从复制）。
- 转发基于TCP/UDP协议的应用流量，例如游戏服务器、VoIP、视频流。
- 高性能的HTTPS流量负载均衡（可以配合SSL卸载）。
- 高并发流量分发场景。

#### **常见工具：**

- **HAProxy**（TCP模式）
- **LVS（Linux Virtual Server）**
- **Nginx Stream模块**
- **AWS NLB（Network Load Balancer）**



## 整体对比

| **特性**         | **七层代理**                        | **四层代理**               |
| ---------------- | ----------------------------------- | -------------------------- |
| **工作层级**     | 应用层（Layer 7）                   | 传输层（Layer 4）          |
| **支持的协议**   | HTTP/HTTPS                          | TCP/UDP                    |
| **流量分发依据** | URL、域名、路径、HTTP头部、Cookie等 | IP地址、端口号、协议       |
| **性能**         | 较低，消耗更多资源                  | 高，消耗较少资源           |
| **功能**         | 智能分发，支持复杂路由规则          | 简单快速，基于网络连接分发 |
| **适用场景**     | 精准内容路由、灰度发布              | 高并发流量分发             |





## 七层代理配置文件实例

### 1、**基于域名分发**

（ps:访问 `app1.example.com` 或 `app2.example.com` 分发到不同的后端）

```nginx
server {
    listen 80;
    
    server_name app1.example.com;
    location / {
        proxy_pass http://backend_app1;
    }

    server_name app2.example.com;
    location / {
        proxy_pass http://backend_app2;
    }
}

upstream backend_app1 {
    server 192.168.1.101:8080;
    server 192.168.1.102:8080;
}

upstream backend_app2 {
    server 192.168.1.103:8080;
    server 192.168.1.104:8080;
}
```

- **proxy_pass**：将流量代理到指定的后端服务器。
- **upstream**：定义后端服务的IP和端口，可实现负载均衡。

### 2、**基于路径分发**

（ps:`/api` 和 `/static` 请求分发到不同后端服务）

```nginx
server {
    listen 80;
    server_name example.com;

    location /api/ {
        proxy_pass http://api_backend;
    }

    location /static/ {
        proxy_pass http://static_backend;
    }
}

upstream api_backend {
    server 192.168.1.101:8080;
}

upstream static_backend {
    server 192.168.1.102:8080;
}
```

## **四层代理（Layer 4 Proxy）**



```nginx
stream {
    upstream db_backend {
        server 192.168.1.101:3306;
        server 192.168.1.102:3306;
    }

    server {
        listen 3306;
        proxy_pass db_backend;
    }
}
```

## **七层代理与四层代理应用场景的区别**

| **场景**                | **七层代理（Layer 7 Proxy）**                            | **四层代理（Layer 4 Proxy）**                      |
| ----------------------- | -------------------------------------------------------- | -------------------------------------------------- |
| **请求路由**            | 可基于 HTTP 内容（如路径、域名、Cookie）精确分发         | 只能基于网络连接分发，无法解析具体的请求内容       |
| **高并发性能**          | 性能较低（需解析应用层内容）                             | 性能高（仅转发流量，无需解析）                     |
| **静态资源分发**        | 非常适合：可将特定请求转发到 CDN 或静态资源服务器        | 不适用：无法根据请求内容分发                       |
| **数据库负载均衡**      | 不适用：无法处理 TCP 流量                                | 适用：代理 TCP/UDP 流量，例如 MySQL 主从负载均衡   |
| **WebSocket/游戏服务**  | 不适用：七层代理可能破坏长连接                           | 适用：直接转发 TCP/WebSocket 长连接流量            |
| **灰度发布**            | 可实现：基于路径或域名的流量控制，分发到不同后端版本服务 | 不适用：无法精确路由到不同版本的服务               |
| **SSL 卸载**            | 支持 HTTPS 的 SSL 卸载功能                               | 可以转发 SSL 流量，但不支持卸载                    |
| **高并发 UDP 流量代理** | 不适用：七层代理不支持 UDP 协议                          | 非常适合：支持高并发的 UDP 流量（如 VoIP、视频流） |

