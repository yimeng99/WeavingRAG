package com.weaving.llm.common.core.domain.dto;

import lombok.Data;

/**
 * @Author: 依梦
 * @Date: 2025/9/21
 * @Description: LoginUserDto
 */
@Data
public class LoginUserDto {
    private String username;
    private String password;
    private String code;
    private String uuid;
    /**
     * 登录方式
     */
    private String type;
}
