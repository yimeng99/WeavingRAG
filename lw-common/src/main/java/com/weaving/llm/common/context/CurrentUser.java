package com.weaving.llm.common.context;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 当前登录用户信息
 */
@Data
@Builder
public class CurrentUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称/显示名称
     */
    private String nickname;

    /**
     * 头像 URL
     */
    private String avatar;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 角色
     */
    private String role;

    /**
     * Token
     */
    private String token;
}
