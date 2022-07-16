package com.hzz.commentbackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hzz.commentbackend.dto.Result;
import com.hzz.commentbackend.dto.UserDTO;
import com.hzz.commentbackend.entity.Blog;
import com.hzz.commentbackend.entity.User;
import com.hzz.commentbackend.mapper.BlogMapper;
import com.hzz.commentbackend.service.IBlogService;
import com.hzz.commentbackend.service.IUserService;
import com.hzz.commentbackend.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private IUserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryHotBlog(Integer current) {

        return Result.ok("功能未完善");
    }

    @Override
    public Result queryBlogById(Long id) {
        // 1.查询blog
        Blog blog = getById(id);
        if (blog == null) {
            return Result.fail("笔记不存在！");
        }
        // 2.查询blog有关的用户
        queryBlogUser(blog);
        // 3.查询blog是否被点赞
        isBlogLiked(blog);
        return Result.ok(blog);
    }

    private void isBlogLiked(Blog blog) {
        // 1.获取登录用户
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            // 用户未登录，无需查询是否点赞
            return;
        }
        Long userId = user.getId();
        // 2.判断当前登录用户是否已经点赞
        String key = "blog:liked:" + blog.getId();
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        blog.setIsLike(score != null);
    }

    @Override
    public Result likeBlog(Long id) {

        return Result.ok("功能未完善");
    }

    @Override
    public Result queryBlogLikes(Long id) {

        return Result.ok("功能未完善");
    }

    @Override
    public Result saveBlog(Blog blog) {

        return Result.ok("功能未完善");
    }

    @Override
    public Result queryBlogOfFollow(Long max, Integer offset) {


        return Result.ok("功能未完善");
    }

    private void queryBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }
}
