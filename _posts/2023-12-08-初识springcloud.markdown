---
layout:     post
title:      "初识SpringCloud"
subtitle:   " \"SpringCloud学习记录\""
date:       2023-12-08 16:03:12
author:     "yangsir"
header-img: "img/bg-material.jpg"
catalog: true
tags:
    - 笔记
    - SpringCloud

---

> “Yeah It's on. ”


<p id = "build"></p>

SpringCloud将现在非常流行的一些技术整合在一起，实现了配置管理，服务发现，智能路由，负载均衡，熔断器，控制总线，集群状态等功能；协调分布式环境中各个系统，为各类服务提供模板性配置。其主要涉及的组件包括：

- Eureka：注册中心

- Zuul、Gateway：服务网关

- Ribbon：负载均衡

- Feign：服务调用

- Hystrix或Resilience4j：熔断器

  ![image-20231208163853791](\img\springBoot\image-20231208163853791.png)

版本特征：以英文单词命名



