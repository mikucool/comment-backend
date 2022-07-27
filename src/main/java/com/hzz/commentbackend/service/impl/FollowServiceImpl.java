package com.hzz.commentbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hzz.commentbackend.dto.Result;
import com.hzz.commentbackend.entity.Follow;
import com.hzz.commentbackend.mapper.FollowMapper;
import com.hzz.commentbackend.service.IFollowService;
import com.hzz.commentbackend.utils.UserHolder;
import org.springframework.stereotype.Service;


@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
        // 获取当前登录用户
        Long userId = UserHolder.getUser().getId();
        // 判断是否关注
        if (isFollow) {
            // 已关注 =》 取关，新增数据
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(followUserId);
            save(follow);
        } else {
            // 未关注 =》 关注，删除数据
            remove(new QueryWrapper<Follow>()
                        .eq("user_id", userId)
                        .eq("follow_user_id", followUserId));
        }
        return Result.ok();
    }

    @Override
    public Result isFollow(Long followUserId) {
        // 获取当前登录用户
        Long userId = UserHolder.getUser().getId();
        // 查询是否是否关注
        Integer count = query().eq("user_id", userId)
                .eq("follow_user_id", followUserId).count();
        return Result.ok(count > 0);
    }
}
