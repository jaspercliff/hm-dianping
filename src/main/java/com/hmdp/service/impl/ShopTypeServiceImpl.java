package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.hmdp.config.RedisConstants.CACHE_SHOP_TYPE;
import static com.hmdp.config.RedisConstants.CACHE_SHOP_TYPE_TTL;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author jasper
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryShopType() {
        String key = CACHE_SHOP_TYPE;
//        从redis查询商店类型
        List<String> typeList = stringRedisTemplate.opsForList().range(key, 0, -1);
        if (typeList.size()!=0) {
            ArrayList<ShopType> shopTypes = new ArrayList<>();
            for (String s : typeList) {
                ShopType shopType = JSONUtil.toBean(s, ShopType.class);
                shopTypes.add(shopType);
            }
            return Result.ok(shopTypes);
        }
//        从数据库查询
        List<ShopType> shopTypes = query().orderByAsc("sort").list();
        ArrayList<String> list = new ArrayList<>();
        for (ShopType shopType : shopTypes) {
            String s = JSONUtil.toJsonStr(shopType);
            list.add(s);
        }
//          保存到redis
        stringRedisTemplate.opsForList().rightPushAll(key,list);
        stringRedisTemplate.expire(key,CACHE_SHOP_TYPE_TTL, TimeUnit.MINUTES);
        log.error("come in");
        return Result.ok(list);
    }
}
