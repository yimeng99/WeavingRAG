package com.weaving.llm.common.core.domain;

import lombok.Data;

/**
 * @Author: 依梦
 * @Date: 2025/9/29
 * @Description: MetaDo
 */
@Data
public class MetaDo {

    private String title;

    private String icon;

    private String hidden;

    private boolean noCache;
}
