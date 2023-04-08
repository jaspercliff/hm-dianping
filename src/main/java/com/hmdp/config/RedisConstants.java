package com.hmdp.config;

/**
 * redis常量
 */
public class RedisConstants {
//    login
    public static final String LOGIN_CODE_KEY = "login:code:";
    public static final Long LOGIN_CODE_TTL = 2L;
    public static final String LOGIN_USER_KEY = "login:token:";
    public static final Long LOGIN_USER_TTL = 30L;

    //缓存穿透的空值的有效时间
    public static final Long CACHE_NULL_TTL = 2L;
    //shop
    public static final Long CACHE_SHOP_TTL = 30L;
    public static final String CACHE_SHOP_KEY = "cache:shop:";
    public static final String CACHE_SHOP_TYPE = "cache:shopType:";
    public static final Long CACHE_SHOP_TYPE_TTL = 30L;


    public static final String LOCK_SHOP_KEY = "lock:shop:";
    public static final Long LOCK_SHOP_TTL = 10L;

    public static final String SECKILL_STOCK_KEY = "seckill:stock:";

    public static final String BLOG_LIKED_KEY = "blog:liked:";

    public static final String Feed_KEY = "feed:";

    public static final String SHOP_GEO_KEY = "shop:geo:";
    public static final String SIGN_KEY = "sign:";


}
