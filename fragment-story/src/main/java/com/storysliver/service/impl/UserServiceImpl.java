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
import com.storysliver.service.SensitiveWordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;

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

    @Autowired
    private SensitiveWordService sensitiveWordService;//敏感词校验（v1.2：昵称不能含敏感词）

    /** 头像保存目录：来自 application.properties 的 app.upload.avatar-dir（当前 D:/HeadImage） */
    @Value("${app.upload.avatar-dir}")
    private String avatarDir;

    /** 头像大小上限：2MB */
    private static final long MAX_AVATAR_SIZE = 2 * 1024 * 1024L;
    /** 头像统一裁剪成 128x128 正方形 */
    private static final int AVATAR_SIZE = 128;

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

        // 动作3.5（Task 29）：用户名格式——只允许字母/数字/下划线（禁止汉字等），已注册老账号不受影响。
        // 为什么先 trim 再校验：用户名首尾带空格能通过长度校验，但登录时对不上，容易造成「注册了登不上」的坑。
        String username = request.getUsername() == null ? "" : request.getUsername().trim();
        if (!username.matches("^[A-Za-z0-9_]{4,20}$")) {
            throw new BusinessException(ResultCode.USERNAME_INVALID);
        }

        // 动作4：用户名唯一性。selectByUsername 查得到说明被占用了
        if (userMapper.selectByUsername(username) != null) {
            throw new BusinessException(ResultCode.USERNAME_TAKEN);
        }

        // 动作4.5（v1.2）：昵称唯一 + 敏感词校验。昵称会对外展示（个人主页 @昵称），
        // 重复会混淆用户，敏感词会违规，都在入库前拦截。
        String nickname = request.getNickname() == null ? "" : request.getNickname().trim();
        if (userMapper.selectByNickname(nickname) != null) {
            throw new BusinessException(ResultCode.NICKNAME_TAKEN);
        }
        if (sensitiveWordService.containsSensitive(nickname)) {
            throw new BusinessException(ResultCode.NICKNAME_SENSITIVE);
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
        user.setUsername(username);// 用户名已在动作3.5 trim 过
        user.setNickname(nickname);
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
        if (user == null) {
            throw new BusinessException(ResultCode.LOGIN_FAILED);
        }
        // 封禁检查放在密码校验之前：被封禁的账号登录时提示封禁（即使密码输错）
        // 已到期的封禁自动解封
        if (user.getStatus() != null && user.getStatus() == User.STATUS_BANNED) {
            if (user.getBanExpiresAt() != null && !user.getBanExpiresAt().isAfter(LocalDateTime.now())) {
                userMapper.unban(user.getId());// 封禁到期，自动解封
            } else {
                throw new BusinessException(ResultCode.ACCOUNT_BANNED, banMessage(user));
            }
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(ResultCode.LOGIN_FAILED);
        }
        return jwtUtil.generateToken(user.getId(), user.getRole());
    }

    /** 封禁提示文案：带上理由，让被封禁用户知道原因 */
    private String banMessage(User user) {
        String reason = user.getBanReason();
        return reason == null || reason.isBlank()
                ? ResultCode.ACCOUNT_BANNED.getMessage()
                : "账号已被封禁：" + reason + "（请联系站长）";
    }

    /** 查询当前用户：前端登录后拉取昵称、角色用于展示 */
    @Override
    public User me(Long userId) {
        return userMapper.selectById(userId);
    }

    /**
     * 上传头像：校验 → 保存到本地目录 → 写入「待审核」字段。
     * 为什么进待审核而不是直接生效：头像也是内容，要经过管理员审核（与碎片一致）。
     */
    @Override
    public String uploadAvatar(Long userId, MultipartFile file) {
        // 1. 校验类型：只允许 jpg/png
        String contentType = file == null ? null : file.getContentType();
        if (contentType == null || !(contentType.equals("image/jpeg") || contentType.equals("image/png"))) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "头像只支持 jpg/png 格式");
        }
        // 2. 校验大小：不超过 2MB
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "头像大小不能超过 2MB");
        }
        // 3. 生成唯一文件名并保存（保留透明通道，统一 128x128，存 PNG）
        String fileName = "avatar_" + userId + "_" + System.currentTimeMillis() + ".png";
        File dir = new File(avatarDir);
        if (!dir.exists()) {
            dir.mkdirs();//目录不存在就创建
        }
        File target = new File(dir, fileName);
        try {
            BufferedImage src = ImageIO.read(file.getInputStream());
            if (src == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "无法解析图片");
            }
            // Task 19：最小尺寸校验——太小的图片放大后会糊，直接拒绝
            if (src.getWidth() < 64 || src.getHeight() < 64) {
                throw new BusinessException(ResultCode.AVATAR_TOO_SMALL);
            }
            ImageIO.write(cropToFilledSquare(src, AVATAR_SIZE), "png", target);
        } catch (IOException e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "头像保存失败");
        }
        // 4. 写入待审核字段（旧头像继续显示，审核通过后才替换）
        String url = "/uploads/" + fileName;
        userMapper.updateAvatarPending(userId, url);
        return url;
    }

    /**
     * 修改昵称（v1.2）：唯一性 + 敏感词校验通过后更新。
     * 为什么在 Service 里校验而不是只在 Controller：校验规则（唯一/敏感词）是业务规则，集中在一处好维护。
     */
    @Override
    public void updateNickname(Long userId, String nickname) {
        nickname = nickname == null ? "" : nickname.trim();
        if (nickname.isEmpty() || nickname.length() > 32) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "昵称不能为空且最长 32 位");
        }
        if (sensitiveWordService.containsSensitive(nickname)) {
            throw new BusinessException(ResultCode.NICKNAME_SENSITIVE);
        }
        // 查重：查到的是「别人」才算重复（自己改自己的昵称不算）
        User byNickname = userMapper.selectByNickname(nickname);
        if (byNickname != null && !byNickname.getId().equals(userId)) {
            throw new BusinessException(ResultCode.NICKNAME_TAKEN);
        }
        userMapper.updateNickname(userId, nickname);
    }

    /**
     * 修改密码（v1.2）：旧密码必须正确，新密码 BCrypt 加密落库。
     * 为什么旧密码用 matches 而不是解密比对：BCrypt 不可逆，只能拿明文去比对哈希。
     */
    @Override
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null || oldPassword == null || !passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(ResultCode.OLD_PASSWORD_WRONG);
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new BusinessException(ResultCode.PASSWORD_TOO_WEAK);
        }
        userMapper.updatePassword(userId, passwordEncoder.encode(newPassword));
    }

    /**
     * Task 19：把图片「内容」居中裁成正方形并缩放到指定尺寸。
     * 为什么不用原来的 cropToSquare：旧实现直接取整张图中心正方形，
     * 如果图片四周是透明边距（前端裁剪时圆形画小了），内容会很小，显示出来就是「特别小的头像」。
     * 现在先扫描不透明像素找到内容边界，裁掉透明空边再取正方形，保证头像占满画布。
     */
    private BufferedImage cropToFilledSquare(BufferedImage src, int size) {
        int w = src.getWidth();
        int h = src.getHeight();
        // 1. 扫描 alpha 通道，找出「可见内容」的边界（alpha > 8 视为可见，忽略抗锯齿的浅透明像素）
        int minX = w, minY = h, maxX = -1, maxY = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int alpha = (src.getRGB(x, y) >>> 24) & 0xFF;
                if (alpha > 8) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }
        // 全透明（理论上不会发生）：退回整张图中心裁剪，保证不抛异常
        if (maxX < 0) {
            return cropToSquare(src, size);
        }
        // 2. 边界外扩 4px，避免圆形边缘被切得太紧
        int pad = 4;
        minX = Math.max(0, minX - pad);
        minY = Math.max(0, minY - pad);
        maxX = Math.min(w - 1, maxX + pad);
        maxY = Math.min(h - 1, maxY + pad);
        // 3. 以内容中心取正方形（边长取宽高的较大值），再缩放
        int side = Math.max(maxX - minX + 1, maxY - minY + 1);
        int cx = (minX + maxX) / 2;
        int cy = (minY + maxY) / 2;
        int x0 = Math.max(0, Math.min(cx - side / 2, w - side));
        int y0 = Math.max(0, Math.min(cy - side / 2, h - side));
        BufferedImage crop = src.getSubimage(x0, y0, side, side);
        // 4. 缩放输出，保持透明通道
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(crop, 0, 0, size, size, null);
        g.dispose();
        return out;
    }

    /** 把任意图片中心裁剪成正方形并缩放到指定尺寸（保留透明通道，供前端圆形头像使用） */
    private BufferedImage cropToSquare(BufferedImage src, int size) {
        int w = src.getWidth();
        int h = src.getHeight();
        int side = Math.min(w, h);//取短边作为正方形边长
        int x = (w - side) / 2;
        int y = (h - side) / 2;
        BufferedImage crop = src.getSubimage(x, y, side, side);
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(crop, 0, 0, size, size, null);
        g.dispose();
        return out;
    }
}
