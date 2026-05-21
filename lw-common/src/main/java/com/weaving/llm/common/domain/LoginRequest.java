package com.weaving.llm.common.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: 依梦
 * @Date: 2025/10/27
 * @Description: 登录请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 密码
     */
    private String password;
    
    /**
     * 邮箱 (注册时使用)
     */
    private String email;
    
    /**
     * 记住我
     */
    private Boolean rememberMe = false;
}
