package com.hzz.commentbackend.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hzz.commentbackend.dto.Result;
import com.hzz.commentbackend.entity.ShopType;
import com.hzz.commentbackend.mapper.ShopTypeMapper;
import com.hzz.commentbackend.service.IShopTypeService;
import com.hzz.commentbackend.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryBySort() {
        String key = RedisConstants.CACHE_SHOPTYPE_KEY;
        String typeList = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(typeList)) {
            List<ShopType> shopTypes = JSONUtil.toList(typeList, ShopType.class);
            return Result.ok(shopTypes);
        }

        List<ShopType> shopTypes = query().orderByAsc("sort").list();
        if (shopTypes == null) {
            return Result.fail("Not found the shop type");
        }
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shopTypes), RedisConstants.CACHE_SHOPTYPE_TTL, TimeUnit.MINUTES);

        return Result.ok(shopTypes);
    }
}
