# fragment-story（匿名故事碎片墙 · 后端）

Spring Boot 3 + MyBatis + MySQL 的匿名故事碎片墙后端；前端构建产物由本模块单端口托管（8080）。
完整项目说明见仓库根目录 `README.md`，开发计划见 `实现计划.md`。

## 技术栈

- Java 17 · Spring Boot 3.3.5 · MyBatis（注解 SQL，动态 SQL 用 XML）· MySQL 8
- 认证：JWT（jjwt 0.12.6）+ BCrypt（spring-security-crypto）
- 分页：PageHelper；测试：JUnit 5 + Mockito

## 快速开始

1. 数据库：执行 `src/main/resources/db/init.sql` 建库（**仅开发用**；线上严禁执行，表结构变更一律用增量脚本）
2. 增量脚本：`db/upgrade_v1.2.sql`（昵称唯一 / 敏感词 / 公告）、`db/upgrade_v2.0.sql`（评论 / 评论举报）
3. 配置：修改 `src/main/resources/application.properties` 的数据库账号/密码（生产用 systemd 环境变量覆盖）
4. 运行：IDEA 运行 `com.storysliver.StoryApplication`，或 `mvn spring-boot:run`
5. 访问 http://localhost:8080（前端另跑 `fragment-story-frontend`，`npm run dev` 后 5173 代理到 8080）

## 目录结构

```text
src/main/java/com/storysliver
├── auth/       # JWT、验证码、注册/发布/评论限流、UserContext、@RequireRole、登录拦截器
├── common/     # ResultCode、BusinessException、GlobalExceptionHandler
├── config/     # WebConfig（CORS/拦截器）、BeanConfig（BCrypt）、ConfigSeedRunner（初始管理员密码）
├── controller/ # Auth / User / Fragment / Comment / Announcement / Admin / AdminAnnouncement / AdminSensitiveWord / AdminComment
├── mapper/     # MyBatis @Mapper 接口（注解 SQL；动态 SQL 在 resources/com/storysliver/mapper/*.xml）
├── pojo/       # 实体 + 请求/响应对象（Auth/ Fragment/ Admin 按业务分包；Result、PageBean 在根包）
├── service/    # 业务接口 + impl（@Service + @Autowired 字段注入）
└── resources/
    ├── application.properties
    ├── db/        # init.sql（开发）+ upgrade_v1.2.sql / upgrade_v2.0.sql（增量）
    ├── static/    # 前端构建产物（mvn package 时打入 jar，单端口托管）
    └── com/storysliver/mapper/  # 动态 SQL XML
```

## 当前功能

- 注册/登录：算术验证码（会话绑定、一次性）+ IP 限流；JWT 30 天；封禁体系（登录拦截 + 旧 token 失效）
- 角色：普通用户 / 管理员 / 站长（站长 = 第一个用特殊密码注册的管理员）
- 碎片：发布（5 分钟/条、1000 字封顶、可匿名）→ 待审核 → 管理员上墙；点赞/取消、隐藏/取消、硬删除
- 评论（V2.0）：免审核发布（1 分钟 10 条、100 字封顶）、分页列表（时间正序）、删自己的（硬删除）、举报（理由必填、防重复）
- 评论举报管理（V2.0）：待处理列表；不下架 / 下架 / 下架并封禁评论用户
- 用户：头像上传与审核、昵称唯一 + 敏感词校验、修改密码（旧 token 失效）
- 公告（V1.2）：站长在线编辑、上架/下架、进站弹窗 + 顶部滚动条
- 敏感词库（V1.2）：站长维护，注册 / 改昵称即时拦截

## 测试

```bash
mvn test -f fragment-story/pom.xml
```

> 注意：本机 Maven 命令行编译依赖 pom.xml 里显式声明的 Lombok 注解处理器路径（annotationProcessorPaths），
> 缺了会报大量「找不到 getter/构造器」错误——不要删掉那段配置。
