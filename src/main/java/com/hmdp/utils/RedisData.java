package com.hmdp.utils;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用来封装逻辑过期时间的 在不改变shop entity源代码的基础上   有一点像dto
 */
@Data
public class RedisData {
    private LocalDateTime expireTime;
    private Object data;
}
