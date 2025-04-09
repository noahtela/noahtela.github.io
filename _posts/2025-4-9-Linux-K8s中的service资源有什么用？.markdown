---
layout:     post
title:      "K8s-Service服务有什么作用？"
subtitle:   " \"k8s\""
date:       2025-4-9 11:28:12
author:     "yangsir"
header-img: "img/bg-material.jpg"
catalog: true
tags:
    - 笔记
    - k8s


---

> “Service在k8s中扮演什么角色”


<p id = "build"></p>

# Service服务有什么作用？



在 Kubernetes（K8s）中，`Service` 资源的作用是为一组 `Pod` 提供**稳定的访问入口**，实现服务发现和负载均衡。



## 为什么需要 Service？

在 Kubernetes 中，`Pod` 是短暂的，可能会因为各种原因重建，而每次重建其 IP 地址都会变化。因此，不能直接通过 Pod 的 IP 来访问服务。`Service` 就是为了解决这个问题。



## Service 的主要作用

**为 Pod 提供一个稳定的网络访问入口**

- 即使 Pod 重建了，Service 的 IP 或名称不会变，客户端可以始终通过 Service 来访问。

**负载均衡**

- 当多个 Pod 提供相同服务时，Service 会自动将请求分发到后端的多个 Pod 上，实现负载均衡。

**服务发现**

- 集群内部的 Pod 可以通过 DNS 名称（如 `my-service.default.svc.cluster.local`）来发现并访问其他 Service。

(注意:ClusterIP中命名规则:`<service-name>.<namespace>.svc.cluster.local`)

| 部分          | 含义                                |
| ------------- | ----------------------------------- |
| my-service    | 你的 Service 名称                   |
| default       | Service 所在的命名空间（namespace） |
| svc           | 表示这是一个 Service 类型资源       |
| cluster.local | 集群的域名后缀（可配置）            |

所以这个名字是系统自动生成的，**不用手动配置**，只要创建了 Service，就能用这个名字在集群内部访问。

**跨网络访问（部分类型）**

- 某些类型的 Service（如 `NodePort`、`LoadBalancer`）允许集群外部的客户端访问集群内部的服务。

## 常见的 Service 类型

| 类型         | 描述                                                         |
| ------------ | ------------------------------------------------------------ |
| ClusterIP    | 默认类型，只能在集群内部访问。                               |
| NodePort     | 暴露一个端口，在集群每个节点的 IP 上都可以通过该端口访问服务。 |
| LoadBalancer | 使用云服务商的负载均衡器暴露服务，适用于公有云环境。         |
| ExternalName | 把服务映射到一个外部 DNS 名称，不真正创建代理，只是 DNS 映射。 |







# CoreDNS又是做什么的？



## CoreDNS是什么？

`CoreDNS` 是 Kubernetes 集群中默认的 **DNS 服务组件**，它的作用是为集群内的 Pod 提供 **服务发现（Service Discovery）和 DNS 解析** 功能。



## 它主要做什么？

在 Kubernetes 中，Pod 之间、Pod 与 Service 之间需要互相通信，但 IP 经常变化，于是就需要使用名称来访问。`CoreDNS` 就是负责把这些名称解析成正确的 IP 地址。

例如：

```shell
ping my-service.default.svc.cluster.local
```

这条命令是向 Service `my-service` 发起访问请求，`CoreDNS` 会负责把这个域名解析为对应的 ClusterIP（或者 Pod IP）



## 工作原理简单图解：

1. Pod 内部发起一个 DNS 请求，例如访问 `my-service.default.svc.cluster.local`。
2. 请求被发送到 `CoreDNS`。
3. `CoreDNS` 查询 Kubernetes API，找到匹配的 Service 和 Pod IP。
4. 返回 IP 地址，Pod 开始通信。



# 问题



##  为什么不能直接给 Pod 设置 NodePort？

- Kubernetes 的 `Pod` 是运行在内部网络中的，没有暴露到 Node 上。
- `NodePort` 是 Kubernetes 中 `Service` 的一种，它的作用是：在每个节点的某个固定端口（比如 30000-32767）上暴露你的服务，从而可以通过 `NodeIP:NodePort` 来访问。
- 这个机制是由 Kubernetes 的 `kube-proxy` 管理的，而 `Pod` 自身并不具备这种能力。

正确做法：创建一个 `Service`，类型设置为 `NodePort`，然后通过这个服务来暴露 Pod

```yaml
apiVersion: v1
kind: Service
metadata:
  name: my-nodeport-service
spec:
  type: NodePort
  selector:
    app: my-app     # 匹配对应的 Pod
  ports:
    - port: 80         # 服务端口
      targetPort: 8080 # Pod 内部端口
      nodePort: 30080  # 暴露在 Node 上的端口（可选，也可由系统自动分配）
```



## 使用 URL 暴露服务时，怎么区分 ClusterIP 和 NodePort？



1、ClusterIP类型

- **默认类型**

- 只能在集群**内部访问**

- URL 格式是：

  ```
  http://<service-name>.<namespace>.svc.cluster.local:<port>
  ```

- 适合 Pod 与 Pod、Job、CronJob 等内部通信

✅ 访问方式示例（在集群内部）：

```shell
curl http://my-service.default.svc.cluster.local:8080
```

2、NodePort类型

- 会在集群每个 Node 上开放一个端口（默认范围是 30000~32767）

- 可以**从集群外部通过 IP:Port 访问**

- URL 格式是：

  ```
  http://<node-ip>:<nodePort>
  ```

✅ 访问方式示例（从集群外部或本机）：

```
curl http://192.168.1.100:30080
```





## ClusterIP和NodePort的实例



### 1、使用 `ClusterIP`（只能在集群内部访问）

```yaml
# clusterip-nginx.yaml
apiVersion: v1
kind: Pod
metadata:
  name: nginx-pod
  labels:
    app: nginx
spec:
  containers:
    - name: nginx
      image: nginx
      ports:
        - containerPort: 80

---

apiVersion: v1
kind: Service
metadata:
  name: nginx-clusterip
spec:
  type: ClusterIP
  selector:
    app: nginx
  ports:
    - port: 8080        # 集群内部访问的端口
      targetPort: 80    # Pod 内部容器端口
```

#### ✅ 部署命令：

```
kubectl apply -f clusterip-nginx.yaml
```

#### 🧪 访问方式（只能在集群内）：

```
curl http://nginx-clusterip.default.svc.cluster.local:8080
```





### 2、使用 `NodePort`（可以从外部访问）



```yaml
# nodeport-nginx.yaml
apiVersion: v1
kind: Pod
metadata:
  name: nginx-pod-nodeport
  labels:
    app: nginx-nodeport
spec:
  containers:
    - name: nginx
      image: nginx
      ports:
        - containerPort: 80

---

apiVersion: v1
kind: Service
metadata:
  name: nginx-nodeport
spec:
  type: NodePort
  selector:
    app: nginx-nodeport
  ports:
    - port: 8080           # 服务内部端口
      targetPort: 80       # Pod 容器端口
      nodePort: 30080      # Node 上暴露的端口（可省略，让系统自动分配）
```

#### ✅ 部署命令：

```
kubectl apply -f nodeport-nginx.yaml
```

#### 🌐 访问方式（集群外部）：

找到任意 Node 的 IP，比如：

```
kubectl get nodes -o wide
```

然后访问：

```
http://<NodeIP>:30080
```

例如：

```
http://192.168.56.101:30080
```



## 📝 注意

- 你可以同时部署这两个版本，它们互不影响。
- `Pod` 和 `Service` 的 `label` 和 `selector` 一定要匹配。





