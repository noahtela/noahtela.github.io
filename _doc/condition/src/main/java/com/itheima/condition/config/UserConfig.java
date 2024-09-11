package com.itheima.condition.config;


import com.itheima.condition.condition.classCondition;
import com.itheima.condition.domain.User;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserConfig {

    @Bean
    @Conditional(classCondition.class)
    public User user(){
        return new User();
    }
}
