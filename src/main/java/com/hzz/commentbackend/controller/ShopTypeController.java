package com.hzz.commentbackend.controller;


import com.hzz.commentbackend.dto.Result;
import com.hzz.commentbackend.entity.ShopType;
import com.hzz.commentbackend.service.IShopTypeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/shop-type")
public class ShopTypeController {
    @Resource
    private IShopTypeService typeService;

    @GetMapping("list")
    public Result queryTypeList() {
        return typeService.queryBySort();
    }
}
