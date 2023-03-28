package com.hmdp.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {
    @Bean
    public RedissonClient redissonClient(){
        Config config = new Config();
        config.useSingleServer().setAddress("redis://192.168.236.134:6379").setPassword("root");
        return Redisson.create(config);
    }

//    @Bean
//    public RedissonClient redissonClient1(){
//        Config config = new Config();
//        config.useSingleServer().setAddress("redis://192.168.236.134:6380");
//        return Redisson.create(config);
//    }
//
//    @Bean
//    public RedissonClient redissonClient2(){
//        Config config = new Config();
//        config.useSingleServer().setAddress("redis://192.168.236.134:6381");
//        return Redisson.create(config);
//    }
}
