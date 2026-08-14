-- ============================================================
-- v1.2 增量脚本（在 init.sql 基础上执行，不删任何数据）
-- 干什么用：昵称唯一索引 + 敏感词表 + 公告表
-- 为什么不用 init.sql：init.sql 开头有 DROP TABLE，线上执行会清库，增量脚本只加不改不删
-- ============================================================

-- 1. 昵称唯一（执行前先确认无重复昵称：
--    SELECT nickname FROM `user` GROUP BY nickname HAVING COUNT(*)>1;）
ALTER TABLE `user` ADD UNIQUE KEY uk_nickname (nickname);

-- 2. 敏感词表：注册/修改昵称时校验；站长可在管理后台增删
CREATE TABLE IF NOT EXISTS sensitive_word (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    word       VARCHAR(50)     NOT NULL COMMENT '敏感词（唯一，校验用包含匹配）',
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_word (word)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='敏感词库';

-- 3. 公告表：站长在线编辑，status 控制上架/下架
CREATE TABLE IF NOT EXISTS announcement (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    title      VARCHAR(50)     NOT NULL COMMENT '公告标题',
    content    VARCHAR(2000)   NOT NULL COMMENT '公告正文',
    image_url  VARCHAR(255)    DEFAULT NULL COMMENT '公告图片URL（可空）',
    status     TINYINT         NOT NULL DEFAULT 0 COMMENT '0下架 1上架',
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='公告';

-- 4. 默认敏感词（站长可增删；INSERT IGNORE 避免重复）
INSERT IGNORE INTO sensitive_word (word) VALUES
('傻逼'), ('你妈'), ('去死'), ('垃圾广告'), ('加我微信'),
('代开发票'), ('博彩'), ('赌博'), ('诈骗'), ('色情');
