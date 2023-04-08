package com.hmdp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ClusterController {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @GetMapping("/get/{key}")
    public String hi(@PathVariable String key){
            return stringRedisTemplate.opsForValue().get(key);
    }

}
