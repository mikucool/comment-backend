package com.hzz.commentbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hzz.commentbackend.entity.Voucher;
import org.apache.ibatis.annotations.Param;

import java.util.List;


public interface VoucherMapper extends BaseMapper<Voucher> {

    List<Voucher> queryVoucherOfShop(@Param("shopId") Long shopId);
}
