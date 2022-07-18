package com.hzz.commentbackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hzz.commentbackend.dto.Result;
import com.hzz.commentbackend.entity.ShopType;

public interface IShopTypeService extends IService<ShopType> {

    Result queryBySort();
}
