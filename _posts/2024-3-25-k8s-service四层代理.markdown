---
layout:     post
title:      "k8s-service四层代理"
subtitle:   " \"linux\""
date:       2024-3-25 09:25:49
author:     "yangsir"
header-img: "img/bg-material.jpg"
catalog: true
tags:
    - 笔记
    - k8s
    - 云原生



---

> “Yeah It's on. ”


<p id = "build"></p>

# service四层代理



**为什么要有service?**

Kubernetes 中的 Service 就好像是一个**交通警察**，它负责管理 Pod（应用程序容器）之间的网络流量。

- **抽象网络复杂性：**Service 隐藏了 Pod 的具体网络地址，让客户端可以轻松访问 Pod，即使 Pod 的地址发生了变化。
- **负载均衡：**Service 将流量均匀地分发到多个 Pod 上，确保应用程序的高可用性和可扩展性。
- **服务发现：**Service 帮助客户端找到 Pod，即使它们位于不同的网络或命名空间中。

有了 Service，应用程序可以轻松地相互通信，即使它们是由不同的团队管理的，或者位于不同的物理位置。它简化了网络管理，提高了应用程序的可靠性和可扩展性。



**service工作原理**



当创建 Kubernetes Service 时，它会根据标签选择器查找与该 Service 匹配的 Pod。然后，它会根据这些 Pod 创建一个名为 `<service-name>-endpoints` 的 Endpoint 对象。Endpoint 对象包含了后端 Pod 的 IP 地址和端口信息。

当 Pod 的地址发生变化时，Endpoint 对象也会自动更新。这是通过 Kubernetes 的控制器机制实现的。当 Pod 被创建、删除或其 IP 地址发生变化时，Endpoint 控制器会检测到这些更改并相应地更新 Endpoint 对象。

Service 接收前端客户端请求时，它会使用 Endpoint 对象来确定将请求转发到哪个后端 Pod。转发到哪个节点的 Pod 由负载均衡器 kube-proxy 决定。kube-proxy 使用各种算法（例如轮询或最少连接）在后端 Pod 之间分发请求，以实现负载均衡。

通过使用 Endpoint 对象，Service 可以动态管理其后端 Pod，并确保客户端请求始终被路由到正确的 Pod。这简化了服务发现和应用程序通信，并提供了故障恢复和可扩展性。



k8s中三类ip地址

1. **Pod IP（pod Network）：**分配给每个 Pod 的唯一 IP 地址，用于 Pod 之间的通信。Pod IP 通常是集群内部的私有 IP 地址。
2. **Service IP（Cluster Network）：**分配给 Service 的**虚拟 IP** 地址，用于访问 Service 后面的 Pod。Service IP 由集群内的所有节点共享。

![image-20240327100456345](\img\springBoot\image-20240327100456345.png)

1. **Node IP（Node Network）：**分配给每个节点的 IP 地址，用于与外部世界（例如客户端和 Internet）通信。Node IP 通常是公有 IP 地址或私有 IP 地址，具体取决于集群的网络配置。

**示例：**

- Pod IP：10.244.0.10
- Service IP：10.100.0.100
- Node IP：192.168.1.10

**用途：**

- **Pod IP：**用于 Pod 之间的直接通信。
- **Service IP：**用于访问 Service 后面的 Pod，简化服务发现和负载均衡。
- **Node IP：**用于与外部世界通信，例如客户端访问和集群管理。



### 创建一个service

ymal中两个重要配置

```
sessionAffinity <string>

sessionAffinityConfig <Object>
```

 

**service** **在实现负载均衡的时候还支持 sessionAffinity**，会话联系，默认是 none，随机调度的（基于 iptables规则调度的）；如果我们定义sessionAffinity的 client ip，那就表示把来自同一客户端的 IP 请求调度到同一个 pod上



### service的四种类型

1. **ClusterIP：**
   - 创建一个内部 IP 地址，集群中的其他 Pod 可以使用该 IP 地址访问该服务。
   - 外部流量无法直接访问该服务。
   - 这是默认的服务类型。
2. **NodePort：**
   - 在每个工作节点上公开一个端口，集群外的流量可以使用该端口访问该服务。
   - 该端口号是随机分配的，并且可以在服务规范中配置。
   - 适用于需要从外部访问的服务。
3. **LoadBalancer：**
   - 在云提供商处创建一个负载均衡器，该负载均衡器将外部流量分配到服务后端的 Pod。
   - 云提供商负责管理负载均衡器。
   - 适用于需要高可用性和高性能的服务。
4. **ExternalName：**
   - 将服务映射到外部 DNS 名称，而不是创建自己的 IP 地址或端口。
   - 适用于与外部服务（例如数据库或 API 网关）交互的服务。



### 常用类型使用场景



**ClusterIP 服务**

**使用场景：**

* 集群内部 Pod 之间的通信
* 不需要外部访问的服务
* 例如：数据库、缓存、消息队列

**优点：**

* 简单易用，无需额外配置
* 仅限于集群内部访问，提高安全性

**NodePort 服务**：

- **从集群外部访问服务，但不需要高可用性或负载均衡。** NodePort 服务在每个工作节点上公开一个端口，因此外部流量可以绕过负载均衡器直接访问服务。这对于不需要高可用性或负载均衡的简单服务非常有用，例如：
  - 内部工具或仪表板
  - 开发或测试环境中的服务
  - 不需要外部用户直接访问的后台服务
- **作为负载均衡器的后端。** NodePort 服务可以作为负载均衡器的后端，负载均衡器将外部流量分配到服务后端的 Pod。这比使用 LoadBalancer 服务更简单，因为无需云提供商管理负载均衡器。
- **在没有外部 IP 地址的集群中提供外部访问。** 在某些情况下，Kubernetes 集群可能没有外部 IP 地址，例如在内部部署或受限的云环境中。 NodePort 服务允许通过工作节点的 IP 地址和 NodePort 访问服务。

**优点：**

- 易于设置和使用
- 无需云提供商管理负载均衡器
- 在没有外部 IP 地址的集群中提供外部访问



**LoadBalancer 服务**

**使用场景：**

* 需要从外部访问的服务
* 高可用性和高性能要求
* 例如：Web 服务器、API 网关、负载均衡器

**优点：**

* 提供外部访问，无需手动配置端口转发
* 云提供商负责管理负载均衡器，提高可靠性
* 可扩展性好，可以处理大量流量

**ExternalName 服务**

**使用场景：**

* 与外部服务（例如数据库、API 网关）交互
* 需要使用外部服务的 DNS 名称
* 例如：连接到外部数据库或第三方 API

**优点：**

* 方便与外部服务集成
* 无需创建自己的 IP 地址或端口
* 提高可移植性，因为服务名称不受集群环境的影响

**示例：**

**ClusterIP 服务：**

```yaml
apiVersion: v1
kind: Service
metadata:
  name: my-database
spec:
  selector:
    app: database
  ports:
  - port: 3306
    targetPort: 3306
```

**NodePort 服务**：

```yaml
apiVersion: v1
kind: Service
metadata:
  name: my-nodeport-service
spec:
  type: NodePort
  selector:
    app: my-app
  ports:
  - port: 80
    targetPort: 80
    nodePort: 30000
```



**LoadBalancer 服务：**

```yaml
apiVersion: v1
kind: Service
metadata:
  name: my-web-server
spec:
  type: LoadBalancer
  selector:
    app: web-server
  ports:
  - port: 80
    targetPort: 80
```

**ExternalName 服务：**

```yaml
apiVersion: v1
kind: Service
metadata:
  name: my-external-service
spec:
  type: ExternalName
  externalName: my-external-service.example.com
```
