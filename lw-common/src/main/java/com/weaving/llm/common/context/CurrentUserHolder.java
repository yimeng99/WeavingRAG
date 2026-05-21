package com.weaving.llm.common.context;

/**
 * 当前用户上下文持有者（基于 ThreadLocal）
 */
public class CurrentUserHolder {

    private static final ThreadLocal<CurrentUser> CURRENT_USER = new ThreadLocal<>();

    /**
     * 设置当前用户
     *
     * @param user 当前登录用户信息
     */
    public static void set(CurrentUser user) {
        CURRENT_USER.set(user);
    }

    /**
     * 获取当前用户
     *
     * @return 当前登录用户信息，未登录返回 null
     */
    public static CurrentUser get() {
        return CURRENT_USER.get();
    }

    /**
     * 获取当前用户 ID
     *
     * @return 当前用户 ID，未登录返回 null
     */
    public static Long getUserId() {
        CurrentUser user = CURRENT_USER.get();
        return user != null ? user.getUserId() : null;
    }

    /**
     * 获取当前用户名
     *
     * @return 当前用户名，未登录返回 null
     */
    public static String getUsername() {
        CurrentUser user = CURRENT_USER.get();
        return user != null ? user.getUsername() : null;
    }

    /**
     * 判断是否已登录
     *
     * @return true 已登录，false 未登录
     */
    public static boolean isLogin() {
        return CURRENT_USER.get() != null;
    }

    /**
     * 清除当前用户（请求结束时调用）
     */
    public static void clear() {
        CURRENT_USER.remove();
    }
}
