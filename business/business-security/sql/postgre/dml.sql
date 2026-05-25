-- =============================================
-- 菜单管理初始化数据
-- 数据库：PostgreSQL
-- =============================================

-- 初始化角色数据
INSERT INTO security_role (id, role_name, role_code, sort, description, status) VALUES
(1, '超级管理员', 'SUPER_ADMIN', 1, '拥有所有权限', 1);

