package com.storysliver.service;

import com.storysliver.auth.CaptchaService;
import com.storysliver.auth.JwtProperties;
import com.storysliver.auth.JwtUtil;
import com.storysliver.auth.RegisterRateLimiter;
import com.storysliver.common.BusinessException;
import com.storysliver.common.ResultCode;
import com.storysliver.mapper.SystemConfigMapper;
import com.storysliver.mapper.UserMapper;
import com.storysliver.pojo.Auth.RegisterRequest;
import com.storysliver.pojo.SystemConfig;
import com.storysliver.pojo.User;
import com.storysliver.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用户服务单元测试：注册 8 个动作、登录、me 的关键行为。
 * 为什么用 Mockito 模拟 Mapper：不连数据库也能测业务逻辑，速度快、隔离性好。
 */
class UserServiceTest {

    private UserMapper userMapper;
    private SystemConfigMapper systemConfigMapper;
    private CaptchaService captchaService;
    private RegisterRateLimiter registerRateLimiter;
    private PasswordEncoder passwordEncoder;
    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        // 全部依赖都用 mock，只有 PasswordEncoder 和 JwtUtil 用真实实现
        userMapper = mock(UserMapper.class);
        systemConfigMapper = mock(SystemConfigMapper.class);
        captchaService = mock(CaptchaService.class);
        registerRateLimiter = mock(RegisterRateLimiter.class);
        passwordEncoder = new BCryptPasswordEncoder();

