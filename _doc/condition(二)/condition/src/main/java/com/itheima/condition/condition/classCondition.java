package com.itheima.condition.condition;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;

/**
 * context 上下文对象。用于获取环境、IOC容器、classLoader对象
 * metadata 注解元对象。可以用于注解定义的属性值
 *
 */
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
