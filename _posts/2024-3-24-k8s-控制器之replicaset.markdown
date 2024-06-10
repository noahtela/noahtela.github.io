---
layout:     post
title:      "k8s-Pod控制器之replicaset"
subtitle:   " \"linux\""
date:       2024-3-24 13:25:49
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

# Pod控制器Replicaset

##   replicaset工作原理

1. **创建ReplicaSet**:
   - 用户通过YAML文件或其他方式创建一个ReplicaSet对象,指定所需的副本数量(replicas)和Pod模板(template)。
2. **监控运行中的Pod**:
   - ReplicaSet会持续监控集群中运行的Pod,根据标签选择器(selector)来识别属于自己管理范围的Pod。
3. **确保期望副本数**:
   - ReplicaSet会定期检查当前运行中的Pod数量是否等于期望的副本数。
   - 如果Pod数量少于期望值,ReplicaSet会根据Pod模板创建新的Pod,以达到预期的副本数。
   - 如果Pod数量多于期望值,ReplicaSet会终止多余的Pod,使副本数保持在期望值。
4. **自动修复**:
   - 如果某个Pod出现故障或被删除,ReplicaSet会自动创建新的Pod来替换它,从而保证总的副本数量不变。

## Replicaset组成部分



1. 用户期望的 pod 副本数：用来定义由这个控制器管控的 pod 副本有几个 
2. 标签选择器：选定哪些 pod 是自己管理的，如果通过标签选择器选到的 pod 副本数量少于我们指定的数量，需要用到下面的组件 
3. pod 资源模板：如果集群中现存的 pod 数量不够我们定义的副本中期望的数量怎么办，需要新建 pod，这就需要 pod 模板，新建的 pod 是基于模板来创建的。 



例：

```
[root@master ~]# vim replicaset.yaml 
apiVersion: apps/v1
kind: ReplicaSet
metadata:
  name: frontend
  namespace: default
  labels:
    app: guestbook
    tier: frontend
spec:
  replicas: 3
  selector:
    matchLabels:
      tier1: frontend1
  template:
    metadata:
      labels:
        tier1: frontend1
    spec:
      containers:
      - name: php-redis
        image: docker.io/yecc/gcr.io-google_samples-gb-frontend:v3
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

​		

导入`frontend.tar.gz` 

```
docker load -i frontend.tar.gz 
```

运行

```
kubectl apply -f replicaset.yaml
```

![image-20240324145400717](\img\springBoot\image-20240324145400717.png)

![image-20240324145455320](\img\springBoot\image-20240324145455320.png)



## Replicaset 管理 pod：扩容、缩容、更新



修改配置文件 replicaset.yaml 里的 replicas 的值由原来 replicas: 3修改为replicas: 4

![image-20240324145754758](\img\springBoot\image-20240324145754758.png)

重新执行

```
kubectl apply -f replicaset.yaml
```

![image-20240324145913738](\img\springBoot\image-20240324145913738.png)

![image-20240324145929907](\img\springBoot\image-20240324145929907.png)



扩容成功

