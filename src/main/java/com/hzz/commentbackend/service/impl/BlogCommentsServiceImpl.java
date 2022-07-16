package com.hzz.commentbackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hzz.commentbackend.entity.BlogComments;
import com.hzz.commentbackend.mapper.BlogCommentsMapper;
import com.hzz.commentbackend.service.IBlogCommentsService;
import org.springframework.stereotype.Service;

@Service
public class BlogCommentsServiceImpl extends ServiceImpl<BlogCommentsMapper, BlogComments> implements IBlogCommentsService {

}
