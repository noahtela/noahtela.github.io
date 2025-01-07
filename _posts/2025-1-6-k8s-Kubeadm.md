---
layout:     post
title:      "k8s-Kubeadm基础"
subtitle:   " \"Kubeadm\""
date:       2025-1-6 14:22:12
author:     "yangsir"
header-img: "img/bg-material.jpg"
catalog: true
tags:
    - 笔记
    - Linux
    - Kubeadm


---

> “Yeah It's on. ”


<p id = "build"></p>

# Kubeadm



Kubeadm 是一个提供了 `kubeadm init` 和 `kubeadm join` 的工具， 作为创建 Kubernetes 集群的 “快捷途径” 的最佳实践，仅用于启动引导，而不用于配置机器



Kubeadm提供了一系列命令行工具

- [kubeadm init](https://kubernetes.io/zh-cn/docs/reference/setup-tools/kubeadm/kubeadm-init) 用于搭建控制平面节点
- [kubeadm join](https://kubernetes.io/zh-cn/docs/reference/setup-tools/kubeadm/kubeadm-join) 用于搭建工作节点并将其加入到集群中
- [kubeadm upgrade](https://kubernetes.io/zh-cn/docs/reference/setup-tools/kubeadm/kubeadm-upgrade) 用于升级 Kubernetes 集群到新版本
- [kubeadm config](https://kubernetes.io/zh-cn/docs/reference/setup-tools/kubeadm/kubeadm-config) 如果你使用了 v1.7.x 或更低版本的 kubeadm 版本初始化你的集群，则使用 `kubeadm upgrade` 来配置你的集群
- [kubeadm token](https://kubernetes.io/zh-cn/docs/reference/setup-tools/kubeadm/kubeadm-token) 用于管理 `kubeadm join` 使用的令牌
- [kubeadm reset](https://kubernetes.io/zh-cn/docs/reference/setup-tools/kubeadm/kubeadm-reset) 用于恢复通过 `kubeadm init` 或者 `kubeadm join` 命令对节点进行的任何变更
- [kubeadm certs](https://kubernetes.io/zh-cn/docs/reference/setup-tools/kubeadm/kubeadm-certs) 用于管理 Kubernetes 证书
- [kubeadm kubeconfig](https://kubernetes.io/zh-cn/docs/reference/setup-tools/kubeadm/kubeadm-kubeconfig) 用于管理 kubeconfig 文件
- [kubeadm version](https://kubernetes.io/zh-cn/docs/reference/setup-tools/kubeadm/kubeadm-version) 用于打印 kubeadm 的版本信息
- [kubeadm alpha](https://kubernetes.io/zh-cn/docs/reference/setup-tools/kubeadm/kubeadm-alpha) 用于预览一组可用于收集社区反馈的特性

