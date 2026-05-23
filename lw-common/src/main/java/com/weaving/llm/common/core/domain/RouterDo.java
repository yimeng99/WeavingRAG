package com.weaving.llm.common.core.domain;

import lombok.Data;

import java.util.List;

/**
 * @Author: 依梦
 * @Date: 2025/9/29
 * @Description: RouterDo
 */
@Data
public class RouterDo {

    /**
     * 路由参数
     */
    private Long id;

    /**
     * 路由名称
     */
    private String name;

    /**
     * 路由地址
     */
    private String path;

    /**
     * 组件路径
     */
    private String component;

    /**
     * 菜单是否展示
     */
    private String hidden;

    /**
     * 菜单元数据
     */
    private MetaDo meta;

    /**
     * 子菜单
     */
    private List<RouterDo> children;
}
