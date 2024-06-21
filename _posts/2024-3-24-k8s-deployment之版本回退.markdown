---
layout:     post
title:      "k8s-deployment版本回退"
subtitle:   " \"linux\""
date:       2024-3-24 19:25:49
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

# Deployment 版本回退



在 Kubernetes 中，Deployment 提供了方便的回滚机制，可以将 Deployment 回滚到之前的版本。这是通过 Kubernetes 的 Deployment 历史记录和版本控制来实现的。以下是如何使用 Deployment 回滚到之前版本的步骤：

### 检查 Deployment 历史记录

首先，你可以检查 Deployment 的历史记录，以查看可以回滚的版本。使用以下命令：

```sh
kubectl rollout history deployment <deployment-name>
```

例如：

```sh
kubectl rollout history deployment my-app
```

这将显示 Deployment 的所有修订版本：

```
deployments "my-app"
REVISION  CHANGE-CAUSE
1         <none>
2         <none>
3         <none>
```

### 回滚到指定版本

如果你想回滚到特定的修订版本，可以使用以下命令：

```sh
kubectl rollout undo deployment <deployment-name> --to-revision=<revision-number>
```

例如，回滚到修订版本 2：

```sh
kubectl rollout undo deployment my-app --to-revision=2
```

### 回滚到上一个版本

如果你只想回滚到上一个版本，可以简单地使用以下命令：

```sh
kubectl rollout undo deployment <deployment-name>
```

例如：

```sh
kubectl rollout undo deployment my-app
```

### 检查回滚状态

在回滚之后，可以使用以下命令检查 Deployment 的状态，以确保回滚成功：

```sh
kubectl rollout status deployment <deployment-name>
```

例如：

```sh
kubectl rollout status deployment my-app
```

### 配置变更原因

为了更好地跟踪变更，你可以在每次更新 Deployment 时，添加 `--record` 参数，这样每次变更的原因将会记录在历史记录中：

```sh
kubectl apply -f deployment.yaml --record
```

或者在编辑 Deployment 时，添加 `--record` 参数：

```sh
kubectl set image deployment/my-app my-app-container=my-app:2.0 --record
```

这样在执行 `kubectl rollout history deployment <deployment-name>` 命令时，你将看到每次变更的原因：

```
deployments "my-app"
REVISION  CHANGE-CAUSE
1         kubectl apply --filename=deployment.yaml --record=true
2         kubectl set image deployment/my-app my-app-container=my-app:2.0 --record=true
3         kubectl set image deployment/my-app my-app-container=my-app:1.0 --record=true
```

### 总结

回滚 Deployment 的过程非常简单，并且 Kubernetes 提供了强大的版本控制和历史记录功能，使得回滚到之前的版本变得非常方便和可靠。通过上述步骤，你可以轻松地将 Deployment 回滚到之前的稳定版本，以解决可能出现的问题。




