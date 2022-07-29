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
import com.hzz.commentbackend.utils.RedisConstants;
import com.hzz.commentbackend.utils.RegexUtils;
import com.hzz.commentbackend.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
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

        if (cacheCode == null || !cacheCode.equals(code)) {
            return Result.fail("验证吗错误");
        }

        // 4. 一致，根据手机号查用户
        User user = query().eq("phone", phone).one();

        // 5. 用户是否存在
        if (user == null) {
            user = createUserWithPhone(phone);
        }

        // 6. 保存用户信息到 redis 中
        // 6.1 生成 token 作为 key 保存到 redis 中，用于判断用户是否登录
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
        // 获取当前用户
        Long userId = UserHolder.getUser().getId();
        // 获取日期
        LocalDateTime time = LocalDateTime.now();
        // 拼接 key
        String keySuffix = time.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;
        // 获取当前时间是这个月的第几天
        int dayOfMonth = time.getDayOfMonth(); // 1 - 31
        // 写入 redis ： SETBIT key offset 1
        stringRedisTemplate.opsForValue().setBit(key, dayOfMonth - 1, true);
        return Result.ok();
    }

    @Override
    public Result signCount() {
        // 获取当前用户
        Long userId = UserHolder.getUser().getId();
        // 获取日期
        LocalDateTime time = LocalDateTime.now();
        // 拼接 key
        String keySuffix = time.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;
        // 获取当前时间是这个月的第几天
        int dayOfMonth = time.getDayOfMonth(); // 1 - 31
        // 获取截止今天本月的所有签到结果，返回的是一个十进制的数字 BITFIELD key get u15 0
        List<Long> result = stringRedisTemplate.opsForValue().bitField(
                key, BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth))
                        .valueAt(0)
        );
        if (result == null || result.isEmpty()) {
            return Result.ok(0);
        }
        Long num = result.get(0);
        if (num == null || num == 0) {
            return Result.ok(0);
        }
        // 循环遍历，和 1 进行与运算，1 为签到，0 为未签到
        int count = 0;
        while (true) {
            if ((num & 1 )== 0) {
                // 0 未签到
                break;
            } else {
                count++;
            }
            // 无符号右移，依次判断前面的天数是否签到
            num >>>= 1;
        }
        return Result.ok(count);
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
