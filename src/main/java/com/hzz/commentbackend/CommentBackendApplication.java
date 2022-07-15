package com.hzz.commentbackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.hzz.commentbackend.mapper")
@SpringBootApplication
public class CommentBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommentBackendApplication.class, args);
    }

}
