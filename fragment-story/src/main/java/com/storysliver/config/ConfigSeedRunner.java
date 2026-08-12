package com.storysliver.config;

import com.storysliver.mapper.SystemConfigMapper;
import com.storysliver.pojo.SystemConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 配置种子数据：应用启动时把默认「管理员注册特殊密码」写入 system_config 表。
 * 干什么用：注册页勾选「注册为管理员」时，后端要拿 system_config 里的哈希来校验，
 * 所以启动时必须保证这个配置存在。
 * 为什么用 CommandLineRunner 而不是写死在 SQL：SQL 里写死 BCrypt 哈希不好维护，
 * 启动时从 application.properties 读取明文并加密落库，更清晰。
 * 为什么只在该键不存在时写入：站长改过的密码不会被每次启动重置覆盖。
 */
@Component
public class ConfigSeedRunner implements CommandLineRunner {

    @Autowired
    private SystemConfigMapper systemConfigMapper;//系统配置表操作

    @Autowired
    private PasswordEncoder passwordEncoder;//BCrypt 加密器

    /** 默认管理员注册密码：来自 application.properties 的 app.admin.default-register-code */
    @Value("${app.admin.default-register-code}")
    private String defaultRegisterCode;

    @Override
    public void run(String... args) {
        // 已有配置（站长改过）就跳过，避免每次启动把密码重置回默认值
        if (systemConfigMapper.selectByKey(SystemConfig.KEY_ADMIN_REGISTER_CODE) == null) {
            systemConfigMapper.upsert(
                    SystemConfig.KEY_ADMIN_REGISTER_CODE,
                    passwordEncoder.encode(defaultRegisterCode));
        }
    }
}
