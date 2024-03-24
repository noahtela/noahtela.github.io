---
layout:     post
title:      "k8s-pod控制器之deployment"
subtitle:   " \"linux\""
date:       2024-3-24 15:25:49
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

# pod控制器之deployment



![image-20240324152027074](\img\springBoot\image-20240324152027074.png)



deployment的主要功能有下面几个：

- 支持replicaset的所有功能
- 支持发布的停止、继续
- 支持版本的滚动更新和版本回退



## Kubernetes Deployment 原理

Deployment 是 Kubernetes 中用于管理应用程序更新和回滚的一种控制器。它通过以下步骤工作：

**1. 创建 Deployment**

- 创建一个 Deployment 对象，其中指定要部署的镜像、副本数、标签等。

**2. 创建 ReplicaSet**

- Deployment 创建一个 ReplicaSet，负责管理 pod 副本。ReplicaSet 确保指定数量的 pod 始终处于运行状态。

**3. 创建 Pod**

- ReplicaSet 创建 pod，这些 pod 运行指定的镜像。

**4. 滚动更新**

- 当 Deployment 更新时，它会创建一个新的 ReplicaSet，并逐渐替换旧 ReplicaSet 中的 pod。
- 新的 ReplicaSet 会使用更新的镜像创建新 pod。
- 旧 ReplicaSet 中的 pod 会逐步终止。

**5. 回滚**

- 如果更新失败，Deployment 可以回滚到之前的版本。
- 它会创建旧 ReplicaSet 的新副本，并终止新 ReplicaSet 中的 pod。



### **Deployment 生命周期**

Deployment 的生命周期包括以下阶段：

- **创建：**Deployment 被创建并创建 ReplicaSet。
- **副本就绪：**ReplicaSet 中的 pod 已启动并运行。
- **稳定：**所有 pod 都处于运行状态，并且达到所需的副本数。
- **更新中：**Deployment 已更新，并且正在滚动更新。
- **更新完成：**新 ReplicaSet 中的所有 pod 都已运行，旧 ReplicaSet 中的所有 pod 都已终止。
- **终止：**Deployment 已终止，并且所有 pod 都已删除。

**优点**

- **滚动更新：**允许在不中断服务的情况下更新应用程序。
- **回滚：**如果更新失败，可以轻松地回滚到之前的版本。
- **编排：**自动化 pod 创建和管理，包括副本管理和滚动更新。
- **可伸缩性：**可以轻松地调整副本数以满足负载需求。

**最佳实践**

- 使用滚动更新策略实现渐进式更新。
- 使用标签和选择器来控制 ReplicaSet 的 pod 选择。
- 设置健康检查以监控 pod 健康状况并触发 pod 重启。
- 考虑使用蓝绿部署策略进行更安全的更新。



### 两种更新方式

**重建式更新**

- **原理：**删除所有现有 pod，然后创建具有新镜像的新 pod。
- 优点：
  - 快速且简单。
  - 确保所有 pod 都使用新镜像。
- 缺点：
  - 可能导致服务中断。
  - 不适合有状态应用程序。

**滚动更新**

- **原理：**逐步替换 pod，一次更新一个或多个 pod。
- 优点：
  - 零停机时间。
  - 即使出现问题，也可以轻松回滚。
  - 适用于有状态应用程序。
- 缺点：
  - 比重建式更新慢。
  - 可能需要更多资源来管理多个 pod 版本。

**选择哪种更新方式**

选择更新方式取决于应用程序的具体要求：

- **对于无状态应用程序，**重建式更新通常是可接受的，因为它速度快且简单。
- **对于有状态应用程序，**滚动更新是首选，因为它可以避免数据丢失或服务中断。

**其他考虑因素**

除了应用程序类型外，还有其他因素需要考虑：

- **应用程序健康检查：**确保在滚动更新期间 pod 健康检查正常工作。
- **流量管理：**使用负载均衡器或 Ingress 来管理更新期间的流量。
- **回滚策略：**制定明确的回滚计划，以防更新失败。

**最佳实践**

- 对于无状态应用程序，优先使用重建式更新。
- 对于有状态应用程序，始终使用滚动更新。
- 使用健康检查和流量管理来确保更新的顺利进行。
- 定期测试更新过程以验证其可靠性。



例：

简单的deployment使用

```
vim deploy-demo.yaml 
apiVersion: apps/v1
kind: Deployment
metadata:
  name: myapp-v1
spec:
  replicas: 2
  selector:
    matchLabels:
      app: myapp
      version: v1
  template:
    metadata:
      labels:
         app: myapp
         version: v1
    spec:
      containers:
      - name: myapp
        image: janakiramm/myapp:v1
        imagePullPolicy: IfNotPresent
        ports:
        - containerPort: 80
        startupProbe:
           periodSeconds: 5
           initialDelaySeconds: 20
           timeoutSeconds: 10
           httpGet:
             scheme: HTTP
             port: 80
             path: /
        livenessProbe:
           periodSeconds: 5
           initialDelaySeconds: 20
           timeoutSeconds: 10
           httpGet:
             scheme: HTTP
             port: 80
             path: /
        readinessProbe:
           periodSeconds: 5
           initialDelaySeconds: 20
           timeoutSeconds: 10
           httpGet:
             scheme: HTTP
             port: 80
             path: /
```



deployment实现滚动升级

```
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-deployment
spec:
  replicas: 3
  selector:
    matchLabels:
      app: my-app
  template:
    metadata:
      labels:
        app: my-app
    spec:
      containers:
      - name: my-container
        image: nginx
        imagePullPolicy: IfNotPresent
        livenessProbe:
          httpGet:
            path: /index.html
            port: 80
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /index.html
            port: 80
          initialDelaySeconds: 20
          periodSeconds: 10
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 0
      maxSurge: 1
```

**在上述示例中，Deployment 配置包括以下关键部分：** 

**replicas 定义了需要运行的 Pod 副本数量。** 

**selector 指定了用于选择 Pod 副本集的标签。** 

**template 定义了 Pod 的模板，包括容器和其他配置。** 

**strategy 指定了滚动更新的策略，类型为 RollingUpdate。** 

**rollingUpdate 定义了滚动更新的具体参数，例如 maxUnavailable 表示更新期间允许的最大不可用**

**Pod 数量，maxSurge 表示更新期间允许的最大额外副本数量。**
