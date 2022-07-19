package com.hzz.commentbackend.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hzz.commentbackend.dto.Result;
import com.hzz.commentbackend.entity.Shop;
import com.hzz.commentbackend.mapper.ShopMapper;
import com.hzz.commentbackend.service.IShopService;
import com.hzz.commentbackend.utils.RedisConstants;
import com.hzz.commentbackend.utils.RedisData;
import com.hzz.commentbackend.utils.SystemConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hzz.commentbackend.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.hzz.commentbackend.utils.RedisConstants.LOCK_SHOP_KEY;


@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {


    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryById(Long id) {
        // 缓存穿透
//        Shop shop = queryWithPassThrough(id);

        // 互斥锁解决缓存击穿
        Shop shop = queryWithMutex(id);

        // 逻辑过期解决缓存击穿
//        Shop shop = queryWithLogicalExpire(id);
        if (shop == null) {
            return Result.fail("not found the shop");
        }
        return Result.ok(shop);
    }

    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("id 不能为空");
        }
        updateById(shop);
        stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
        return Result.ok();
    }

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        Page<Shop> page = query()
                .eq("type_id", typeId)
                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
        // 返回数据
        return Result.ok(page.getRecords());
    }

    // 解决缓存穿透
    private Shop queryWithPassThrough(Long id) {
        String key = CACHE_SHOP_KEY + id;
        // 1. 从 redis 查询商户数据
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        // 2. 判断是否存在
        if (StrUtil.isNotBlank(shopJson)) {
            // 3. 存在，返回
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        if (shopJson != null) { // shopJson 为转义字符和 "" 等情况
            return null;
        }

        // 4. redis 中不存在该商户，查询数据库
        Shop shop = getById(id);
        // 4.1 数据库中不存在该商户，将该商户设为空存入 redis，然后返回错误
        if (shop == null) {
            stringRedisTemplate.opsForValue().set(key, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        // 4.2 数据库中存在该商户，将该商户存入 redis 中
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        // 返回
        return shop;
    }

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    // 逻辑过期解决缓存击穿
    private Shop queryWithLogicalExpire(Long id) {
        String key = CACHE_SHOP_KEY + id;
        // 1. 从 redis 查询商户数据
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        // 2. 判断是否存在
        if (StrUtil.isBlank(shopJson)) {
            // 3. 不存在，返回
            return null;
        }
        // 4. 命中，判断过期时间
        System.out.println(shopJson);
        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        System.out.println(redisData);
        Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
        LocalDateTime expireTime = redisData.getExpireTime();
        // 4.1 没过期
        if (expireTime.isAfter(LocalDateTime.now())) {
            return shop;
        }
        // 4.2 过期
        // 5 缓存重建
        // 5.1 获取锁
        boolean isLock = tryLock(LOCK_SHOP_KEY + id);
        if (isLock) {
            // 5.3 获取成功，开启新的线程重建 redis 缓存
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    saveShop2Redis(id, 3600L);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    unlock(LOCK_SHOP_KEY + id);
                }
            });

        }
        // 5.4 获取失败，返回商铺信息
        // 返回
        return shop;
    }

    // 使用互斥锁解决缓存击穿
    private Shop queryWithMutex(Long id) {
        String key = CACHE_SHOP_KEY + id;
        // 1. 从 redis 查询商户数据
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        // 2. 判断是否存在
        if (StrUtil.isNotBlank(shopJson)) {
            // 3. 存在，返回
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        if (shopJson != null) { // shopJson 为转义字符和 "" 等情况
            return null;
        }
        Shop shop = null;

        try {
            // 4. redis 中不存在该商户，查询数据库并重建缓存
            // 4.1 获取锁
            String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
            boolean isLock = tryLock(lockKey);
            // 4.2 判断是否获取锁成功
            // 4.3 获取失败, 休眠重试
            if (!isLock) {
                Thread.sleep(50);
                return queryWithMutex(id);
            }
            // 4.4 获取锁成功，查询数据库，重建缓存
            // 获取锁成功后应先判断缓存是否已被重建
            String shopAfterUnlock = stringRedisTemplate.opsForValue().get(key);
            if (StrUtil.isNotBlank(shopAfterUnlock)) {
                unlock(key);
                return JSONUtil.toBean(shopAfterUnlock, Shop.class);
            }
            shop = getById(id);
            // 数据库中不存在该商户，将该商户设为空存入 redis，然后返回错误
            if (shop == null) {
                stringRedisTemplate.opsForValue().set(key, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            // 数据库中存在该商户，将该商户存入 redis 中
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // 释放锁
            unlock(key);
        }
        // 返回
        return shop;
    }

    // 保存 Shop 对象到 Redis 中
    private void saveShop2Redis(Long id, Long expireSeconds) {
        // 1. 查询数据库
        Shop shop = getById(id);
        // 2. 封装逻辑过期时间
        RedisData data = new RedisData();
        data.setData(shop);
        data.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        // 3. 写入 redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(data));
    }

    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }
}
