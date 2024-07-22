---
  layout:     post
title:      "linux软件-自动化运维工具Ansible"
subtitle:   " \"linux\""
date:       2024-7-22 16:41:12
author:     "yangsir"
header-img: "img/bg-material.jpg"
catalog: true
tags:
    - 笔记
    - linux学习
    - 云原生


---

> “Yeah It's on. ”


<p id = "build"></p>

# linux软件-自动化运维工具Ansible



## 特性

- 模块化:调用特定的模块，完成特定任务
- 有Paramiko，PyYAML，Jinja2(模板语言)三个关键模块
- 支持自定义模块
- 基于Python语言实现
- 部署简单，基于python和SSH(默认已安装)，agentless
- 安全，基于OpensSH
- 支持playbook编排任务
- 幂等性:一个任务执行1遍和执行n遍效果一样，不因重复执行带来意外情况
- 无需代理不依赖PKI(无需ssI)
- 可使用任何编程语言写模块
- YAML格式，编排任务，支持丰富的数据结构
- 较强大的多层解决方案

![image-20240722161553409](\img\springBoot\image-20240722161553409.png)



## 主要组成部分

- Ansible-playbook(剧本)执行过程
  - 将已有编排好的任务集写入Ansible-Playbook
  - 通过ansible-playbook命令分拆任务集至逐条ansible命令，按预定规则逐条执行
- Ansible主要操作对象
  - HOSTS主机
  - NETWORKING网络设备
- 注意事项
  - 执行ansible的主机一般称为主控端，中控，master或堡垒机
  - 主控端Python版本需要2.6或以上
  - 被控端Python版本小于2.4需要安装python-simplejson
  - 被控端如开启SELinux需要安装libselinux-python
  - windows不能做为主控端

## 配置文件

- /etc/ansible/ansible.cfg主配置文件，配置ansible工作特性
- /etc/ansible/hosts 主机清单
- /etc/ansible/roles/存放角色的目录

## 程序

- /usr/bin/ansible 主程序，临时命令执行工具
- /usr/bin/ansible-doc 查看配置文档，模块功能查看工具
- /usr/bin/ansible-galaxy下载/上传优秀代码或Roles模块的官网平台
- /usr/bin/ansible-playbook定制自动化任务,编排剧本工具
- /usr/bin/ansible-pull 远程执行命令的工具
- /usr/bin/ansible-vault 文件加密工具
- /usr/bin/ansible-console 基于console界面与用户交互的执行工具

## Ansible使用和模块化

### Ansible主配置文件

![image-20240722170158421](\img\springBoot\image-20240722170158421.png)

```shell
[defaults]
#inventory = /etc/ansible/hosts #主机列表配置文件
#library = /usr/share/my_modules/ #库文件存放目录
#remote_tmp =$HOME/.ansible/tmp #临时py命令文件存放在远程主机目录
#local_tmp=$HOME/.ansible/tmp #本机的临时命令执行目录
#forks=5 #默认并发数
#sudo_user= root # 默认sudo 用户
#ask_sudo_pass = True #每次执行ansible命令是否询问ssh密码
#ask_pass =True
#remote_port =22
#host_key_checking = False # 检査对应服务器的host_key，建议取消注释
#log_path=/var/log/ansible.log #日志文件
```



## ansible的Host-pattern

逻辑与

```shell
ansible “websrvs:&dbsrvs" -m ping
```

在websrvs组并且在dbsrvs组中的主机

逻辑非

```shell
ansible 'websrvs:!dbsrvs' -m ping
```

在websrvs组，但不在dbsrvs组中的主机

注意:此处为单引号
综合逻辑

```shell
ansible 'websrvs:dbsrvs:&appsrvs:!ftpsrvs' -m ping
```

正则表达式

```shell
ansible "websrvs:&dbsrvs" -m ping
ansible "~(webdb).*.magedul.com" -m ping
```

