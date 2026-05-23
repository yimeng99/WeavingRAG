package com.weaving.llm.common.pages;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.weaving.llm.common.core.Convert;
import com.weaving.llm.common.core.domain.AjaxResult;
import com.weaving.llm.common.utils.ServletUtils;

import java.util.List;

/**
 * @Author: 依梦
 * @Date: 2025/9/20
 * @Description: PageUtils
 */
public class PageUtils {
    public static final String CUR_PAGE = "currentPage";

    public static final String PAGE_NUM = "pageNum";

    public static final String PAGE_SIZE = "pageSize";

    public static final String ORDER_BY_COLUMN = "orderByColumn";

    /**
     * 排序的方向 "desc" 或者 "asc".
     */
    public static final String IS_ASC = "isAsc";

    /**
     * 分页参数合理化
     */
    public static final String REASONABLE = "reasonable";

    /**
     * 封装分页对象
     */
    public static PageEntity getPageEntity() {
        PageEntity pageDomain = new PageEntity();
        pageDomain.setPageNum(Convert.toInt(ServletUtils.getParameter(PAGE_NUM), 1));
        pageDomain.setPageSize(Convert.toInt(ServletUtils.getParameter(PAGE_SIZE), 10));
/*        pageDomain.setOrderByColumn(ServletUtils.getParameter(ORDER_BY_COLUMN));
        pageDomain.setIsAsc(ServletUtils.getParameter(IS_ASC));
        pageDomain.setReasonable(ServletUtils.getParameterToBool(REASONABLE));*/
        return pageDomain;
    }

    public static PageEntity buildPageRequest() {
        PageEntity pageDomain = new PageEntity();
        pageDomain.setPageNum(Convert.toInt(ServletUtils.getParameter(PAGE_NUM), 1));
        pageDomain.setCurrentPage(Convert.toInt(ServletUtils.getParameter(CUR_PAGE), 1));
        pageDomain.setPageSize(Convert.toInt(ServletUtils.getParameter(PAGE_SIZE), 10));
        return pageDomain;
    }

    /**
     * 设置请求分页数据
     */
    public static void startPage() {
        PageEntity page = PageUtils.buildPageRequest();
        int pageNum = page.getPageNum();
        int pageSize = page.getPageSize();
        String orderBy = ""; // SqlUtil.escapeOrderBySql(page.getOrderBy());
//        Boolean reasonable = pageDomain.getReasonable();
        PageHelper.startPage(pageNum, pageSize, orderBy); // .setReasonable(reasonable);
    }

    /**
     * 清理分页的线程变量
     */
    public static void clearPage() {
        PageHelper.clearPage();
    }


    public static PageDataResult generatePageDataResult(List<?> list) {
        PageDataResult pageDataResult = new PageDataResult();
        pageDataResult.setCode(AjaxResult.SUCCESS);
        pageDataResult.setMsg("查询成功");
        PageEntity pageEntity = PageUtils.buildPageRequest();
        PageInfo<?> pageInfo = new PageInfo<>(list);
        pageEntity.setTotal((int) pageInfo.getTotal());
        pageEntity.setPages(pageInfo.getPages());
        pageEntity.setHasMore(pageInfo.isHasNextPage());
        pageEntity.setCurrentPage(pageInfo.getPageNum());
        pageDataResult.setPageResult(pageEntity);
        pageDataResult.setPageData(list);
        return pageDataResult;
    }
}
