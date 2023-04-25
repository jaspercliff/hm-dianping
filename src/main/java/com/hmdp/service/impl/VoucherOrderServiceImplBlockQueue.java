//package com.hmdp.service.impl;
//
//import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
//import com.hmdp.dto.Result;
//import com.hmdp.entity.VoucherOrder;
//import com.hmdp.mapper.VoucherOrderMapper;
//import com.hmdp.service.ISeckillVoucherService;
//import com.hmdp.service.IVoucherOrderService;
//import com.hmdp.utils.RedisIdWorker;
//import com.hmdp.utils.UserHolder;
//import lombok.extern.slf4j.Slf4j;
//import org.redisson.api.RLock;
//import org.redisson.api.RedissonClient;
//import org.springframework.aop.framework.AopContext;
//import org.springframework.core.io.ClassPathResource;
//import org.springframework.data.redis.core.StringRedisTemplate;
//import org.springframework.data.redis.core.script.DefaultRedisScript;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import javax.annotation.PostConstruct;
//import javax.annotation.Resource;
//import java.util.Collections;
//import java.util.concurrent.ArrayBlockingQueue;
//import java.util.concurrent.BlockingQueue;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
////@Slf4j
////@Service
//public class VoucherOrderServiceImplBlockQueue extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
//
//    @Resource
//    private ISeckillVoucherService seckillVoucherService;
//
//    @Resource
//    private RedisIdWorker redisIdWorker;
//
//    @Resource
//    private StringRedisTemplate stringRedisTemplate;
//
//    @Resource
//    private RedissonClient redissonClient;
//
//    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
//
//    static {
//        SECKILL_SCRIPT = new DefaultRedisScript<>();
//        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
//        SECKILL_SCRIPT.setResultType(Long.class);
//    }
//
//    private  IVoucherOrderService proxy;
//
//    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();
//
//    @PostConstruct
//    private void init(){
//        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
//    }
//
//    /**
//     * 减少库存 生成订单
//     *
//     * @param voucherId
//     * @return
//     * @throws InterruptedException
//     */
//
//    @Override
//    public Result seckillVoucher(Long voucherId) {
//        Long userId = UserHolder.getUser().getId();
//        // 脚本 key argv
//        Long result = stringRedisTemplate.execute(SECKILL_SCRIPT,
//                Collections.emptyList(), voucherId.toString(), userId.toString());
//        int r = result.intValue();
//        if (r != 0) {
//            return Result.fail(r == 1 ? "the stock is fucking gone!!" : "one man can only place one  fucking order");
//        }
////        0 有购买资格 把下单信息放到阻塞队列里面
//        //        创建订单
//        VoucherOrder voucherOrder = new VoucherOrder();
//        long orderId = redisIdWorker.nextId("order");
//        voucherOrder.setId(orderId);
//        voucherOrder.setUserId(userId);
//        voucherOrder.setVoucherId(voucherId);
//////放入阻塞队列
//        orderTasks.add(voucherOrder);
////获取代理对象
//        proxy = (IVoucherOrderService) AopContext.currentProxy();
//        return Result.ok(orderId);
//    }
//
//    private BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024 * 1024);
//    private class VoucherOrderHandler implements Runnable{
//        @Override
//        public void run() {
//            while (true){
////              获取队列中的订单
//                try {
//                    VoucherOrder voucherOrder = orderTasks.take();
//                    handleVoucherOrder(voucherOrder);
//                } catch (InterruptedException e) {
//                    log.error("处理订单异常");
//                }
//            }
//        }
//
//
//        private void handleVoucherOrder(VoucherOrder voucherOrder) {
//            Long userId = voucherOrder.getUserId();
//            RLock lock = redissonClient.getLock("lock:order:" + userId);
//            boolean isLock = lock.tryLock();
//            if (!isLock) {
////            获取锁失败
//                log.error("不允许重复下单");
//                return;
//            }
//            /**
//             * 获取代理对象 在同一个类中 非事务方法调用事务方法 会导致事务失效，
//             * 得采用AopContext.currentProxy().xx进行调用 事务才能生效
//             * 这里子线程获取不到代理对象 只能去初始化的时候去获取
//             */
//            try {
//                proxy.createVoucherOrder(voucherOrder);//this
////            事务提交之后才会暴露锁
//            } finally {
//                lock.unlock();
//            }
//        }
//    }
//
//
//    @Transactional
//    public void createVoucherOrder(VoucherOrder voucherOrder){
//        boolean isTrue = seckillVoucherService.update().
//                setSql("stock = stock -1").
//                eq("voucher_id", voucherOrder.getVoucherId())
//                .gt("stock",0)//乐观锁 前提是修改数据 cas     compare and swap   库存大于0随便减
//                .update();
//
//        if (!isTrue) {
//            log.error("库存不足");
//        }
//        log.info("更新成功");
//        //        创建订单
//        save(voucherOrder);
//
//    }
//}
//
