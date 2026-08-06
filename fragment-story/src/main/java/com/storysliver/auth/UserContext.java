package com.storysliver.auth;

/**
 * 当前登录用户上下文。
 * 干什么用：拦截器解析 token 后把 userId/role 放进 ThreadLocal，Controller/Service 直接读取。
 * 为什么用 ThreadLocal：每个请求在独立的线程里执行，天然隔离；
 * 为什么请求结束必须 clear：Tomcat 会复用线程，不清会串号、泄漏内存。
 */
public final class UserContext {
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<Integer> ROLE = new ThreadLocal<>();

    /** 请求开始时写入当前用户 */
    public static void set(Long userId, Integer role) {
        USER_ID.set(userId);
        ROLE.set(role);
    }

    /** 读取当前用户 id；未登录时为 null */
    public static Long getUserId() {
        return USER_ID.get();
    }

    /** 读取当前用户角色 */
    public static Integer getRole() {
        return ROLE.get();
    }

    /** 请求结束时清理，防止线程复用导致串号 */
    public static void clear() {
        USER_ID.remove();
        ROLE.remove();
    }
}
