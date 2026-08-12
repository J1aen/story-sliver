-- ============================================================
-- 匿名故事碎片墙数据库初始化脚本
-- 干什么用：一次性创建全部表结构与索引（开发期可直接重建；上线后请去掉 DROP 语句再执行）
-- 为什么用 utf8mb4：完整支持中文与 emoji，避免生僻字/表情存不进去
-- 使用方法：mysql -uroot -p < init.sql
-- ============================================================
CREATE DATABASE IF NOT EXISTS story_sliver DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE story_sliver;

-- 开发期重建方便；上线前删除以下 DROP 语句，避免误删数据
DROP TABLE IF EXISTS fragment_like;
DROP TABLE IF EXISTS story_fragment;
DROP TABLE IF EXISTS system_config;
DROP TABLE IF EXISTS `user`;

-- 用户表：注册/登录账号
CREATE TABLE `user` (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    username   VARCHAR(32)     NOT NULL COMMENT '登录用户名（唯一）',
    nickname   VARCHAR(32)     NOT NULL COMMENT '展示昵称',
    password   VARCHAR(100)    NOT NULL COMMENT 'BCrypt 密码哈希，绝不存明文',
    email      VARCHAR(100)    DEFAULT NULL COMMENT '预留邮箱（暂不验证）',
    role       TINYINT         NOT NULL DEFAULT 0 COMMENT '角色：0普通 1管理员 2站长',
    status     TINYINT         NOT NULL DEFAULT 0 COMMENT '状态：0正常 1封禁（预留）',
    avatar     VARCHAR(255)    DEFAULT NULL COMMENT '当前头像URL（已审核通过）',
    avatar_pending VARCHAR(255) DEFAULT NULL COMMENT '待审核头像URL（审核通过后转正到 avatar）',
    avatar_reject_reason VARCHAR(255) DEFAULT NULL COMMENT '头像被拒绝原因（拒绝时写入，重新上传/通过后清空）',
    ban_expires_at DATETIME    DEFAULT NULL COMMENT '封禁到期时间（null 且 status=1 表示永久封禁）',
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='用户';

-- 故事碎片表：生命周期 0待审核 1已发布 2已隐藏
CREATE TABLE story_fragment (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id      BIGINT UNSIGNED NOT NULL COMMENT '真实发布者id（匿名只影响展示，数据库始终记录发布者，便于管理员追溯）',
    content      TEXT            NOT NULL COMMENT '碎片内容（1000字封顶）',
    like_count   INT             NOT NULL DEFAULT 0 COMMENT '点赞数（冗余计数）',
    is_anonymous TINYINT         NOT NULL DEFAULT 0 COMMENT '是否匿名：0显示昵称 1显示「匿名用户」',
    status       TINYINT         NOT NULL DEFAULT 0 COMMENT '状态：0待审核 1已发布 2已隐藏',
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_user (user_id),
    KEY idx_status_created (status, created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='故事碎片';

-- 点赞表：唯一约束保证每人每条只能赞一次
CREATE TABLE fragment_like (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    fragment_id BIGINT UNSIGNED NOT NULL COMMENT '被点赞的碎片id',
    user_id     BIGINT UNSIGNED NOT NULL COMMENT '点赞用户id',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_fragment_user (fragment_id, user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='点赞';

-- 系统配置表：存放站长可在线修改的配置
CREATE TABLE system_config (
    config_key   VARCHAR(50)  NOT NULL COMMENT '配置键（主键）',
    config_value VARCHAR(255) NOT NULL COMMENT '配置值（如管理员注册密码的BCrypt哈希）',
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (config_key)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='系统配置';
