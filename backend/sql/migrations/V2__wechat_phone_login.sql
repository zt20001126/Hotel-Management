-- Existing databases: run this migration once before starting the updated backend.
-- The current seed data has distinct phone numbers. Resolve any duplicate phone values first.
ALTER TABLE `sys_guest`
    MODIFY COLUMN `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '兼容旧账号的用户名',
    MODIFY COLUMN `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '兼容旧账号的密码',
    ADD COLUMN `openid` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '微信小程序用户标识' AFTER `id`,
    DROP INDEX `idx_phone`,
    ADD UNIQUE INDEX `uk_sys_guest_openid` (`openid`),
    ADD UNIQUE INDEX `uk_sys_guest_phone` (`phone`);
