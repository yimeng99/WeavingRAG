package com.weaving.llm.common.pages;


import com.weaving.llm.common.utils.StringUtils;
import lombok.Data;


/**
 * @Author: 依梦
 * @Date: 2025/9/20
 * @Description: PageEntity
 */
@Data
public class PageEntity {
    private int pageNum;
    private int currentPage;
    private int pageSize;
    private int total;
    private int pages;
    private boolean hasMore;
    /** 排序的方向desc或者asc */
    private String isAsc = "asc";
    /** 排序列 */
    private String orderByColumn;

    public String getOrderBy()
    {
        if (StringUtils.isEmpty(orderByColumn))
        {
            return "";
        }
        return StringUtils.toUnderScoreCase(orderByColumn) + " " + isAsc;
    }
}
