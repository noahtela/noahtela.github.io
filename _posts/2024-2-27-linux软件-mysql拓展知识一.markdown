---
layout:     post
title:      "linux软件-mysql拓展知识一"
subtitle:   " \"linux\""
date:       2024-2-27 17:22:49
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

# MySQL拓展知识一



## 破解root密码

![image-20240228155719633](\img\springBoot\image-20240228155719633.png)







1、关闭数据库服务

![image-20240228155819228](\img\springBoot\image-20240228155819228.png)



2、绕开root密码登录

 

```
/usr/local/mysql/bin/mysqld --console --skip-grant-tables --user=mysql
```

![image-20240228160055588](\img\springBoot\image-20240228160055588.png)







3、新开一个终端登录MySQL

![image-20240228160204135](\img\springBoot\image-20240228160204135.png)



无密码直接登录



4、清空密码



```
FLUSH PRIVILEGES ;
ALTER USER 'root'@'%' IDENTIFIED BY '';	
```

