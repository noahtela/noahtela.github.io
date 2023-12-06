---
layout:     post
title:      "SpringBoot监听机制"
subtitle:   " \"SpringBoot学习记录\""
date:       2023-12-06 09:00:31
author:     "yangsir"
header-img: "img/bg-material.jpg"
catalog: true
tags:
    - 笔记
    - SpringBoot

---

> “Yeah It's on. ”

## SpringBoot 监听机制



### Java 监听机制

SpringBoot的监听机制，其实就是对Java提供的事件监听机制的封装。

#### Java中的事件监听机制定义了以下几个角色：

1、事件：Event，继承java.util.EventObject类对象

2、事件源：Source，任意对象Object

3、监听器：Listener,实现java.util.EventListener接口的对象



### SpringBoot 监听机制

SpringBoot在项目启动时，会对几个监听器进行回调，我们可以实现这些监听器接口，在项目启动时完成一些操作。



```java
//ApplicationContextInitializer

@Component
public class MyApplicationContextlnitializer implements ApplicationContextInitializer{
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        System.out.println("ApplicationContextInitializer....initialize run");
    }
}
```



```java
//SpringApplicationRunListener

@Component
public class MySpringApplicationRunListener implements SpringApplicationRunListener {
    @Override
    public void starting(ConfigurableBootstrapContext bootstrapContext) {

        System.out.println("starting");
    }

    @Override
    public void environmentPrepared(ConfigurableBootstrapContext bootstrapContext, ConfigurableEnvironment environment) {
        System.out.println("environmentPrepared");
    }

    @Override
    public void contextPrepared(ConfigurableApplicationContext context) {
        System.out.println("contextPrepared");
    }

    @Override
    public void contextLoaded(ConfigurableApplicationContext context) {
        System.out.println("contextLoaded");
    }

    @Override
    public void started(ConfigurableApplicationContext context, Duration timeTaken) {
        System.out.println("started");
    }

    @Override
    public void started(ConfigurableApplicationContext context) {
        SpringApplicationRunListener.super.started(context);
    }

    @Override
    public void ready(ConfigurableApplicationContext context, Duration timeTaken) {
        System.out.println("ready");
    }

    @Override
    public void running(ConfigurableApplicationContext context) {
        SpringApplicationRunListener.super.running(context);
    }

    @Override
    public void failed(ConfigurableApplicationContext context, Throwable exception) {
        System.out.println("failed");
    }
}
```



```java
//CommandLineRunner

@Component
public class MyCommandLineRunner implements CommandLineRunner {
    @Override
    public void run(String... args) throws Exception {
        System.out.println("CommandLineRunner...run");
    }
}

```



```java
//ApplicationRunner

@Component
public class MyApplicationRunner implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println("ApplicationRunner...run");
    }
}
```

![image-20231203194709693](\img\springBoot\image-20231203194709693.png)

实验说明，只有最后两个被执行了

**原因**：SpringApplicationRunListener、ApplicationContextInitializer还需要进一步配置



在resources下新建META-INF/spring.factories

![image-20231206085243392](\img\springBoot\image-20231206085243392.png)

ApplicationContextInitializer成功运行

SpringApplicationRunListener报错 非法参数异常



MySpringApplicationRunListener加构造方法

```java
public MySpringApplicationRunListener(SpringApplication application, String[] args){

}
```



最终运行效果

![image-20231206090329765](\img\springBoot\image-20231206090329765.png)
