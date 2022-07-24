package com.hzz.commentbackend.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hzz.commentbackend.dto.Result;
import com.hzz.commentbackend.dto.UserDTO;
import com.hzz.commentbackend.entity.Blog;
import com.hzz.commentbackend.service.IBlogService;
import com.hzz.commentbackend.utils.SystemConstants;
import com.hzz.commentbackend.utils.UserHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/blog")
public class BlogController {

    @Resource
    private IBlogService blogService;

    @PostMapping
    public Result saveBlog(@RequestBody Blog blog) {
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        blogService.save(blog);
        return Result.ok(blog.getId());
    }

    @PutMapping("/like/{id}")
    public Result likeBlog(@PathVariable("id") Long id) {
        return Result.ok("尚未完善");
    }

    @GetMapping("/of/me")
    public Result queryMyBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return Result.ok("尚未完善");
    }

    @GetMapping("/hot")
    public Result queryHotBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return blogService.queryHotBlog(current);
    }

    @GetMapping("/{id}")
    public Result queryBlogById(@PathVariable("id") Long id) {
        return Result.ok("尚未完善");
    }

    @GetMapping("/likes/{id}")
    public Result queryBlogLikes(@PathVariable("id") Long id) {
        return Result.ok("尚未完善");
    }

    @GetMapping("/of/user")
    public Result queryBlogByUserId(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam("id") Long id) {
        return Result.ok("尚未完善");
    }

    @GetMapping("/of/follow")
    public Result queryBlogOfFollow(
            @RequestParam("lastId") Long max, @RequestParam(value = "offset", defaultValue = "0") Integer offset){
        return Result.ok("尚未完善");
    }
}
