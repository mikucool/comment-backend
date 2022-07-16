package com.hzz.commentbackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hzz.commentbackend.dto.Result;
import com.hzz.commentbackend.entity.Shop;
import com.hzz.commentbackend.mapper.ShopMapper;
import com.hzz.commentbackend.service.IShopService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;


@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {


    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryById(Long id) {
        return Result.ok("功能未完善");
    }

    @Override
    @Transactional
    public Result update(Shop shop) {

        return Result.ok("功能未完善");
    }

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        return Result.ok("功能未完善");
    }
}
