package com.itheima.condition;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class ConditionApplication {

    public static void main(String[] args) {

        //启动springboot应用,返回spring的ico容器
        ConfigurableApplicationContext context = SpringApplication.run(ConditionApplication.class, args);

//        //获取Bean,redisTemplate
//        Object redisTemplate = context.getBean("redisTemplate");
//        System.out.println(redisTemplate);
        Object user = context.getBean("user");
        System.out.println(user);


    }

}
