package com.weaving.llm.common.utils;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtils {

    // 定义加密的计算强度，默认为10
    private static final int LOG_ROUNDS = 10;

    public static String encode(CharSequence rawPassword) {
        // 该方法会自动生成盐并混入哈希结果中
        return BCrypt.hashpw(rawPassword.toString(), BCrypt.gensalt(LOG_ROUNDS));
    }

    public static boolean matches(CharSequence rawPassword, String encodedPassword) {
        // 该方法会从encodedPassword中提取盐，然后进行校验
        return BCrypt.checkpw(rawPassword.toString(), encodedPassword);
    }
}
