package com.weaving.llm.common.core.domain;


import com.weaving.llm.common.utils.StringUtils;

import java.util.HashMap;

/**
 * @Author: 依梦
 * @Date: 2025/9/21
 * @Description: AjaxResult
 */
public class AjaxResult extends HashMap<String, Object> {

    private static final long serialVersionUID = 1L;

    public static final int SUCCESS = 200;
    public static final int ERROR = 500;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int BAD_REQUEST = 400;
    public static final int NOT_ACCEPTABLE = 406;
    public static final int CONFLICT = 409;
    public static final int UNSUPPORTED_MEDIA_TYPE = 415;
    public static final int INTERNAL_SERVER_ERROR = 500;


    public static final String SUCCESS_MESSAGE = "操作成功";
    public static final String ERROR_MESSAGE = "操作失败";
    public static final String UNAUTHORIZED_MESSAGE = "未授权";
    public static final String FORBIDDEN_MESSAGE = "无权限";
    /**
     * 状态码
     */
    public static final String CODE_TAG = "code";

    /**
     * 返回内容
     */
    public static final String MSG_TAG = "msg";

    /**
     * 数据对象
     */
    public static final String DATA_TAG = "data";

    private int code;
    private String msg;
    private Object data;

    /**
     * 初始化一个新创建的 AjaxResult 对象
     *
     * @param code 状态码
     * @param msg  返回内容
     * @param data 数据对象
     */
    public AjaxResult(int code, String msg, Object data) {
        super.put(CODE_TAG, code);
        super.put(MSG_TAG, msg);
        if (StringUtils.isNotNull(data)) {
            super.put(DATA_TAG, data);
        }
    }

    public AjaxResult(int code, String msg) {
        super.put(CODE_TAG, code);
        super.put(MSG_TAG, msg);
    }

    public static AjaxResult success() {
        return new AjaxResult(200, "操作成功", null);
    }

    public static AjaxResult success(String message) {
        return new AjaxResult(200, message, null);
    }

    public static AjaxResult success(Object data) {
        return new AjaxResult(200, "操作成功", data);
    }

    public static AjaxResult error(String msg) {
        return new AjaxResult(500, msg, null);
    }

    public static AjaxResult error(String msg, Integer code) {
        return new AjaxResult(code, msg, null);
    }


    public static AjaxResult result(boolean result, String successMsg, String errorMsg) {
        if (result) {
            return new AjaxResult(SUCCESS, successMsg);
        } else {
            return new AjaxResult(ERROR, errorMsg);
        }
    }

    @SuppressWarnings("unchecked")
    public AjaxResult putData(String key, Object value) {
        Object data = this.get(DATA_TAG);
        HashMap<String, Object> map;

        if (StringUtils.isNull(data)) {
            map = new HashMap<String, Object>();
        } else if (data instanceof HashMap) {
            map = (HashMap<String, Object>) data;
        } else {
            // 处理数据不是HashMap的情况
            map = new HashMap<String, Object>();
            // 如果原数据是Map类型，可以尝试转换
            if (data instanceof java.util.Map) {
                map.putAll((java.util.Map<? extends String, ?>) data);
            }
        }
        map.put(key, value);
        super.put(DATA_TAG, map);
        return this;
    }
}
