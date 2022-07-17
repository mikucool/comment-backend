package com.hzz.commentbackend.utils;

import com.hzz.commentbackend.dto.UserDTO;
import com.hzz.commentbackend.entity.User;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 判断 session 中是否有 user
        HttpSession session = request.getSession();
        Object user = session.getAttribute("user");
        if(user == null) {
            response.setStatus(401);
            return false; // 拦截
        }
        // 将 user 存到 ThreadLocal 中
        UserHolder.saveUser((UserDTO) user);
        // 有用户，则放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 移除用户
        UserHolder.removeUser();
    }
}
