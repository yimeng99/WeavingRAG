package com.weaving.llm.common.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.weaving.llm.common.domain.User;

import java.util.List;

/**
 * @Author: 依梦
 * @Date: 2025/10/27
 * @Description: 用户服务接口
 */
public interface UserService extends IService<User> {

    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户对象
     */
    User getUserByUsername(String username);

    /**
     * 根据邮箱查询用户
     * @param email 邮箱
     * @return 用户对象
     */
    User getUserByEmail(String email);

    /**
     * 根据手机号查询用户
     * @param phone 手机号
     * @return 用户对象
     */
    User getUserByPhone(String phone);

    /**
     * 根据 token 查询用户
     * @param token 用户 token
     * @return 用户对象
     */
    User getUserByToken(String token);
}
