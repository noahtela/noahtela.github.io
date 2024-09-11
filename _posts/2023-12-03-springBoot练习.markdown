---
layout:     post
title:      "SpringBoot自动配置-练习"
subtitle:   " \"SpringBoot学习记录\""
date:       2023-12-03 15:35:12
author:     "yangsir"
header-img: "img/bg-material.jpg"
catalog: true
tags:
    - 笔记
    - SpringBoot

---

> “Yeah It's on. ”


<p id = "build"></p>

## 案例需求

​		自定义redis-stater.要求当导入redis坐标时，SpringBoot自动创建Jedis的Bean.

## 案例实现步骤

​		1、创建redis-spring-boot-autoconfigure模块

​		2、创建redis-spring-boot-starter模块,依赖redis-spring-boot-autoconfigure的模块

​		3、在redis-spring-boot-autoconfigure模块中初始化Jedis的Bean。并定义在META-INF/spring.factories文件

​		4、在测试模块中引入自定义的redis-starter依赖，测试获取Jedis的Bean，操作redis
