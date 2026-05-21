package com.weaving.llm.common.interceptor;

import com.weaving.llm.common.context.CurrentUser;
import com.weaving.llm.common.context.CurrentUserHolder;
import com.weaving.llm.common.domain.User;
import com.weaving.llm.common.service.UserService;
import com.weaving.llm.common.store.TokenStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;

/**
 * Token 拦截器
 * 解析请求中的 token，设置当前登录用户上下文
 */
@Slf4j
@Component
public class TokenInterceptor implements HandlerInterceptor {

    @Autowired
    private UserService userService;

    @Autowired
    private TokenStore tokenStore;

    /**
     * 不需要 token 验证的路径
     */
    private static final List<String> EXCLUDE_PATHS = Arrays.asList(
            "/v0/auth/login",
            "/v0/auth/register",
            "/v0/auth/refresh",
            "/error",
            "/swagger-ui",
            "/v3/api-docs"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String requestUri = request.getRequestURI();
        
        // 跳过不需要验证的路径
        if (isExcludePath(requestUri)) {
            return true;
        }

        // 从请求头或 Cookie 中获取 token
        String token = extractToken(request);
        
        if (token != null && !token.isEmpty()) {
            try {
                // 从 JWT Token 中解析用户 ID
                Long userId = tokenStore.getUserIdByToken(token);

                if (userId != null) {
                    // 根据用户 ID 查询用户
                    User user = userService.getById(userId);

                    if (user != null) {
                        // 构建当前用户上下文
                        CurrentUser currentUser = CurrentUser.builder()
                                .userId(user.getId())
                                .username(user.getUsername())
                                .nickname(user.getName())
                                .avatar(user.getAvatar())
                                .email(user.getEmail())
                                .phone(user.getPhone())
                                .role(user.getRole())
                                .token(token)
                                .build();

                        // 设置到 ThreadLocal
                        CurrentUserHolder.set(currentUser);

                        log.debug("Token 验证成功，用户：{}，URI：{}", user.getUsername(), requestUri);
                    } else {
                        log.warn("用户不存在，userId：{}，URI：{}", userId, requestUri);
                    }
                } else {
                    log.warn("Token 无效或已过期，token：{}，URI：{}", token, requestUri);
                }
            } catch (Exception e) {
                log.error("Token 解析失败，token：{}，错误：{}", token, e.getMessage());
            }
        } else {
            log.debug("请求未携带 Token，URI：{}", requestUri);
        }

        // 无论是否解析成功，都放行请求（由业务层决定是否要求登录）
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求结束后清除当前用户上下文，防止内存泄漏
        CurrentUserHolder.clear();
    }

    /**
     * 从请求中提取 token
     * 优先从 Authorization 头获取，其次从 Cookie 获取
     */
    private String extractToken(HttpServletRequest request) {
        // 1. 从 Authorization 头获取
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // 2. 从 Cookie 获取
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        // 3. 从请求参数获取（用于调试或特殊场景）
        String tokenParam = request.getParameter("token");
        if (tokenParam != null && !tokenParam.isEmpty()) {
            return tokenParam;
        }

        return null;
    }

    /**
     * 判断是否为不需要验证的路径
     */
    private boolean isExcludePath(String requestUri) {
        return EXCLUDE_PATHS.stream().anyMatch(requestUri::startsWith);
    }
}
