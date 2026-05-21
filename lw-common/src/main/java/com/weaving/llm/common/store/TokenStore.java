package com.weaving.llm.common.store;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.weaving.llm.common.utils.JwtUtil;


/**
 * Token 存储器（基于 JWT，userId 存储在 token 中）
 * JWT Token 自身包含用户信息，无需额外存储
 *
 * @Author: 依梦
 * @Date: 2025/10/27
 */
@Slf4j
@Component
public class TokenStore {

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 存储 token（JWT 模式下只需记录用户最后登录时间）
     *
     * @param token  token 字符串
     * @param userId 用户 ID
     * @param username 用户名
     * @return 生成的 JWT Token
     */
    public String generateAndStoreToken(Long userId, String username) {
        String token = jwtUtil.generateToken(userId, username);
        log.debug("为用户 {} 生成 JWT Token，userId={}", username, userId);
        return token;
    }

    /**
     * 根据 token 获取用户 ID（从 JWT 中解析）
     *
     * @param token token 字符串
     * @return 用户 ID，token 无效返回 null
     */
    public Long getUserIdByToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        try {
            return jwtUtil.getUserIdFromToken(token);
        } catch (Exception e) {
            log.warn("解析 Token 失败：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 根据 token 获取用户名（从 JWT 中解析）
     *
     * @param token token 字符串
     * @return 用户名，token 无效返回 null
     */
    public String getUsernameByToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        try {
            return jwtUtil.getUsernameFromToken(token);
        } catch (Exception e) {
            log.warn("解析 Token 失败：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 移除 token（JWT 模式下无需操作，Token 过期自动失效）
     * 如果需要实现登出功能，可以结合 Redis 实现黑名单机制
     *
     * @param token token 字符串
     */
    public void removeToken(String token) {
        // JWT 模式下，Token 在有效期内一直有效
        // 如需立即失效，需要结合 Redis 实现黑名单
        log.debug("Token 注销请求（实际 JWT 仍然有效直到过期）");
    }

    /**
     * 移除用户的所有 token（JWT 模式下无法实现，除非使用黑名单）
     *
     * @param userId 用户 ID
     */
    public void removeUserTokens(Long userId) {
        // JWT 模式下无法主动使已发放的 Token 失效
        log.debug("JWT 模式下无法主动注销用户的所有 Token");
    }

    /**
     * 检查 token 是否有效（验证 JWT 签名和有效期）
     *
     * @param token token 字符串
     * @return true 有效，false 无效
     */
    public boolean isValidToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        return jwtUtil.validateToken(token);
    }

    /**
     * 刷新 Token
     *
     * @param oldToken 原 Token
     * @param userId 用户 ID
     * @param username 用户名
     * @return 新的 Token，刷新失败返回 null
     */
    public String refreshToken(String oldToken, Long userId, String username) {
        return jwtUtil.refreshToken(oldToken, userId, username);
    }
}
