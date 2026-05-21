package com.weaving.llm.common.utils;

import com.weaving.llm.common.context.CurrentUser;
import com.weaving.llm.common.context.CurrentUserHolder;

/**
 * 当前登录用户工具类
 * 提供便捷方法获取当前登录用户的基本信息
 */
public class CurrentUserUtil {

    /**
     * 获取当前登录用户完整信息
     *
     * @return CurrentUser 对象，未登录返回 null
     */
    public static CurrentUser getCurrentUser() {
        return CurrentUserHolder.get();
    }

    /**
     * 获取当前登录用户 ID
     *
     * @return 用户 ID，未登录返回 null
     */
    public static Long getUserId() {
        return CurrentUserHolder.getUserId();
    }

    /**
     * 获取当前登录用户名
     *
     * @return 用户名，未登录返回 null
     */
    public static String getUsername() {
        return CurrentUserHolder.getUsername();
    }

    /**
     * 获取当前登录用户昵称
     *
     * @return 昵称，未登录返回 null
     */
    public static String getNickname() {
        CurrentUser user = CurrentUserHolder.get();
        return user != null ? user.getNickname() : null;
    }

    /**
     * 获取当前登录用户头像
     *
     * @return 头像 URL，未登录返回 null
     */
    public static String getAvatar() {
        CurrentUser user = CurrentUserHolder.get();
        return user != null ? user.getAvatar() : null;
    }

    /**
     * 获取当前登录用户邮箱
     *
     * @return 邮箱，未登录返回 null
     */
    public static String getEmail() {
        CurrentUser user = CurrentUserHolder.get();
        return user != null ? user.getEmail() : null;
    }

    /**
     * 获取当前登录用户手机号
     *
     * @return 手机号，未登录返回 null
     */
    public static String getPhone() {
        CurrentUser user = CurrentUserHolder.get();
        return user != null ? user.getPhone() : null;
    }

    /**
     * 获取当前登录用户角色
     *
     * @return 角色，未登录返回 null
     */
    public static String getRole() {
        CurrentUser user = CurrentUserHolder.get();
        return user != null ? user.getRole() : null;
    }

    /**
     * 获取当前登录用户 Token
     *
     * @return Token，未登录返回 null
     */
    public static String getToken() {
        CurrentUser user = CurrentUserHolder.get();
        return user != null ? user.getToken() : null;
    }

    /**
     * 检查当前用户是否已登录
     *
     * @return true 已登录，false 未登录
     */
    public static boolean isLogin() {
        return CurrentUserHolder.isLogin();
    }

    /**
     * 检查当前用户是否未登录
     *
     * @return true 未登录，false 已登录
     */
    public static boolean isNotLogin() {
        return !CurrentUserHolder.isLogin();
    }

    /**
     * 要求用户必须登录，返回用户 ID
     * 未登录时抛出异常
     *
     * @return 用户 ID
     * @throws IllegalStateException 未登录时抛出
     */
    public static Long requireUserId() {
        Long userId = getUserId();
        if (userId == null) {
            throw new IllegalStateException("用户未登录");
        }
        return userId;
    }

    /**
     * 要求用户必须登录，返回用户完整信息
     * 未登录时抛出异常
     *
     * @return CurrentUser 对象
     * @throws IllegalStateException 未登录时抛出
     */
    public static CurrentUser requireCurrentUser() {
        CurrentUser user = getCurrentUser();
        if (user == null) {
            throw new IllegalStateException("用户未登录");
        }
        return user;
    }
}
