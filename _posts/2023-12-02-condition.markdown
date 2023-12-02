---
layout:     post
title:      "condition(一)"
subtitle:   " \"SpringBoot学习记录\""
date:       2023-12-02 12:50:12
author:     "yangsir"
header-img: "img/bg-material.jpg"
catalog: true
tags:
    - 笔记
    - SpringBoot

---

> “Yeah It's on. ”


<p id = "build"></p>

## 开头测试

快速创建springBoot项目，获取bean，redisTemplate

![image-20231201203504440](\img\springBoot\image-20231201203504440.png)

报错是必然的，因为根本就没导入redis坐标

![image-20231201204204905](\img\springBoot\image-20231201204204905.png)

导入坐标后再运行

![image-20231201204235027](\img\springBoot\image-20231201204235027.png)

### 实验案例

在Spring的IOC容器中有一个User的bean，现要求：

​		1、导入·jedis坐标后，加载该Bean，没导入，则不加载。

![image-20231202105442440](\img\springBoot\image-20231202105442440.png)

先加载Bean

​		2、实现判断

```java
//classCondition
public class classCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return false;
    }
}
```

```java

/*userConfig*/
@Configuration
public class UserConfig {

    @Bean
    @Conditional(classCondition.class)
    public User user(){
        return new User();
    }
}
```

因为classCondition中返回的是false,所以再运行时，user不会再被创建



```java
/*classCondition*/
public class classCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        //导入jedis坐标后创建User
        //怎么才能知道，导入了jedis
        //思路 判断redis.clients.jedis.Jedis文件是否存在
        boolean flag = true;
        try {
            Class<?> cls = Class.forName("redis.clients.jedis.Jedis");
        } catch (ClassNotFoundException e) {
            flag = false;
        }
        return flag;
    }
}
```

如此，便能实现

代码在_doc中的condition中
