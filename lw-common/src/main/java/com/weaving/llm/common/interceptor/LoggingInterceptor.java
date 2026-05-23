package com.weaving.llm.common.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 请求日志拦截器
 * 打印请求地址和参数
 */
@Slf4j
@Component
public class LoggingInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 记录开始时间
        request.setAttribute("startTime", System.currentTimeMillis());

        // 打印请求方法、地址、参数
        String method = request.getMethod();
        String uri = request.getRequestURI();
        Map<String, String> params = getParams(request);

        log.info("[Request] {} {} | params: {}", method, uri, params);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        int status = response.getStatus();
        String uri = request.getRequestURI();
        long elapsed = System.currentTimeMillis() - (long) request.getAttribute("startTime");

        if (ex != null) {
            log.error("[Response] {} | status: {} | elapsed: {}ms | error: {}", uri, status, elapsed, ex.getMessage());
        } else {
            log.info("[Response] {} | status: {} | elapsed: {}ms", uri, status, elapsed);
        }
    }

    /**
     * 获取请求参数
     */
    private Map<String, String> getParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String name = paramNames.nextElement();
            params.put(name, request.getParameter(name));
        }
        return params;
    }
}