package com.hzz.commentbackend.service;

import com.hzz.commentbackend.dto.Result;
import com.hzz.commentbackend.entity.VoucherOrder;

public interface IVoucherOrderService {
    Result seckillVoucher(Long voucherId);

    void createVoucherOrder(VoucherOrder voucherOrder);
}
