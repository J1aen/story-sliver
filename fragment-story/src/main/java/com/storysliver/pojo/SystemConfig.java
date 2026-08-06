package com.storysliver.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 系统配置实体：对应 system_config 表。
 * 干什么用：存放站长可在线修改的配置，目前只有「管理员注册特殊密码」。
 * 为什么放数据库而不是配置文件：站长在管理页改完立即生效，不用改配置重启服务。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SystemConfig {
    /** 配置键常量：管理员注册特殊密码（存 BCrypt 哈希，不存明文） */
    public static final String KEY_ADMIN_REGISTER_CODE = "admin_register_code";

    private String configKey;//配置键（主键）
    private String configValue;//配置值（哈希后的管理员注册密码）
    private LocalDateTime updatedAt;//更新时间
}
