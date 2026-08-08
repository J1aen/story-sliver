package com.storysliver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 通用 Bean 配置（Task 5）。
 * 干什么用：声明 Spring 管理的通用对象，目前只有一个：密码加密器 PasswordEncoder。
 * 为什么用 @Configuration + @Bean 而不是直接 @Component：
 *   BCryptPasswordEncoder 是 Spring Security 库里的第三方类，我们改不了它的源码、
 *   没法给它加 @Component，所以写一个 @Bean 方法替它「办身份」，交给 Spring 容器统一管理。
 */
@Configuration
public class BeanConfig {

    /**
     * 密码加密器 Bean：全项目唯一的 BCrypt 实现。
     * @return PasswordEncoder 接口实例（实际是 BCryptPasswordEncoder）
     * 为什么方法名叫 passwordEncoder：方法名就是这个 Bean 的名字，
     * 谁 @Autowired PasswordEncoder，Spring 就把这个方法返回的对象注入给他。
     * 为什么返回接口 PasswordEncoder 而不是实现类 BCryptPasswordEncoder：
     * 面向接口编程——以后想换加密算法，只改这一处实现，所有调用方代码不用动。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt 自带随机盐值，是目前最常用的密码安全存储方案
        return new BCryptPasswordEncoder();
    }
}
