-- p4：多级评论（只执行一次）
-- 当前库已经执行过；新库按顺序执行即可。
-- 1. 备份
CREATE TABLE IF NOT EXISTS db_topic_comment_bak_p4 AS SELECT * FROM db_topic_comment;

-- 2. 加 root_id：顶层评论的 root_id = 自身 id，回复继承父评论的 root_id
ALTER TABLE db_topic_comment
    ADD COLUMN root_id INT NULL AFTER quote,
    ADD INDEX idx_comment_tid_root (tid, root_id),
    ADD INDEX idx_comment_quote (quote);

-- 3. jwt-1.0 迁移过来的旧数据用 quote=-1 表示顶层评论，统一成 NULL
UPDATE db_topic_comment SET quote = NULL WHERE quote = -1;

-- 4. 顶层评论 root_id = 自身 id
UPDATE db_topic_comment SET root_id = id WHERE quote IS NULL;

-- 5. 父评论已经不存在（历史脏数据）的回复，提升为顶层评论，避免整条消失
UPDATE db_topic_comment c LEFT JOIN db_topic_comment p ON c.quote = p.id
SET c.quote = NULL, c.root_id = c.id
WHERE c.quote IS NOT NULL AND p.id IS NULL;

-- 6.1 递归回填 root_id（旧数据可能有多级）
UPDATE db_topic_comment c JOIN db_topic_comment p ON c.quote = p.id
SET c.root_id = p.root_id
WHERE c.quote IS NOT NULL AND (c.root_id IS NULL OR c.root_id <> p.root_id);
-- 6.2 递归回填 root_id（旧数据可能有多级）
UPDATE db_topic_comment c JOIN db_topic_comment p ON c.quote = p.id
SET c.root_id = p.root_id
WHERE c.quote IS NOT NULL AND (c.root_id IS NULL OR c.root_id <> p.root_id);
-- 6.3 递归回填 root_id（旧数据可能有多级）
UPDATE db_topic_comment c JOIN db_topic_comment p ON c.quote = p.id
SET c.root_id = p.root_id
WHERE c.quote IS NOT NULL AND (c.root_id IS NULL OR c.root_id <> p.root_id);
-- 6.4 递归回填 root_id（旧数据可能有多级）
UPDATE db_topic_comment c JOIN db_topic_comment p ON c.quote = p.id
SET c.root_id = p.root_id
WHERE c.quote IS NOT NULL AND (c.root_id IS NULL OR c.root_id <> p.root_id);
-- 6.5 递归回填 root_id（旧数据可能有多级）
UPDATE db_topic_comment c JOIN db_topic_comment p ON c.quote = p.id
SET c.root_id = p.root_id
WHERE c.quote IS NOT NULL AND (c.root_id IS NULL OR c.root_id <> p.root_id);
-- 6.6 递归回填 root_id（旧数据可能有多级）
UPDATE db_topic_comment c JOIN db_topic_comment p ON c.quote = p.id
SET c.root_id = p.root_id
WHERE c.quote IS NOT NULL AND (c.root_id IS NULL OR c.root_id <> p.root_id);
-- 6.7 递归回填 root_id（旧数据可能有多级）
UPDATE db_topic_comment c JOIN db_topic_comment p ON c.quote = p.id
SET c.root_id = p.root_id
WHERE c.quote IS NOT NULL AND (c.root_id IS NULL OR c.root_id <> p.root_id);
-- 6.8 递归回填 root_id（旧数据可能有多级）
UPDATE db_topic_comment c JOIN db_topic_comment p ON c.quote = p.id
SET c.root_id = p.root_id
WHERE c.quote IS NOT NULL AND (c.root_id IS NULL OR c.root_id <> p.root_id);
-- 6.9 递归回填 root_id（旧数据可能有多级）
UPDATE db_topic_comment c JOIN db_topic_comment p ON c.quote = p.id
SET c.root_id = p.root_id
WHERE c.quote IS NOT NULL AND (c.root_id IS NULL OR c.root_id <> p.root_id);
-- 6.10 递归回填 root_id（旧数据可能有多级）
UPDATE db_topic_comment c JOIN db_topic_comment p ON c.quote = p.id
SET c.root_id = p.root_id
WHERE c.quote IS NOT NULL AND (c.root_id IS NULL OR c.root_id <> p.root_id);

SELECT COUNT(*) AS null_root_count FROM db_topic_comment WHERE root_id IS NULL;
SELECT COUNT(*) AS bad_root_count FROM db_topic_comment c JOIN db_topic_comment p ON c.quote = p.id WHERE c.root_id <> p.root_id;
