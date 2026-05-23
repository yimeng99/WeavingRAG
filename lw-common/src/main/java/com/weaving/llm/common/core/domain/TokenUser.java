package com.weaving.llm.common.core.domain;

import lombok.Data;

/**
 * @Author: 依梦
 * @Date: 2025/9/21
 * @Description: LoginUser
 */
@Data
public class TokenUser {

    private Long userId;

    private String userName;

    private String currentRoleId;
}
