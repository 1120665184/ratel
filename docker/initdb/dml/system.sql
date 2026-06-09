-- =============================================
-- 系统模块初始化数据
-- =============================================

-- 用户初始化
INSERT INTO sys_user (id, username, nickname, avatar, email, phone, gender, status, last_login_time, last_login_ip, tenant_id, create_op, create_time, modify_op, modify_time, delete_op, delete_time, deleted) VALUES ('1', 'admin', '系统管理员', NULL, 'admin@gmail.com', '18333333333', 1, 1, NULL, NULL, NULL, NULL, '2026-04-13 06:20:03.758554', 'admin', '2026-04-28 09:51:35.695549', NULL, NULL, 0);

-- 初始化管理员用户（密码: admin123，使用 BCrypt 加密）
INSERT INTO sys_account (id, user_id, identity_type, identifier, credential, status, verified_time, bind_time, tenant_id, create_op, create_time, modify_op, modify_time, delete_op, delete_time, deleted, verified) VALUES ('1', '1', 'password', 'admin', '$2a$10$2QdERKkWEV15RxjMNRYGiuSowpPtdwPCnpEHqbhiD3Qrt4lyfwhpm', 1, NULL, '2026-04-13 06:20:03.77132', NULL, NULL, '2026-04-13 06:20:03.77132', 'admin', '2026-05-25 20:32:53.882114', NULL, NULL, 0, 1);

-- 部门初始化
INSERT INTO sys_dept (id, name, type, parent_id, enabled, sort, path, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('1', '总部', 1, NULL, 1, 1, '1', NULL, NULL, '2026-04-26 13:18:46.82807', NULL, NULL, 0, NULL, NULL);
INSERT INTO sys_dept (id, name, type, parent_id, enabled, sort, path, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2', '技术部', 2, '1', 1, 1, '1/2', NULL, NULL, '2026-04-26 13:18:46.82807', NULL, NULL, 0, NULL, NULL);
INSERT INTO sys_dept (id, name, type, parent_id, enabled, sort, path, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('3', '开发组', 3, '2', 1, 1, '1/2/3', NULL, NULL, '2026-04-26 13:18:46.82807', NULL, NULL, 0, NULL, NULL);
INSERT INTO sys_dept (id, name, type, parent_id, enabled, sort, path, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('4', '测试组', 3, '2', 1, 2, '1/2/4', NULL, NULL, '2026-04-26 13:18:46.82807', NULL, NULL, 0, NULL, NULL);
INSERT INTO sys_dept (id, name, type, parent_id, enabled, sort, path, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('5', '产品部', 2, '1', 1, 2, '1/5', NULL, NULL, '2026-04-26 13:18:46.82807', NULL, NULL, 0, NULL, NULL);
INSERT INTO sys_dept (id, name, type, parent_id, enabled, sort, path, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('6', '运营部', 2, '1', 1, 3, '1/6', NULL, NULL, '2026-04-26 13:18:46.82807', NULL, NULL, 0, NULL, NULL);
INSERT INTO sys_dept (id, name, type, parent_id, enabled, sort, path, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('7', '市场组', 3, '6', 1, 1, '1/6/7', NULL, NULL, '2026-04-26 13:18:46.82807', NULL, NULL, 0, NULL, NULL);
INSERT INTO sys_dept (id, name, type, parent_id, enabled, sort, path, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('8', '销售组', 3, '6', 1, 2, '1/6/8', NULL, NULL, '2026-04-26 13:18:46.82807', NULL, NULL, 0, NULL, NULL);
INSERT INTO sys_dept (id, name, type, parent_id, enabled, sort, path, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('9', '财务部', 2, '1', 1, 4, '1/9', NULL, NULL, '2026-04-26 13:18:46.82807', NULL, NULL, 0, NULL, NULL);
INSERT INTO sys_dept (id, name, type, parent_id, enabled, sort, path, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('10', '人力资源部', 2, '1', 1, 5, '1/10', NULL, NULL, '2026-04-26 13:18:46.82807', NULL, NULL, 0, NULL, NULL);

INSERT INTO sys_dept_parent (id, dept_id, parent_id, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2048683469779460096', '4', '5', NULL, 'admin', '2026-04-27 16:39:36.550057', 'admin', '2026-04-27 16:39:36.550057', 0, NULL, NULL);

-- 用户部门关联初始化
INSERT INTO sys_user_dept (id, user_id, dept_id, is_primary, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2050538173673480192', '1', '1', 1, NULL, 'admin', '2026-05-02 19:29:32.417689', 'admin', '2026-05-02 19:29:32.417689', 0, NULL, NULL);

