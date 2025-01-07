---
layout:     post
title:      "k8s-Prometheus基础"
subtitle:   " \"Prometheus\""
date:       2025-1-6 14:22:12
author:     "yangsir"
header-img: "img/bg-material.jpg"
catalog: true
tags:
    - 笔记
    - Linux
    - Prometheus


---

> “Yeah It's on. ”


<p id = "build"></p>

# Prometheus基础



## 一、什么是Prometheus?

Prometheus 是一个开源的系统监控和报警系统，现在已经加入到 CNCF 基金会，成为继 k8s 之后第二个在 CNCF 托管的项目，在 kubernetes 容器管理系统中，通常会搭配 prometheus 进行监控，同时也支持多种 exporter 采集数据，还支持 pushgateway 进行数据上报，Prometheus 性能足够支撑上万台规模的集群。



## 二、Prometheus 特点？



### 1.多维度数据模型 

每一个时间序列数据都由 metric 度量指标名称和它的标签 labels 键值对集合唯一确定：这个 metric 度量指标名称指定监控目标系统的测量特征（如：http_requests_total- 接收 http 请求的总计数）。labels 开启了 Prometheus 的多维数据模型：对于相同的度量名称，通过不同标签列表的结合, 会形成特定的度量维度实例。(例如：所有包含度量名称为/api/tracks 的 http 请求，打上 method=POST 的标签，则形成了具体的 http 请求)。这个查询语言在这些度量和标签列表的基础上进行过滤和聚合。改变任何度量上的任何标签值，则会形成新的时间序列图。 

### 2.灵活的查询语言（PromQL） 

可以对采集的 metrics 指标进行加法，乘法，连接等操作； 

### 3.可以直接在本地部署，不依赖其他分布式存储； 

### 4.通过基于 HTTP 的 pull 方式采集时序数据； 

### 5.可以通过中间网关 pushgateway 的方式把时间序列数据推送到 prometheus server 端； 

### 6.可通过服务发现或者静态配置来发现目标服务对象（targets）。 

### 7.有多种可视化图像界面，如 Grafana 等。 

### 8.高效的存储，每个采样数据占 3.5 bytes 左右，300 万的时间序列，30s 间隔，保留 60 天，消耗磁盘大概 200G。 

### 9.做高可用，可以对数据做异地备份，联邦集群，部署多套 prometheus，pushgateway 上报数据



## 三、Prometheus 组件介绍 

- Prometheus Server: 用于收集和存储时间序列数据。 
- Client Library: 客户端库，检测应用程序代码，当 Prometheus 抓取实例的 HTTP 端点时，客户端库会将所有跟踪的 metrics 指标的当前状态发送到 prometheus server 端。
- Exporters: prometheus 支持多种 exporter，通过 exporter 可以采集 metrics 数据，然后发送到 prometheus server 端，所有向 promtheus server 提供监控数据的程序都可以被称为 exporter 
- Alertmanager: 从 Prometheus server 端接收到 alerts 后，会进行去重，分组，并路由到相应的接收方，发出报警，常见的接收方式有：电子邮件，微信，钉钉, slack 等。 
- Grafana：监控仪表盘，可视化监控数据 
- pushgateway: 各个目标主机可上报数据到 pushgateway，然后 prometheus server 统一从 pushgateway 拉取数据。

![image-20250106143023619](\img\linux\image-20250106143023619.png)



从上图可发现，Prometheus 整个生态圈组成主要包括 prometheus server，Exporter， pushgateway，alertmanager，grafana，Web ui 界面，Prometheus server 由三个部分组成， Retrieval，Storage，PromQL 

1.Retrieval 负责在活跃的 target 主机上抓取监控指标数据 

2.Storage 存储主要是把采集到的数据存储到磁盘中 

3.PromQL 是 Prometheus 提供的查询语言模块。 



## 四、Prometheus 工作流程 

1.Prometheus server 可定期从活跃的（up）目标主机上（target）拉取监控指标数据，目标主机的监控数据可通过配置静态 job 或者服务发现的方式被 prometheus server 采集到，这种方式默认的pull方式拉取指标；也可通过 pushgateway 把采集的数据上报到 prometheus server 中；还可通过一些组件 自带的 exporter 采集相应组件的数据； 

2.Prometheus server 把采集到的监控指标数据保存到本地磁盘或者数据库； 

3.Prometheus 采集的监控指标数据按时间序列存储，通过配置报警规则，把触发的报警发送到alertmanager 

4.Alertmanager 通过配置报警接收方，发送报警到邮件，微信或者钉钉等 

5.Prometheus 自带的 web ui 界面提供 PromQL 查询语言，可查询监控数据 

6.Grafana 可接入 prometheus 数据源，把监控数据以图形化形式展示出 
