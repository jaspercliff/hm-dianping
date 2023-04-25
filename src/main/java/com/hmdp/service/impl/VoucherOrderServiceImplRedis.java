//package com.hmdp.service.impl;
//
//import cn.hutool.core.bean.BeanUtil;
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
//import org.springframework.data.redis.connection.stream.*;
//import org.springframework.data.redis.core.StringRedisTemplate;
//import org.springframework.data.redis.core.script.DefaultRedisScript;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import javax.annotation.PostConstruct;
//import javax.annotation.Resource;
//import java.time.Duration;
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//@Slf4j
//@Service
//public class VoucherOrderServiceImplRedis extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
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
////    生产者
//    @Override
//    public Result seckillVoucher(Long voucherId) {
//        Long userId = UserHolder.getUser().getId();
//        long orderId = redisIdWorker.nextId("order");
//        Long result = stringRedisTemplate.execute(SECKILL_SCRIPT,
//                Collections.emptyList(), voucherId.toString(), userId.toString(),String.valueOf(orderId));
////        Returns a String object representing this Long's value
////        Returns the string representation of the long argument.
//        int r = result.intValue();
//        if (r != 0) {
//            return Result.fail(r == 1 ? "the stock is fucking gone!!" : "one man can only place one  fucking order");
//        }
////        0 有购买资格 把下单信息放到阻塞队列里
////获取代理对象  在处理队列时需要用到
//        proxy = (IVoucherOrderService) AopContext.currentProxy();
//        return Result.ok(orderId);
//    }
//
//
//    private class VoucherOrderHandler implements Runnable{
//        String queueName = "stream.orders";
//        @Override
//        public void run() {
//            while (true){
//                try {
////                获取消息队列中的订单 xreadgroup group g1 c1 count 1 block 2000 streams stream.orders >
//                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
//                            Consumer.from("g1", "c1"),
//                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
//                            StreamOffset.create(queueName, ReadOffset.lastConsumed())
//                    );
////                判断消息是否成功
//                    if(list == null||list.isEmpty()){
////                        失败 没有消息继续下一次循环
//                        continue;
//                    }
//                    MapRecord<String, Object, Object> record = list.get(0);
//                    Map<Object, Object> value = record.getValue();
//                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
////                有创建订单
//                    handleVoucherOrder(voucherOrder);
////                    XACK key group id [id ...]
////                ack确认 xack stream.orders g1  id
//                    stringRedisTemplate.opsForStream().acknowledge(queueName,"g1",record.getId());
//                } catch (Exception e) {
//                    log.error("处理订单异常");
//                    handlePendingList();
//                }
//            }
//        }
//
//        private void handlePendingList() {
//            while (true){
//                try {
////                获取pending-list中的订单 xreadgroup group g1 c1 count 1  streams stream.order 0
//                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
//                            Consumer.from("g1", "c1"),
//                            StreamReadOptions.empty().count(1),
//                            StreamOffset.create(queueName, ReadOffset.from("0"))
//                    );
////                判断消息是否成功
//                    if(list == null||list.isEmpty()){
////                        失败 说明pending-list中没有消息 结束循环
//                        break;
//                    }
//                    MapRecord<String, Object, Object> record = list.get(0);
//                    Map<Object, Object> value = record.getValue();
//                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
////                有创建订单
//                    handleVoucherOrder(voucherOrder);
////                ack确认 sack stream.order g1  id
//                    stringRedisTemplate.opsForStream().acknowledge(queueName,"g1",record.getId());
//                } catch (Exception e) {
//                    log.error("处理pending-list订单异常");
//                    try {
//                        Thread.sleep(20);//防止抛出异常太频繁
//                    } catch (InterruptedException ex) {
//                        ex.printStackTrace();
//                    }
//                }
//            }
//        }
//
//        /**
//         * 处理队列中的订单
//         * @param voucherOrder
//         */
//        private void handleVoucherOrder(VoucherOrder voucherOrder) {
//            Long userId = voucherOrder.getUserId();
//            RLock lock = redissonClient.getLock("lock:order:" + userId);
//        boolean isLock = lock.tryLock();
//        if (!isLock) {
////            获取锁失败
//            log.error("不允许重复下单");
//            return;
//        }
//        /**
//         * 获取代理对象 在同一个类中 非事务方法调用事务方法 会导致事务失效，
//         * 得采用AopContext.currentProxy().xx进行调用 事务才能生效
//         * 这里子线程获取不到代理对象 只能去初始化的时候去获取
//         */
//        try {
//             proxy.createVoucherOrder(voucherOrder);//this
//        } finally {
//            lock.unlock();
//        }
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
//
//
//
//
//
////    @Override
////    public Result seckillVoucher(Long voucherId) {
////        Long userId = UserHolder.getUser().getId();
////    脚本 key argv
////        Long result = stringRedisTemplate.execute(SECKILL_SCRIPT,
////                Collections.emptyList(), voucherId.toString(), userId.toString());
////        int r = result.intValue();
////        if (r != 0) {
////            return Result.fail(r == 1 ? "the stock is fucking gone!!" : "one man can only place one  fucking order");
////        }
//////        0 有购买资格 把下单信息放到阻塞队列里面
////        //        创建订单
////        VoucherOrder voucherOrder = new VoucherOrder();
////        long orderId = redisIdWorker.nextId("order");
////        voucherOrder.setId(orderId);
////        voucherOrder.setUserId(userId);
////        voucherOrder.setVoucherId(voucherId);
//////放入阻塞队列
////        orderTasks.add(voucherOrder);
//////获取代理对象
////        proxy = (IVoucherOrderService) AopContext.currentProxy();
////        return Result.ok(orderId);
////    }
//
////    private BlockingQueue <VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024 * 1024);
////    private class VoucherOrderHandler implements Runnable{
////        @Override
////        public void run() {
////            while (true){
//////                获取队列中的订单
////                try {
////                    VoucherOrder voucherOrder = orderTasks.take();
////                    handleVoucherOrder(voucherOrder);
////                } catch (InterruptedException e) {
////                    log.error("处理订单异常");
////                }
////            }
////        }
//
//
//
//
//
//
//
////    @Override
////    public Result seckillVoucher(Long voucherId) throws InterruptedException {
//////        查询
////        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
//////        判断是否开始结束
////        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
////            return Result.fail("秒杀尚未开始");
////        }
////        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
////            return Result.fail("秒杀已经结束");
////        }
//////        判断库存
////        Integer stock = voucher.getStock();
////        if (stock < 1) {
////            return Result.fail("秒杀商品已经完了,库存不足");
////        }
////        Long userId = UserHolder.getUser().getId();
//////        防止事务失效
//// 锁加在用户上，缩小了加锁的范围 加在方法上 表示this 是每个线程都给加锁了
//////        synchronized (userId.toString().intern()){
////
//////        SimpleRedisLock lock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
//////        boolean isLock = lock.tryLock(1200);
//
////       多集群时一个用户发送请求到达俩个服务器 得采用分布式锁
////        RLock lock = redissonClient.getLock("lock:order:" + userId);
////        boolean isLock = lock.tryLock(1L, TimeUnit.SECONDS);
//
////        if (!isLock) {
//////            获取锁失败
////            return Result.fail("one man can only make one fucking order");
////        }
////        /**
////         * 先释放锁 再去提交事务  事务由spring管理 是方法结束之后才会提交
////         *如果此时锁释放了事务没有提交有方法进来 则会判断该用户未下单 可能会出现线程安全问题
////         * 获取代理对象 在同一个类中 非事务方法调用事务方法 会导致事务失效，
////         * 得采用AopContext.currentProxy().xx进行调用 事务才能生效
////         */
////        IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
////        try {
////            return proxy.createVoucherOrder(voucherId);//this
//////            事务提交之后才会暴露锁
////        } finally {
////            lock.unlock();
////        }
////    }
////
////
////@Transactional
////public Result createVoucherOrder(Long voucherId){
////    SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
////            Integer stock = voucher.getStock();
////            //        根据用户id和优惠券id查看是不是黄牛 同一个用户重复下单
////            Long userId = UserHolder.getUser().getId();
////
////            int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
////            if (count>0){
////                return Result.fail("one user can only purchase one fucking order");
////            }
////////        扣减库存
//////            LambdaUpdateWrapper<SeckillVoucher> updateWrapper = new LambdaUpdateWrapper<>();
//////            updateWrapper.eq(SeckillVoucher::getVoucherId,voucherId).set(SeckillVoucher::getStock,stock-1);
//////            boolean isTrue = seckillVoucherService.update(updateWrapper);
//////    TODO 使用lambaUpdateWrapper 不会出现线程并发问题
////
////
////        boolean isTrue = seckillVoucherService.update().
////                setSql("stock = stock -1").
////                eq("voucher_id", voucherId)
////                .gt("stock",0)//乐观锁 前提是修改数据 cas     compare and swap   库存大于0随便减
////                .update();
////
////            if (!isTrue) {
////                return Result.fail("库存不足");
////            }
////            log.info("更新成功");
////            //        创建订单
////            VoucherOrder voucherOrder = new VoucherOrder();
////            long orderId = redisIdWorker.nextId("order");
////            voucherOrder.setId(orderId);
////
////            voucherOrder.setUserId(userId);
////            voucherOrder.setVoucherId(voucherId);
////            save(voucherOrder);
////            //        返回订单id
////            return Result.ok(orderId);
////
////        }
////        }
//
