-- ============================================================
-- P2：失物招领 / 校园活动 / 表白墙
-- 在已有库上执行一次即可（ALTER 不是幂等的）
-- 执行前会建两张备份表：db_topic_bak_p2、db_topic_type_bak_p2
-- 新建库请直接跑 test.sql，不要跑本文件
-- ============================================================

CREATE TABLE IF NOT EXISTS db_topic_bak_p2 AS SELECT * FROM db_topic;
CREATE TABLE IF NOT EXISTS db_topic_type_bak_p2 AS SELECT * FROM db_topic_type;

-- 帖子扩展字段：失物招领的 item/place/status、活动的时间地点等
ALTER TABLE db_topic ADD COLUMN extra JSON NULL AFTER content;
ALTER TABLE db_topic ADD KEY idx_topic_type (type);

-- 类型元数据：稳定的 code 供前端路由/后端判断使用；anonymous 控制是否匿名展示
ALTER TABLE db_topic_type
    ADD COLUMN code VARCHAR(32) NULL AFTER id,
    ADD COLUMN anonymous TINYINT(1) NOT NULL DEFAULT 0 AFTER code;

UPDATE db_topic_type SET code = CASE id
    WHEN 1 THEN 'chat'
    WHEN 2 THEN 'friend'
    WHEN 3 THEN 'feedback'
    WHEN 4 THEN 'love'
    WHEN 5 THEN 'pitfall'
    WHEN 6 THEN 'daily'
    WHEN 7 THEN 'study'
    WHEN 8 THEN 'activity'
    WHEN 9 THEN 'qa'
    WHEN 10 THEN 'other'
    ELSE code END
WHERE code IS NULL;

-- 原来的「活动」就是校园活动，直接复用，避免出现两个活动分类
UPDATE db_topic_type
SET name = '校园活动', `desc` = '社团、讲座、比赛等校园活动信息'
WHERE id = 8;

INSERT INTO db_topic_type (code, name, `desc`, color, anonymous) VALUES
    ('lost', '失物招领', '丢了或捡到东西都可以发，找到后可标记「已找回」', '#e6a23c', 0),
    ('confession', '表白墙', '匿名表达心意，作者对其他人不可见', '#f56c6c', 1);

ALTER TABLE db_topic_type ADD UNIQUE KEY uk_topic_type_code (code);
