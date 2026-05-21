package com.weaving.llm.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weaving.llm.common.domain.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Author: 依梦
 * @Date: 2025/10/27
 * @Description: 用户 Mapper 接口
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
