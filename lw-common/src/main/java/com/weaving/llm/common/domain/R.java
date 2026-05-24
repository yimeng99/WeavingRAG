package com.weaving.llm.common.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class R<T> implements Serializable {

    private Integer code;
    private String msg;
    private T data;

    public R() {
    }

    public R(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> R<T> ok() {
        return new R<>(200, "success", null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(200, "success", data);
    }

    public static <T> R<T> ok(String msg) {
        return new R<>(500, "fail", null);
    }

    public static <T> R<T> ok(String msg, T data) {
        return new R<>(200, msg, data);
    }

    public static <T> R<T> fail() {
        return new R<>(500, "fail", null);
    }

    public static <T> R<T> fail(String msg) {
        return new R<>(500, msg, null);
    }

    public static <T> R<T> fail(Integer code, String msg) {
        return new R<>(code, msg, null);
    }

    public static <T> R<T> fail(Integer code, String msg, T data) {
        return new R<>(code, msg, data);
    }
}