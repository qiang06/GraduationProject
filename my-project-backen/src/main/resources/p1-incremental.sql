-- P1 增量脚本：消息、点赞、收藏
-- 仅创建缺失对象，不删除或清空现有数据。适用于 MySQL 8。

CREATE TABLE IF NOT EXISTS db_topic_interact_like (
    tid INT NOT NULL,
    uid INT NOT NULL,
    time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY tid_uid_like (tid, uid),
    KEY idx_like_tid (tid),
    KEY idx_like_uid (uid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS db_topic_interact_collect (
    tid INT NOT NULL,
    uid INT NOT NULL,
    time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY tid_uid_collect (tid, uid),
    KEY idx_collect_tid (tid),
    KEY idx_collect_uid (uid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS db_notification (
    id INT NOT NULL AUTO_INCREMENT,
    uid INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    content VARCHAR(500) NULL,
    type VARCHAR(32) NOT NULL,
    url VARCHAR(255) NULL,
    time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_read TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_notification_uid_read_time (uid, is_read, time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 如果 db_notification 是旧表且没有 is_read，请只执行下面这一行一次：
-- ALTER TABLE db_notification ADD COLUMN is_read TINYINT(1) NOT NULL DEFAULT 0 AFTER time;
