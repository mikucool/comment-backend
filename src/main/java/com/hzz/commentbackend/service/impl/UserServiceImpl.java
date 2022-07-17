package com.hzz.commentbackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hzz.commentbackend.dto.LoginFormDTO;
import com.hzz.commentbackend.dto.UserDTO;
import com.hzz.commentbackend.entity.User;
import com.hzz.commentbackend.mapper.UserMapper;
import com.hzz.commentbackend.service.IUserService;
import com.hzz.commentbackend.dto.Result;
import com.hzz.commentbackend.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hzz.commentbackend.utils.RedisConstants.*;
import static com.hzz.commentbackend.utils.SystemConstants.USER_NICK_NAME_PREFIX;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 1.校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 2.如果不符合，返回错误信息
            return Result.fail("手机号格式错误！");
        }
        // 3.符合，生成验证码
        String code = RandomUtil.randomNumbers(6);

        // 4.保存验证码到 redis
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);

        // TODO 5.此处应为验证码发送到客户手机的业务，这里只是简单模拟一下
        log.debug("发送短信验证码成功，验证码：{}", code);
        // 返回ok
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // 1.校验手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 2.如果不符合，返回错误信息
            return Result.fail("手机号格式错误！");
        }

        // 3. 从 redis 中获取并校验验证码
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        String code = loginForm.getCode();

        if(cacheCode == null || !cacheCode.equals(code)) {
            return Result.fail("验证吗错误");
        }

        // 4. 一致，根据手机号查用户
        User user = query().eq("phone", phone).one();

        // 5. 用户是否存在
        if(user == null) {
            user = createUserWithPhone(phone);
        }

        // 6. 保存用户信息到 redis 中
        // 6.1 以用户 phone 生成 token 作为 key 保存到 redis 中，用于判断用户是否登录
        String token = UUID.randomUUID().toString(true);
        // 6.2 redis 使用 Hash 保存 user 对象数据
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                                        CopyOptions.create().setIgnoreNullValue(true)
                                                .setFieldValueEditor((fn, fv) -> fv.toString()));
        stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY + token, userMap);
        stringRedisTemplate.expire(LOGIN_USER_KEY + token, LOGIN_USER_TTL, TimeUnit.MINUTES); // 设置 token 有效期
        // 6.3 返回 token 给客户端，保存在客户端本地，在每次访问后端时都会携带 token 以判断用户是否登录
        return Result.ok(token);

    }

    @Override
    public Result sign() {
        return null;
    }

    @Override
    public Result signCount() {
        return null;
    }


    private User createUserWithPhone(String phone) {
        // 1.创建用户
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        // 2.保存用户
        save(user);
        return user;
    }
}
