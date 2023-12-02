---
layout:     post
title:      "condition(二)"
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

在condition(一)中redis.clients.jedis.Jedis是写死的，不能灵活运用。。。



需求二：将类的判断定义为动态的。判断哪个字节码文件存在可以动态指定。

```java
//ConditionOnClass.java

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(classCondition.class)
public @interface ConditionOnClass {
    String[] value();
}

```

```java
//UserConfig

@Configuration
public class UserConfig {

    @Bean
    /*@Conditional(classCondition.class)*/
    @ConditionOnClass("redis.clients.jedis.Jedis")
    public User user(){
        return new User();
    }
}
```

新建了一个注解ConditionOnClass

```java
public class classCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        //导入jedis坐标后创建User
        //怎么才能知道，导入了jedis
        //思路 判断redis.clients.jedis.Jedis文件是否存在
        /*boolean flag = true;
        try {
            Class<?> cls = Class.forName("redis.clients.jedis.Jedis");
        } catch (ClassNotFoundException e) {
            flag = false;
        }
        return flag;*/
        //通过注解属性值导入value指定坐标后创建User
        //获取注解属性值
        Map<String, Object> map = metadata.getAnnotationAttributes(ConditionOnClass.class.getName());
        System.out.println(map);
        String[] value = (String[]) map.get("value");

        boolean flag = true;
        try {
            for (String className : value) {
                System.out.println(className);
                System.out.println(value);
                Class<?> cls = Class.forName(className);
            }
        } catch (ClassNotFoundException e) {
            flag = false;
        }
        return flag;
    }
}
```

现在，就可以在userConfig中自定义标签属性值了





## 小结

### 自定义条件：

​		1、定义条件类：自定义类实现Condition接口，重写matches方法，在matches中进行逻辑判断，返回boolean值。matches方法两个参数：

​					context: 上下文对象，可以获取属性值，获取类加载器，获取BeanFactory等。

​					metadate：元数据对象，用于获取注解属性。

​		2、判断条件：在初始化Bean时，使用@Conditional(条件类.class)注解

### SpeingBoot提供的常用条件注解：

​		ConditionalOnProperty: 判断配置文件中是否有对应属性和值才初始化Bean

​		ConditionalOnClass:判断环境中是否有对应字节码文件才初始化Bean

​		ConditionalOnMissingBean:判断环境中没有对应Bean才初始化Bean



完整代码在_doc/condition2中
