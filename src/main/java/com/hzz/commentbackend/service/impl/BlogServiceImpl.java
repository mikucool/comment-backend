package com.hzz.commentbackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hzz.commentbackend.dto.Result;
import com.hzz.commentbackend.dto.UserDTO;
import com.hzz.commentbackend.entity.Blog;
import com.hzz.commentbackend.entity.User;
import com.hzz.commentbackend.mapper.BlogMapper;
import com.hzz.commentbackend.service.IBlogService;
import com.hzz.commentbackend.service.IUserService;
import com.hzz.commentbackend.utils.RedisConstants;
import com.hzz.commentbackend.utils.SystemConstants;
import com.hzz.commentbackend.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.hzz.commentbackend.utils.RedisConstants.BLOG_LIKED_KEY;

@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private IUserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(blog -> {
            this.queryBlogUser(blog);
            this.isBlogLiked(blog);
        });
        return Result.ok(records);
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
        String key = BLOG_LIKED_KEY + blog.getId();
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        blog.setIsLike(score != null);
    }

    @Override
    public Result likeBlog(Long id) {
        // 1. 判断当前登录用户是否点赞
        Long userId = UserHolder.getUser().getId();
        String key = BLOG_LIKED_KEY + id;
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        if (score == null) {
            // 2. 未点赞，数据库 + 1， 并添加到 redis 的 SortedSet 集合中
            boolean isSuccess = update().setSql("liked = liked + 1").eq("id", id).update();
            if (isSuccess) {
                stringRedisTemplate.opsForZSet().add(key, userId.toString(), System.currentTimeMillis());
            }
        } else {
            // 3. 已点赞，数据库 - 1， redis 的 SortedSet 中移除该用户
            boolean isSuccess = update().setSql("liked = liked - 1").eq("id", id).update();
            if (isSuccess) {
                stringRedisTemplate.opsForZSet().remove(key, userId.toString());
            }
        }

        return Result.ok();
    }

    @Override
    public Result queryBlogLikes(Long id) {
        // 1. 查询前五个点赞的用户 zrange key 0 4
        String key = BLOG_LIKED_KEY + id;
        Set<String> fiveLikes = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        if (fiveLikes == null || fiveLikes.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        // 2. 根据用户 id 获取用户信息
        List<Long> ids = fiveLikes.stream().map(Long::valueOf).collect(Collectors.toList());
        // 封装到 UserDTO 进行敏感信息的过滤,
        // where id in (5, 1) order by field (id, 5, 1) => 保证查询的数据跟传进的 ids 顺序一致
        String idStr = StrUtil.join(",", ids);
        List<UserDTO> userDTOS = userService.query()
                .in("id", ids)
                .last("ORDER BY FIELD(id," + idStr + ")").list().stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        // 3. 返回
        return Result.ok(userDTOS);
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
