package com.hzz.commentbackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hzz.commentbackend.dto.Result;
import com.hzz.commentbackend.entity.SeckillVoucher;
import com.hzz.commentbackend.entity.VoucherOrder;
import com.hzz.commentbackend.mapper.VoucherOrderMapper;
import com.hzz.commentbackend.service.ISeckillVoucherService;
import com.hzz.commentbackend.service.IVoucherOrderService;
import com.hzz.commentbackend.service.IVoucherService;
import com.hzz.commentbackend.utils.RedisIdWorker;
import com.hzz.commentbackend.utils.SimpleRedisLock;
import com.hzz.commentbackend.utils.UserHolder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@Service
public class IVoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService iSeckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    @Override
    public Result seckillVoucher(Long voucherId) {
        // 1. 查询优惠券
        SeckillVoucher seckillVoucher = iSeckillVoucherService.getById(voucherId);
        // 2. 是否开始
        if (seckillVoucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("活动尚未开始");
        }
        // 3. 是否结束
        if (seckillVoucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("活动尚已结束");
        }
        // 4. 库存是否充足
        if (seckillVoucher.getStock() < 1) {
            return Result.fail("库存不足");
        }

        Long userId = UserHolder.getUser().getId();

        // 获取分布式锁
//        SimpleRedisLock lock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        boolean isLock = lock.tryLock();
        if (!isLock) {
            // 获取锁失败
            return Result.fail("不允许重复下单");
        }
        try {
            // 由于事务的提交是 spring 提供的代理对象完成的，所以需要代理对象执行 createVoucherOrder 方法才能完成事务操作
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        } finally {
            lock.unlock();
        }
    }


    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        // 判断是否已经领取过优惠券
        Long userId = UserHolder.getUser().getId();
        int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        if (count > 0) {
            return Result.fail("该用户已经领取过此优惠券了");
        }

        // 5. 扣减库存
        boolean success = iSeckillVoucherService.update()
                .setSql("stock = stock - 1")    // set stock = stock - 1
                .eq("voucher_id", voucherId).gt("stock", 0) // where id = ? and stock > 0
                .update();
        if (!success) {
            // 扣减失败
            return Result.fail("库存不足");
        }
        // 6. 创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        // 6. 1 订单 id
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        // 6.2 用户 id
        voucherOrder.setUserId(userId);
        // 6.3 代金券 id
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);
        // 7. 返回订单 id
        return Result.ok(orderId);
    }
}
