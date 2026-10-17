-- ============================================================
-- P3：管理端基础字段
-- 在已有库上执行一次（ALTER 不是幂等的）
-- 执行前备份：db_topic_bak_p3、db_account_bak_p3
-- 新建库请直接跑 test.sql
-- ============================================================

CREATE TABLE IF NOT EXISTS db_topic_bak_p3 AS SELECT * FROM db_topic;
CREATE TABLE IF NOT EXISTS db_account_bak_p3 AS SELECT * FROM db_account;

-- 管理端：锁定（禁止评论/编辑）、屏蔽（普通用户不可见）
ALTER TABLE db_topic
    ADD COLUMN locked TINYINT(1) NOT NULL DEFAULT 0 AFTER top,
    ADD COLUMN invisible TINYINT(1) NOT NULL DEFAULT 0 AFTER locked;

-- 管理端：禁言、封禁
ALTER TABLE db_account
    ADD COLUMN mute TINYINT(1) NOT NULL DEFAULT 0 AFTER role,
    ADD COLUMN banned TINYINT(1) NOT NULL DEFAULT 0 AFTER mute;

-- 演示用：把 1 号账号设为管理员；登录后即可进入 /admin
UPDATE db_account SET role = 'admin' WHERE id = 1;
