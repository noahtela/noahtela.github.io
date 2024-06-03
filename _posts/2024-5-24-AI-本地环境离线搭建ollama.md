---
layout:     post
title:      "AI-本地环境离线搭建ollama"
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

> “ollama”


<p id = "build"></p>



# 本地环境离线搭建ollama运行大模型（cpu）



## 安装Ollama



Ollama模型管理工具是一款简化和优化NLP模型训练、部署、监控和维护的一站式解决方案，支持版本控制、实时监控、可视化分析和与主流框架集成。

![image-20240603082752048](\img\springBoot\image-20240603082752048.png)



### 1、下载程序包

```
https://ollama.com/download/ollama-linux-amd64
```



### 2、将ollama-linux-amd64上传至内网服务器

### 3、将ollama-linux-amd64移动到/usr/bin/并重命名为ollama

### 4、为ollama服务创建用户

```shell
sudo useradd -r -s /bin/false -m -d /usr/share/ollama ollama
```

### 5、新建服务文件

```shell
#vi /etc/systemd/system/ollama.service

[Unit]
Description=Ollama Service
After=network-online.target

[Service]
ExecStart=/usr/bin/ollama serve
User=ollama
Group=ollama
Restart=always
RestartSec=3

[Install]
WantedBy=default.target
```

### 6、启动服务

```shell
sudo systemctl daemon-reload
sudo systemctl enable ollama
sudo systemctl start ollama
```

![image-20240603084004318](\img\springBoot\image-20240603084004318.png)



## 下载GGUF模型

ollama官网提供了下载渠道

![image-20240603084241453](\img\springBoot\image-20240603084241453.png)





这里下载了qwen:0.5b模型

![image-20240603084948514](\img\springBoot\image-20240603084948514.png)



## 创建Ollama Modelfile

创建一个名为 Modelfile 的文件，并使用 FROM 指令，填写的模型的本地文件路径

![image-20240603085242759](\img\springBoot\image-20240603085242759.png)

```shell
FROM ./qwen1_5-0_5b-chat-q5_k_m.gguf #本地模型文件路径
#自此往下复制即可
TEMPLATE """{{ if .System }}<|im_start|>system    
{{ .System }}<|im_end|>{{ end }}<|im_start|>user
{{ .Prompt }}<|im_end|>
<|im_start|>assistant
"""

PARAMETER stop "<|im_start|>"
PARAMETER stop "<|im_end|>"
```



在Ollama中创建模型

```shell
ollama create qwen:0.5b -f Modelfile
```

![image-20240603085910770](\img\springBoot\image-20240603085910770.png)



```
ollama list #查看已创建的模型
```

![image-20240603090035211](\img\springBoot\image-20240603090035211.png)

运行模型

```shell
ollama run [模型名称]
```

![image-20240603091944612](\img\springBoot\image-20240603091944612.png)



删除模型

```
ollama rm [模型名称]
```





16H16G物理服务器实测

![image-20240603092403829](\img\springBoot\image-20240603092403829.png)