        // 真实 JwtUtil：测试密钥（>=32 字节），方便断言 token 内容
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-secret-test-secret-test-secret-123456");
        properties.setExpireDays(30);
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "properties", properties);

        // 真实 service + 反射注入字段（模拟 Spring 的 @Autowired）
        service = new UserServiceImpl();
        ReflectionTestUtils.setField(service, "userMapper", userMapper);
        ReflectionTestUtils.setField(service, "systemConfigMapper", systemConfigMapper);
        ReflectionTestUtils.setField(service, "captchaService", captchaService);
        ReflectionTestUtils.setField(service, "registerRateLimiter", registerRateLimiter);
        ReflectionTestUtils.setField(service, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(service, "jwtUtil", jwtUtil);
    }

    /** 构造一个注册请求：默认验证码 key=k、答案=8 */
    private RegisterRequest request(String username, String password, boolean isAdmin, String adminCode) {
        RegisterRequest r = new RegisterRequest();
        r.setUsername(username);
        r.setNickname(username);
        r.setPassword(password);
        r.setCaptchaKey("k");
        r.setCaptchaAnswer("8");
        r.setIsAdmin(isAdmin);
        r.setAdminCode(adminCode);
        return r;
    }

    /** 正常注册：验证码对、限流放行、用户名没人用 → 角色 0、密码加密、返回 token */
    @Test
    void registerNormalUserSucceeds() {
        when(captchaService.verify("k", "8")).thenReturn(true);
        when(registerRateLimiter.isAllowed("1.2.3.4")).thenReturn(true);
        when(userMapper.selectByUsername("alice")).thenReturn(null);
        // insert 时模拟数据库回填自增 id = 1
        when(userMapper.insert(any())).thenAnswer(inv -> {
            inv.<User>getArgument(0).setId(1L);
            return 1;
        });

        String token = service.register(request("alice", "123456", false, null), "1.2.3.4");

        assertNotNull(token, "注册成功应返回 token");
        // 用 ArgumentCaptor 取出真正 insert 的用户，验证角色和加密
        org.mockito.ArgumentCaptor<User> captor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        User saved = captor.getValue();
        assertEquals(0, saved.getRole(), "普通用户角色应为 0");
        assertTrue(passwordEncoder.matches("123456", saved.getPassword()), "库里存的应是 BCrypt 哈希");
    }

    /** 验证码错误 → 抛 CAPTCHA_ERROR */
    @Test
    void registerFailsWhenCaptchaWrong() {
        when(captchaService.verify("k", "8")).thenReturn(false);
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.register(request("alice", "123456", false, null), "1.2.3.4"));
        assertEquals(ResultCode.CAPTCHA_ERROR, e.getResultCode());
    }

    /** IP 被限流 → 抛 REGISTER_TOO_FREQUENT */
    @Test
    void registerFailsWhenRateLimited() {
        when(captchaService.verify("k", "8")).thenReturn(true);
        when(registerRateLimiter.isAllowed("1.2.3.4")).thenReturn(false);
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.register(request("alice", "123456", false, null), "1.2.3.4"));
        assertEquals(ResultCode.REGISTER_TOO_FREQUENT, e.getResultCode());
    }

    /** 密码不足 6 位 → 抛 PASSWORD_TOO_WEAK */
    @Test
    void registerFailsWhenPasswordTooWeak() {
        when(captchaService.verify("k", "8")).thenReturn(true);
        when(registerRateLimiter.isAllowed("1.2.3.4")).thenReturn(true);
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.register(request("alice", "123", false, null), "1.2.3.4"));
        assertEquals(ResultCode.PASSWORD_TOO_WEAK, e.getResultCode());
    }

    /** 用户名被占用 → 抛 USERNAME_TAKEN */
    @Test
    void registerFailsWhenUsernameTaken() {
        when(captchaService.verify("k", "8")).thenReturn(true);
        when(registerRateLimiter.isAllowed("1.2.3.4")).thenReturn(true);
        when(userMapper.selectByUsername("alice")).thenReturn(new User());
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.register(request("alice", "123456", false, null), "1.2.3.4"));
        assertEquals(ResultCode.USERNAME_TAKEN, e.getResultCode());
    }

    /** 第一个用正确特殊密码注册的管理员 → 角色 2（站长） */
    @Test
    void firstAdminBecomesOwner() {
        when(captchaService.verify("k", "8")).thenReturn(true);
        when(registerRateLimiter.isAllowed("1.2.3.4")).thenReturn(true);
        when(userMapper.selectByUsername("admin1")).thenReturn(null);
        SystemConfig config = new SystemConfig();
        config.setConfigKey(SystemConfig.KEY_ADMIN_REGISTER_CODE);
        config.setConfigValue(passwordEncoder.encode("SecretCode1"));
        when(systemConfigMapper.selectByKey(SystemConfig.KEY_ADMIN_REGISTER_CODE)).thenReturn(config);
        when(userMapper.countAdmin()).thenReturn(0L);

        service.register(request("admin1", "123456", true, "SecretCode1"), "1.2.3.4");

        org.mockito.ArgumentCaptor<User> captor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        assertEquals(2, captor.getValue().getRole(), "第一个管理员应为站长（角色 2）");
    }

    /** 回归测试：先注册过普通用户，第一个管理员依然成为站长（修复站长位锁死 bug） */
    @Test
    void firstAdminStillBecomesOwnerWhenNormalUsersExist() {
        when(captchaService.verify("k", "8")).thenReturn(true);
        when(registerRateLimiter.isAllowed("1.2.3.4")).thenReturn(true);
        when(userMapper.selectByUsername("admin1")).thenReturn(null);
        SystemConfig config = new SystemConfig();
        config.setConfigKey(SystemConfig.KEY_ADMIN_REGISTER_CODE);
        config.setConfigValue(passwordEncoder.encode("SecretCode1"));
        when(systemConfigMapper.selectByKey(SystemConfig.KEY_ADMIN_REGISTER_CODE)).thenReturn(config);
        // 已存在普通用户，但管理员数量为 0 → 第一个管理员仍应是站长
        when(userMapper.countAdmin()).thenReturn(0L);

        service.register(request("admin1", "123456", true, "SecretCode1"), "1.2.3.4");

        org.mockito.ArgumentCaptor<User> captor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        assertEquals(2, captor.getValue().getRole(), "普通用户先注册不应影响第一个管理员成为站长");
    }

    /** 管理员特殊密码错误 → 抛 ADMIN_CODE_WRONG */
    @Test
    void adminCodeWrongFails() {
        when(captchaService.verify("k", "8")).thenReturn(true);
        when(registerRateLimiter.isAllowed("1.2.3.4")).thenReturn(true);
        when(userMapper.selectByUsername("admin1")).thenReturn(null);
        SystemConfig config = new SystemConfig();
        config.setConfigValue(passwordEncoder.encode("RightCode1"));
        when(systemConfigMapper.selectByKey(SystemConfig.KEY_ADMIN_REGISTER_CODE)).thenReturn(config);

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.register(request("admin1", "123456", true, "WrongCode"), "1.2.3.4"));
        assertEquals(ResultCode.ADMIN_CODE_WRONG, e.getResultCode());
    }

    /** 登录成功：返回 token，且 token 里是正确用户 id */
    @Test
    void loginSucceeds() {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setRole(0);
        user.setPassword(passwordEncoder.encode("123456"));
        when(userMapper.selectByUsername("alice")).thenReturn(user);

        String token = service.login("alice", "123456");
        assertNotNull(token);
    }

    /** 密码错误 → 抛 LOGIN_FAILED */
    @Test
    void loginFailsWhenPasswordWrong() {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setRole(0);
        user.setPassword(passwordEncoder.encode("123456"));
        when(userMapper.selectByUsername("alice")).thenReturn(user);

        BusinessException e = assertThrows(BusinessException.class, () -> service.login("alice", "wrong"));
        assertEquals(ResultCode.LOGIN_FAILED, e.getResultCode());
    }

    /** me：按 id 返回用户 */
    @Test
    void meReturnsUser() {
        User user = new User();
        user.setId(1L);
        when(userMapper.selectById(1L)).thenReturn(user);
        assertEquals(user, service.me(1L));
    }
}
