package com.weaving.llm.common.core.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: 35038
 */
// 统一响应对象
@Data
@NoArgsConstructor
@AllArgsConstructor
public class R<T> {
    private boolean success;
    private String message;
    private T data;

    public static <T> R<T> success(String message, T data) {
        return new R<>(true, message, data);
    }


    public static <T> R<T> error(String message) {
        return new R<>(false, message, null);
    }
}