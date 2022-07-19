package com.hzz.commentbackend;

import com.hzz.commentbackend.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class CommentBackendApplicationTests {

    @Resource
    private RedisIdWorker redisIdWorker;

    @Test
    void testIdWorker() {

    }
}
