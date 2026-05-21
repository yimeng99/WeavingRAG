package com.weaving.llm.common.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @Author: 依梦
 * @Date: 2025/10/27
 * @Description: 用户实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user")
public class User {
    
    /**
     * 用户 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 姓名 (显示名称) - 非数据库字段，使用 username 作为显示名称
     */
    @TableField(exist = false)
    private String name;
    
    /**
     * 密码 (加密存储)
     */
    private String password;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 手机
     */
    private String phone;
    
    /**
     * 角色 (admin:管理员，editor:编辑者，reviewer:审核者，reader:只读者)
     */
    private String role;
    
    /**
     * 部门
     */
    private String department;
    
    /**
     * 状态 (online:在线，offline:离线，pending:待激活)
     */
    private String status;
    
    /**
     * 头像 URL
     */
    private String avatar;
    
    /**
     * 上次登录时间
     */
    private LocalDateTime lastLogin;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    /**
     * 逻辑删除标识 (0:未删除，1:已删除)
     */
    @TableLogic
    private Integer deleted;
}
