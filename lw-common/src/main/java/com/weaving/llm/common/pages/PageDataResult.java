package com.weaving.llm.common.pages;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @Author: 依梦
 * @Date: 2025/9/20
 * @Description: PageDataResult
 */
@Data
public class PageDataResult  implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer code;
    private String msg;

    private List<?> pageData;
    private PageEntity pageResult;
}
