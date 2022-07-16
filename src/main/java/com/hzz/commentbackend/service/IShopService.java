package com.hzz.commentbackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hzz.commentbackend.dto.Result;
import com.hzz.commentbackend.entity.Shop;

public interface IShopService extends IService<Shop> {

    Result queryById(Long id);

    Result update(Shop shop);

    Result queryShopByType(Integer typeId, Integer current, Double x, Double y);
}
