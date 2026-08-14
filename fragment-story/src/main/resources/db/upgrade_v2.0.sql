-- ============================================================
-- v2.0 增量脚本（Task 21 评论功能；在 v1.2 基础上执行，不删任何数据）
-- 干什么用：新增评论表 + 评论举报表
-- ============================================================

-- 1. 评论表：登录用户可评论（免审核，发布即显示）
CREATE TABLE IF NOT EXISTS comment (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    fragment_id BIGINT UNSIGNED NOT NULL COMMENT '所属碎片id',
    user_id     BIGINT UNSIGNED NOT NULL COMMENT '评论者id',
    content     VARCHAR(100)    NOT NULL COMMENT '评论内容（≤100字）',
    status      TINYINT         NOT NULL DEFAULT 0 COMMENT '0正常 1已下架',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_fragment (fragment_id, id)   -- 按碎片查评论的索引（列表正序分页用）
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='评论';

-- 2. 评论举报表：理由必填，同一人不能重复举报同一条
CREATE TABLE IF NOT EXISTS comment_report (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    comment_id  BIGINT UNSIGNED NOT NULL COMMENT '被举报评论id',
    reporter_id BIGINT UNSIGNED NOT NULL COMMENT '举报人id',
    reason      VARCHAR(255)    NOT NULL COMMENT '举报理由（必填）',
    status      TINYINT         NOT NULL DEFAULT 0 COMMENT '0待处理 1已处理',
    handled_at  DATETIME        DEFAULT NULL COMMENT '处理时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_comment_reporter (comment_id, reporter_id), -- 同一个人不能重复举报同一评论
    KEY idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='评论举报';
