package com.hzz.commentbackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hzz.commentbackend.dto.Result;
import com.hzz.commentbackend.entity.Voucher;


public interface IVoucherService extends IService<Voucher> {

    Result queryVoucherOfShop(Long shopId);

    void addSeckillVoucher(Voucher voucher);
}
