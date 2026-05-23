package com.weaving.llm.common.utils.duplicateKey;

import org.springframework.dao.DuplicateKeyException;

/**
 * @Author: 35038
 */
public class DuplicateKeyHelper {
    /**
     * 从 DuplicateKeyException 中提取约束名
     * 消息格式示例: "Duplicate entry '测试' for key 'sys_user.uniq_user_name'"
     * @return 约束名，例如 "sys_user.uniq_user_name"
     */
    public static String extractConstraintName(DuplicateKeyException e) {
        String message = e.getMostSpecificCause().getMessage();
        // 匹配 for key '...'
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("for key '([^']+)'");
        java.util.regex.Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
