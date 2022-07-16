package com.hzz.commentbackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hzz.commentbackend.dto.Result;
import com.hzz.commentbackend.entity.Blog;

public interface IBlogService extends IService<Blog> {

    Result queryHotBlog(Integer current);

    Result queryBlogById(Long id);

    Result likeBlog(Long id);

    Result queryBlogLikes(Long id);

    Result saveBlog(Blog blog);

    Result queryBlogOfFollow(Long max, Integer offset);

}
