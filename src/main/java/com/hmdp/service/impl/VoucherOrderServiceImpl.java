package com.hmdp.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.SimpleRedisLock;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    @Override
    public Result seckillVoucher(Long voucherId) throws InterruptedException {
//        查询
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
//        判断是否开始结束
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("秒杀尚未开始");
        }
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("秒杀已经结束");
        }
//        判断库存
        Integer stock = voucher.getStock();
        if (stock < 1) {
            return Result.fail("秒杀商品已经完了,库存不足");
        }
        Long userId = UserHolder.getUser().getId();
//        防止事务失效
//        synchronized (userId.toString().intern()){

//        SimpleRedisLock lock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
//        boolean isLock = lock.tryLock(1200);

        RLock lock = redissonClient.getLock("lock:order:" + userId);
        boolean isLock = lock.tryLock(1L, TimeUnit.SECONDS);
//        多集群时一个用户发送请求到达俩个服务器
        if (!isLock) {
//            获取锁失败
            return Result.fail("one man can only make one fucking order");
        }
        /**
         * 获取代理对象 在同一个类中 非事务方法调用事务方法 会导致事务失效，
         * 得采用AopContext.currentProxy().xx进行调用 事务才能生效
         */
        IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
        try {
            return proxy.createVoucherOrder(voucherId);//this
//            事务提交之后才会暴露锁
        } finally {
            lock.unlock();
        }
    }


@Transactional
public Result createVoucherOrder(Long voucherId){
    SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
            Integer stock = voucher.getStock();
            //        根据用户id和优惠券id查看是不是黄牛 同一个用户重复下单
            Long userId = UserHolder.getUser().getId();

            int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
            if (count>0){
                return Result.fail("one user can only purchase one fucking order");
            }
////        扣减库存
//            LambdaUpdateWrapper<SeckillVoucher> updateWrapper = new LambdaUpdateWrapper<>();
//            updateWrapper.eq(SeckillVoucher::getVoucherId,voucherId).set(SeckillVoucher::getStock,stock-1);
//            boolean isTrue = seckillVoucherService.update(updateWrapper);
//    TODO 使用lambaUpdateWrapper 不会出现线程并发问题


        boolean isTrue = seckillVoucherService.update().
                setSql("stock = stock -1").
                eq("voucher_id", voucherId)
                .gt("stock",0)//乐观锁 前提是修改数据 cas     compare and swap   库存大于0随便减
                .update();

            if (!isTrue) {
                return Result.fail("库存不足");
            }
            log.info("更新成功");
            //        创建订单
            VoucherOrder voucherOrder = new VoucherOrder();
            long orderId = redisIdWorker.nextId("order");
            voucherOrder.setId(orderId);

            voucherOrder.setUserId(userId);
            voucherOrder.setVoucherId(voucherId);
            save(voucherOrder);
            //        返回订单id
            return Result.ok(orderId);

        }
        }

