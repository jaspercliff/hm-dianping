---
--- Generated by Luanalysis
--- Created by jasper.
--- DateTime: 2023/3/23 22:23
---
-- local 局部变量
-- 参数列表
-- 优惠卷id  用户id 订单id
local voucherId = ARGV[1]
local userId = ARGV[2]
--local orderId = ARGV[3]
-- 数据key
--库存key 订单key
local stockKey = 'seckill:stock:' ..voucherId
local orderKey = 'seckill:order:' ..voucherId
-- determine whether stock is >=0
if(tonumber(redis.call('get',stockKey)) <= 0) then
    return 1
end
-- determine whether is a same user
if(redis.call('sismember',orderKey,userId) == 1) then
    return 2
end

-- stock deduction
redis.call('incrby',stockKey ,-1)
-- order
redis.call('sadd',orderKey,userId)
--发送消息到队列 xadd stream.orders * k1 v1
--redis.call('xadd','stream.orders','*','userId',userId,'voucherId',voucherId,'id',orderId)
--@TableId(value = "id", type = IdType.INPUT)
--private Long id;
--创建订单
return 0


