-- =============================================
-- 菜单管理初始化数据
-- 数据库：PostgreSQL
-- =============================================

-- 初始化角色数据
INSERT INTO security_role (id, role_name, role_code, sort, description, status) VALUES
(1, '超级管理员', 'SUPER_ADMIN', 1, '拥有所有权限', 1);
INSERT INTO security_role_subject ("id", "subject_id", "role_id", "tenant_id", "create_op", "create_time", "modify_op", "modify_time", "deleted", "delete_op", "delete_time") VALUES ('1', '1', '1', NULL, NULL, '2026-05-01 01:16:11.414947', NULL, NULL, 0, NULL, NULL);
-- 初始化权限信息
INSERT INTO security_abac ("id", "tenant_id", "expression", "description", "status", "create_op", "create_time", "modify_op", "modify_time", "delete_op", "delete_time", "deleted") VALUES ('1', 'default', 'contains(r.sub.roles , ''super_admin'')', '超级管理员角色表达式', 1, NULL, '2026-04-04 06:30:06.683643', NULL, '2026-04-04 06:30:06.683643', NULL, NULL, 0);
INSERT INTO security_abac_permission ("id", "tenant_id", "abac_id", "resource_type", "action", "url_pattern", "effect", "status", "create_op", "create_time", "modify_op", "modify_time", "delete_op", "delete_time", "deleted") VALUES ('1', 'default', '1', '*', '*', '*', 'allow', 1, NULL, '2026-05-02 11:17:56.593787', NULL, '2026-05-02 11:17:56.593787', NULL, NULL, 0);
