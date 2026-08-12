package com.storysliver.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storysliver.mapper.UserMapper;
import com.storysliver.pojo.User;
import com.storysliver.pojo.Result;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 认证拦截器：处理所有 /api/** 请求（公开接口在 WebConfig 排除）。
 * 干什么用：解析并校验 JWT → 写入 UserContext → 检查方法上的 @RequireRole 角色要求。
 * 为什么这样设计：登录校验集中在这里做，Controller 不用重复写「取 token、验 token」的代码。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {
    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserMapper userMapper;//校验账号是否被封禁

    /** 序列化 Result 用；一次创建、处处复用（线程安全） */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 请求进入 Controller 前执行。
     * 返回 true 放行，返回 false 拦截（已写好响应）。
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // 首页列表是公开接口（仅 GET /api/fragments），游客可看，无需登录；
        // 若带了 token，Controller 的 optionalUserId 会解析并标记「已赞」。
        // 为什么在这里判断而不是在 WebConfig 排除：排除是按路径的，会把 POST 发布也放行，
        // 导致发布时拿不到登录用户（之前就是这里出的空指针 bug）。
        if ("GET".equals(request.getMethod()) && "/api/fragments".equals(request.getRequestURI())) {
            return true;
        }

        // 约定：token 放在请求头 Authorization: Bearer <token> 里
        String auth = request.getHeader("Authorization");
        String token = (auth != null && auth.startsWith("Bearer ")) ? auth.substring(7) : null;

        Claims claims;
        try {
            // 解析失败（缺失/过期/篡改）统一按未登录处理，不区分细节，避免给攻击者泄露原因
            claims = token == null ? null : jwtUtil.parse(token);
        } catch (Exception e) {
            claims = null;
        }
        if (claims == null) {
            writeJson(response, 401, Result.error(401, "请先登录"));
            return false;
        }

        // 从 token 里取出身份：subject 是 userId，role 是自定义声明
        Long userId = Long.valueOf(claims.getSubject());
        Integer role = claims.get("role", Integer.class);
        UserContext.set(userId, role);

        // 封禁账号：即使持有旧 token 也拒绝所有登录态请求（防封禁后继续使用）
        User user = userMapper.selectById(userId);
        if (user != null && user.getStatus() == User.STATUS_BANNED) {
            // 已到期的封禁自动解封，否则拒绝所有登录态请求
            if (user.getBanExpiresAt() != null && !user.getBanExpiresAt().isAfter(LocalDateTime.now())) {
                userMapper.unban(user.getId());
            } else {
                UserContext.clear();
                String reason = user.getBanReason();
                String text = (reason == null || reason.isBlank())
                        ? "账号已被封禁，请联系站长"
                        : "账号已被封禁：" + reason + "（请联系站长）";
                writeJson(response, 403, Result.error(403, text));
                return false;
            }
        }

        // 方法上标了 @RequireRole 才做角色校验；没标表示登录即可
        if (handler instanceof HandlerMethod handlerMethod) {
            RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
            if (requireRole != null) {
                boolean allowed = false;
                for (int r : requireRole.value()) {
                    if (r == role) {
                        allowed = true;
                        break;
                    }
                }
                if (!allowed) {
                    UserContext.clear();
                    writeJson(response, 403, Result.error(403, "没有权限"));
                    return false;
                }
            }
        }
        return true;
    }

    /** 请求处理完后清理 ThreadLocal，防止 Tomcat 线程复用串号 */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) {
        UserContext.clear();
    }

    /** 把统一的 Result 写成 JSON 响应，前端拦截 code/status 统一处理 */
    private void writeJson(HttpServletResponse response, int status, Result result) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
