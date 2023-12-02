package com.itheima.condition.condition;


import org.springframework.context.annotation.Conditional;

import java.lang.annotation.*;


@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(classCondition.class)
public @interface ConditionOnClass {
    String[] value();
}
