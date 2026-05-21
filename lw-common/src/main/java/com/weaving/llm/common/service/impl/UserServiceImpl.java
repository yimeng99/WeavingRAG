package com.weaving.llm.common.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weaving.llm.common.domain.User;
import com.weaving.llm.common.mapper.UserMapper;
import com.weaving.llm.common.service.UserService;
import org.springframework.stereotype.Service;

/**
 * @Author: 依梦
 * @Date: 2025/10/27
 * @Description: 用户服务实现类
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public User getUserByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return this.getOne(wrapper);
    }

    @Override
    public User getUserByEmail(String email) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, email);
        return this.getOne(wrapper);
    }

    @Override
    public User getUserByPhone(String phone) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, phone);
        return this.getOne(wrapper);
    }

    @Override
    public User getUserByToken(String token) {
        // JWT 模式下，这个方法不再使用
        // 用户信息应该从 TokenInterceptor 中直接解析设置
        throw new UnsupportedOperationException("JWT 模式下不支持此方法，请使用 TokenStore 解析 JWT");
    }
}
