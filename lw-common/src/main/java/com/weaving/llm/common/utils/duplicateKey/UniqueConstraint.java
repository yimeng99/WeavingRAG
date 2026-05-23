package com.weaving.llm.common.utils.duplicateKey;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 唯一约束映射枚举
 * @Author: 35038
 */
@Getter
@RequiredArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public enum UniqueConstraint {
    UNIQ_USER_NAME("sys_user.uniq_user_name", "用户名已经被使用"),
    UNIQ_PHONE("sys_user.uniq_phone_number", "手机号已经被使用"),
    UNIQ_EMAIL("sys_user.uniq_email", "邮箱已经被使用");
    // ...其他约束

    private final String constraintName;
    private final String fieldDesc;


    // 构造器/getter

    public static String getFieldDescByConstraintName(String constraintName) {
        for (UniqueConstraint uc : values()) {
            if (uc.constraintName.equals(constraintName)) {
                return uc.fieldDesc;
            }
        }
        return "数据重复，请检查唯一字段";
    }
}
