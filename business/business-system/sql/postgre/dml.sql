-- =============================================
-- 初始化数据
-- =============================================

-- 初始化管理员用户（密码: admin123，使用 BCrypt 加密）
INSERT INTO sys_user (id, username, nickname, gender, status, create_time)
VALUES ('1', 'admin', '系统管理员', 1, 1, CURRENT_TIMESTAMP);

-- 初始化管理员账号
INSERT INTO sys_account (id, user_id, identity_type, identifier, credential, status, verified, bind_time)
VALUES ('1', 1, 'password', 'admin', '$2a$10$YRRHnxMXy/qYoDcc8QuKV.K2E.4AZMFiZN9rqqr41CQMvt8YVFAd.', 1, TRUE, CURRENT_TIMESTAMP);
