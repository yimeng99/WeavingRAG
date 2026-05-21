-- 用户表新增字段
ALTER TABLE `user` 
ADD COLUMN `name` varchar(100) DEFAULT NULL COMMENT '姓名 (显示名称)' AFTER `username`,
ADD COLUMN `role` varchar(50) DEFAULT 'reader' COMMENT '角色 (admin:管理员，editor:编辑者，reviewer:审核者，reader:只读者)' AFTER `phone`,
ADD COLUMN `department` varchar(100) DEFAULT NULL COMMENT '部门' AFTER `role`,
ADD COLUMN `status` varchar(50) DEFAULT 'offline' COMMENT '状态 (online:在线，offline:离线，pending:待激活)' AFTER `department`,
ADD COLUMN `last_login` datetime DEFAULT NULL COMMENT '上次登录时间' AFTER `avatar`;

-- 初始化默认数据（如果表中没有数据）
INSERT INTO `user` (`username`, `name`, `password`, `email`, `phone`, `role`, `department`, `status`, `avatar`, `create_time`) VALUES
('admin', '张明远', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iDMqsHqKQ8bJHvPzQx6qP3hKkPvC', 'ming.zhang@zhiku.com', '13800138000', 'admin', '技术部', 'online', NULL, NOW()),
('liming', '李明轩', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iDMqsHqKQ8bJHvPzQx6qP3hKkPvC', 'li.ming@zhiku.com', '13800138001', 'editor', '前端开发组', 'offline', NULL, NOW()),
('chen', '陈思琪', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iDMqsHqKQ8bJHvPzQx6qP3hKkPvC', 'siqi.chen@zhiku.com', '13800138002', 'editor', '前端架构组', 'online', NULL, NOW()),
('wang', '王芳', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iDMqsHqKQ8bJHvPzQx6qP3hKkPvC', 'fang.wang@zhiku.com', '13800138003', 'reviewer', '内容运营组', 'offline', NULL, NOW()),
('liu', '刘子轩', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iDMqsHqKQ8bJHvPzQx6qP3hKkPvC', 'zixuan.liu@zhiku.com', '13800138004', 'reader', '数据科学组', 'offline', NULL, NOW()),
('zhao', '赵一鸣', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iDMqsHqKQ8bJHvPzQx6qP3hKkPvC', 'yiming.zhao@zhiku.com', '13800138005', 'reader', '后端开发组', 'pending', NULL, NOW());
