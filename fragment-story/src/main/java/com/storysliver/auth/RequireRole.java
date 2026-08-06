package com.storysliver.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色校验注解：标在 Controller 方法上，由 AuthInterceptor 读取执行。
 * 例子：@RequireRole(User.ROLE_OWNER) 表示只有站长能访问。
 * 为什么用注解：把「需要什么角色」声明在方法上，比在方法里手写判断更清晰、可复用。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    /** 允许访问的角色数组，命中任意一个即可 */
    int[] value();
}
