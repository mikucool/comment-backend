package com.hzz.commentbackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hzz.commentbackend.entity.User;
import com.hzz.commentbackend.dto.Result;

import javax.servlet.http.HttpSession;

public interface IUserService extends IService<User> {

    Result sendCode(String phone, HttpSession session);

}
