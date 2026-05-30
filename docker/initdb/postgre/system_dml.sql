-- =============================================
-- 初始化数据
-- =============================================

-- 初始化管理员用户（密码: admin123，使用 BCrypt 加密）
INSERT INTO sys_user (id, username, nickname, gender, status, create_time)
VALUES ('1', 'admin', '系统管理员', 1, 1, CURRENT_TIMESTAMP);

-- 初始化管理员账号
INSERT INTO sys_account (id, user_id, identity_type, identifier, credential, status, verified, bind_time)
VALUES ('1', 1, 'password', 'admin', '$2a$10$2QdERKkWEV15RxjMNRYGiuSowpPtdwPCnpEHqbhiD3Qrt4lyfwhpm', 1, 1,
        CURRENT_TIMESTAMP);

-- 初始化部门测试数据
INSERT INTO sys_dept (id, name, type, parent_id, enabled, sort, path)
VALUES (1, '总公司', 1, NULL, 1, 1, '1'),
       (2, '北京分公司', 2, 1, 1, 1, '1/2'),
       (3, '技术部', 3, 2, 1, 1, '1/2/3'),
       (4, '前端开发组', 4, 3, 1, 1, '1/2/3/4'),
       (5, '后端开发组', 4, 3, 1, 2, '1/2/3/5'),
       (6, '测试组', 4, 3, 1, 3, '1/2/3/6'),
       (7, '产品部', 3, 2, 1, 2, '1/2/7'),
       (8, '设计组', 4, 7, 1, 1, '1/2/7/8'),
       (9, '市场部', 3, 2, 1, 3, '1/2/9'),
       (10, '销售部', 3, 2, 1, 4, '1/2/10'),
       (11, '上海分公司', 2, 1, 1, 2, '1/11'),
       (12, '华东区域技术部', 3, 11, 1, 1, '1/11/12'),
       (13, '广州分公司', 2, 1, 1, 3, '1/13'),
       (14, '华南区域销售部', 3, 13, 1, 1, '1/13/14');

-- 初始化用户部门关联
INSERT INTO sys_user_dept (user_id, dept_id, is_primary, sort)
VALUES (1, 1, 1, 1);