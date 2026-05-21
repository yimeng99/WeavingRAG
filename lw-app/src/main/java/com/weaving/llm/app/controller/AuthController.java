package com.weaving.llm.app.controller;

import com.weaving.llm.common.domain.LoginRequest;
import com.weaving.llm.common.domain.LoginResponse;
import com.weaving.llm.common.domain.R;
import com.weaving.llm.common.domain.User;
import com.weaving.llm.common.exception.BusinessException;
import com.weaving.llm.common.service.UserService;
import com.weaving.llm.common.store.TokenStore;
import com.weaving.llm.common.utils.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @Author: 依梦
 * @Date: 2025/10/27
 * @Description: 认证控制器
 */
@Slf4j
@Tag(name = "用户认证", description = "用户登录、注册、退出等认证相关接口")
@RestController
@RequestMapping("/v0/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private TokenStore tokenStore;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "支持用户名、手机号、邮箱三种登录方式")
    public R<LoginResponse> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "登录信息") @RequestBody LoginRequest request,
            HttpServletResponse response) {
        String account = request.getUsername();
        if (account == null || account.trim().isEmpty()) {
            throw new BusinessException("请输入用户名/手机号/邮箱");
        }
        
        User user = null;
        
        // 判断登录方式：手机号、邮箱或用户名
        if (account.matches("^1[3-9]\\d{9}$")) {
            // 手机号登录
            user = userService.getUserByPhone(account);
            if (user == null) {
                throw new BusinessException("手机号未注册");
            }
        } else if (account.contains("@")) {
            // 邮箱登录
            user = userService.getUserByEmail(account);
            if (user == null) {
                throw new BusinessException("邮箱未注册");
            }
        } else {
            // 用户名登录
            user = userService.getUserByUsername(account);
            if (user == null) {
                throw new BusinessException("用户名不存在");
            }
        }

        // 验证密码 (实际应该使用加密后的密码对比)
        String inputPassword = DigestUtils.md5DigestAsHex(request.getPassword().getBytes());
        if (!user.getPassword().equals(inputPassword)) {
            throw new BusinessException("密码错误");
        }

        // 生成 JWT Token
        String token = tokenStore.generateAndStoreToken(user.getId(), user.getUsername());

        // 设置 cookie
        ResponseCookie cookie = ResponseCookie.from("token", token)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(7 * 24 * 60 * 60) // 7天
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        log.info("用户登录成功：{}，JWT Token 已生成", user.getUsername());

        LoginResponse loginResponse = LoginResponse.builder()
                .token(token)
                .refreshToken(token)
                .user(user)
                .expiresIn(jwtUtil.getExpirationDateFromToken(token).getTime() - System.currentTimeMillis())
                .build();

        return R.ok(loginResponse);
    }

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "创建新用户账号")
    public R<User> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "注册信息") @RequestBody LoginRequest request) {
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new BusinessException("用户名不能为空");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new BusinessException("密码长度不能少于6位");
        }
        
        // 检查用户名是否已存在
        User existingUser = userService.getUserByUsername(request.getUsername());
        if (existingUser != null) {
            throw new BusinessException("用户名已存在");
        }

        // 创建新用户
        User user = User.builder()
                .username(request.getUsername().trim())
                .password(DigestUtils.md5DigestAsHex(request.getPassword().getBytes()))
                .email(request.getEmail())
                .avatar("https://api.dicebear.com/7.x/avataaars/svg?seed=" + request.getUsername())
                .build();

        userService.save(user);
        log.info("用户注册成功：{}", user.getUsername());

        return R.ok(user);
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    @Operation(summary = "退出登录", description = "移除 token，退出当前登录状态")
    public R<Void> logout(
            @Parameter(description = "认证 token", required = true) @RequestHeader(value = "Authorization", required = false) String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        // JWT 模式下，Token 在有效期内一直有效，这里只是记录日志
        if (token != null && !token.isEmpty()) {
            tokenStore.removeToken(token);
        }
        log.info("用户退出登录（JWT Token 将在过期后自动失效）");
        return R.ok();
    }

    /**
     * 刷新 Token
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新 Token", description = "刷新即将过期的 JWT Token")
    public R<Map<String, Object>> refreshToken(
            @Parameter(description = "认证 token", required = true) @RequestHeader(value = "Authorization", required = false) String token,
            HttpServletResponse response) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 验证原 Token 是否有效
        if (!tokenStore.isValidToken(token)) {
            throw new BusinessException("Token 已过期或无效，请重新登录");
        }

        // 从 Token 中获取用户信息
        Long userId = tokenStore.getUserIdByToken(token);
        String username = tokenStore.getUsernameByToken(token);

        if (userId == null || username == null) {
            throw new BusinessException("Token 无效");
        }

        // 生成新的 Token
        String newToken = tokenStore.generateAndStoreToken(userId, username);

        // 设置 cookie
        ResponseCookie cookie = ResponseCookie.from("token", newToken)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        Map<String, Object> result = new HashMap<>();
        result.put("token", newToken);
        result.put("expiresIn", jwtUtil.getExpirationDateFromToken(newToken).getTime() - System.currentTimeMillis());

        log.info("用户 {} 刷新 Token 成功", username);
        return R.ok(result);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息", description = "获取已登录用户的详细信息")
    public R<User> getCurrentUser(
            @Parameter(description = "认证 token", required = true) @RequestHeader(value = "Authorization", required = false) String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        // 从 JWT Token 中解析用户 ID
        Long userId = tokenStore.getUserIdByToken(token);
        if (userId == null) {
            throw new BusinessException("未登录或 token 已过期");
        }

        // 根据用户 ID 查询用户
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        return R.ok(user);
    }
}
