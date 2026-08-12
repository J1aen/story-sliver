package com.storysliver.service.impl;

import com.storysliver.auth.CaptchaService;
import com.storysliver.auth.JwtUtil;
import com.storysliver.auth.RegisterRateLimiter;
import com.storysliver.common.BusinessException;
import com.storysliver.common.ResultCode;
import com.storysliver.mapper.SystemConfigMapper;
import com.storysliver.mapper.UserMapper;
import com.storysliver.pojo.Auth.RegisterRequest;
import com.storysliver.pojo.SystemConfig;
import com.storysliver.pojo.User;
import com.storysliver.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现：注册 / 登录的核心业务逻辑。
 * 校验顺序为什么固定：验证码 → IP 限流 → 密码强度 → 用户名唯一 → 管理员密码，
 * 越「廉价」的校验越靠前，尽早拦截无效请求、少查数据库。
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;//用户表操作：查重、插入、按 id 查询

    @Autowired
    private SystemConfigMapper systemConfigMapper;//系统配置表：存管理员注册特殊密码

    @Autowired
    private CaptchaService captchaService;//验证码校验（一次性）

    @Autowired
    private RegisterRateLimiter registerRateLimiter;//注册 IP 限流

    @Autowired
    private PasswordEncoder passwordEncoder;//BCrypt 密码加密器（BeanConfig 里注册的 Bean）

    @Autowired
    private JwtUtil jwtUtil;//登录令牌签发

    /**
     * 注册：按顺序完成 8 个动作，任何一个失败就抛 BusinessException 结束。
     * @param request 注册表单
     * @param ip 客户端 IP
     * @return JWT token
     */
    @Override
    public String register(RegisterRequest request, String ip) {
        // 动作1：验证码。verify 内部会 remove，同一个 key 只能用一次
        if (!captchaService.verify(request.getCaptchaKey(), request.getCaptchaAnswer())) {
            throw new BusinessException(ResultCode.CAPTCHA_ERROR);
        }

        // 动作2：IP 限流。防止同一 IP 批量注册
        if (!registerRateLimiter.isAllowed(ip)) {
            throw new BusinessException(ResultCode.REGISTER_TOO_FREQUENT);
        }

        // 动作3：密码至少 6 位。先判 null 再取 length，否则会空指针
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new BusinessException(ResultCode.PASSWORD_TOO_WEAK);
        }

        // 动作4：用户名唯一性。selectByUsername 查得到说明被占用了
        if (userMapper.selectByUsername(request.getUsername()) != null) {
            throw new BusinessException(ResultCode.USERNAME_TAKEN);
        }

        // 动作5：判定角色。默认普通用户；勾选管理员才需要校验特殊密码
        int role = User.ROLE_USER;
        if (Boolean.TRUE.equals(request.getIsAdmin())) {
            // 从配置表读出管理员注册密码的 BCrypt 哈希
            SystemConfig config = systemConfigMapper.selectByKey(SystemConfig.KEY_ADMIN_REGISTER_CODE);
            // 配置不存在或输入不匹配都拒绝；BCrypt matches 是安全比较，不泄露哈希信息
            if (config == null || request.getAdminCode() == null
                    || !passwordEncoder.matches(request.getAdminCode(), config.getConfigValue())) {
                throw new BusinessException(ResultCode.ADMIN_CODE_WRONG);
            }
            // 第一个用特殊密码注册的管理员自动成为站长；之后用同一密码注册的只是普通管理员
            // 为什么统计 countAdmin 而不是 countAll：普通用户先注册不影响站长位，
            // 否则「第一个用户恰好是普通用户」会导致站长位永远锁死
            role = userMapper.countAdmin() == 0 ? User.ROLE_OWNER : User.ROLE_ADMIN;
        }

        // 动作6：组装用户并加密密码。数据库里永远只存 BCrypt 哈希，不存明文
        User user = new User();
        user.setUsername(request.getUsername().trim());// 用户名去首尾空格，避免误输空格导致登录对不上
        user.setNickname(request.getNickname().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setStatus(User.STATUS_NORMAL);
        // email 字段预留：Task 5 暂不收集，以后接 QQ 邮箱验证再加

        // 动作7：存库。useGeneratedKeys 会把自增 id 回填到 user.id（第 8 步签发 token 要用）
        userMapper.insert(user);

        // 动作8：注册成功直接签发 token，省去「注册后再登录」一步
        return jwtUtil.generateToken(user.getId(), user.getRole());
    }

    /**
     * 登录：按用户名查用户，BCrypt 比对密码。
     * 为什么用户不存在和密码错误返回同一个错误：
     * 不暴露「这个用户名有没有注册过」，防止账号枚举攻击。
     */
    @Override
    public String login(String username, String password) {
        User user = userMapper.selectByUsername(username);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(ResultCode.LOGIN_FAILED);
        }
        return jwtUtil.generateToken(user.getId(), user.getRole());
    }

    /** 查询当前用户：前端登录后拉取昵称、角色用于展示 */
    @Override
    public User me(Long userId) {
        return userMapper.selectById(userId);
    }
}
