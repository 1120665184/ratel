-- =============================================
-- 菜单管理初始化数据
-- 数据库：PostgreSQL
-- =============================================

-- 初始化角色数据
INSERT INTO security_role (id, role_name, role_code, sort, description, status) VALUES
(1, '超级管理员', 'SUPER_ADMIN', 1, '拥有所有权限', 1);

-- 初始化菜单数据
-- 一级目录
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status) VALUES
(1, NULL, '系统管理', 1, 1, 'setting', '/sub-system', 1, 1),
(2, NULL, '安全中心', 1, 2, 'security', '/sub-security', 1, 1);

-- 二级菜单
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, path, micro_app, visible, status) VALUES
(3, 1, '仪表盘', 2, 1, '/sub-system/dashboard', 'gwsu-sub-system', 1, 1),
(4, 1, '用户管理', 2, 2, '/sub-system/user', 'gwsu-sub-system', 1, 1),
(22, 1, '部门管理', 2, 3, '/sub-system/dept', 'gwsu-sub-system', 1, 1),
(5, 2, '角色管理', 2, 1, '/sub-security/role', 'gwsu-sub-security', 1, 1),
(6, 2, '菜单管理', 2, 2, '/sub-security/menu', 'gwsu-sub-security', 1, 1);

-- 部门管理按钮权限
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, permission, visible, status) VALUES
(23, 22, '新增部门', 3, 1, 'system:dept:add', 1, 1),
(24, 22, '编辑部门', 3, 2, 'system:dept:edit', 1, 1),
(25, 22, '删除部门', 3, 3, 'system:dept:delete', 1, 1),
(26, 22, '分配部门', 3, 4, 'system:dept:assign', 1, 1);

-- 超级管理员添加部门管理权限
INSERT INTO security_role_menu (role_id, menu_id) VALUES
(1, 22), (1, 23), (1, 24), (1, 25), (1, 26);
