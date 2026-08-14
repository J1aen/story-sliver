> 🚀 匿名故事墙测试版1.0 已上线

> 📅 首次发布：2026 年 8 月

# story-sliver 匿名故事碎片墙

## 一、项目简介

「匿名故事碎片墙」（Story Sliver）是一个轻量匿名 UGC 网站：在这里，任何人都可以**匿名写故事**、分享生活碎片，发布时自由选择是否匿名；提交后进入**待审核**状态，管理员通过后展示在「碎片墙」上，其他用户可以点赞。简单来说，这是一个让每个人都能**匿名发故事**、互相阅读和点赞的碎片墙。

- **核心体验**：温暖、克制的内容墙——米白纸感、瀑布流布局、手机优先适配
- **匿名设计**：匿名只影响对外展示，数据库始终记录真实发布者（`user_id`），方便管理员追溯违规内容
- **当前状态**：测试版 1.0 已上线，公网直连即可使用（IP + 8080 端口访问）

## 二、功能清单

### 用户侧

| 功能 | 说明 |
| --- | --- |
| 注册 / 登录 | JWT 登录态；注册需算术验证码 + IP 限流（防脚本）；密码 BCrypt 加密存储 |
| 角色体系 | 普通用户 / 管理员 / 站长；站长 = 第一个用特殊密码注册的管理员 |
| 发布碎片 | 1000 字封顶；每个用户 5 分钟限发 1 条；可匿名；发布后进入待审核 |
| 碎片墙 | 只展示「已发布」的碎片；瀑布流（桌面 3 列 / 平板 2 列 / 手机 1 列）；长碎片（≥200 字）独占一行 |
| 点赞 | 登录用户每人每条只能赞一次，可取消 |
| 我的碎片 | 查看自己的全部碎片；可隐藏 / 取消隐藏（软删除）；删除需弹窗确认且**不可恢复**（硬删除） |
| 个人主页 | 展示昵称、头像、身份铭牌（站长金色 / 管理员蓝色）；可上传自定义头像 |

### 管理侧

| 功能 | 说明 |
| --- | --- |
| 碎片审核 | 待审核列表 → 通过上墙 / 拒绝（硬删除）；可「删除并封禁」违规账号 |
| 头像审核 | 用户上传的头像需审核；通过后转正，拒绝时必填原因并通知用户 |
| 用户管理 | 用户列表（全部 / 正常 / 暂时封禁 / 永久封禁筛选）；封禁支持自定义天数或永久；**封禁理由必填**；被封禁用户登录时可见原因 |
| 权限矩阵 | 站长最高权限（任何人不可封禁站长）；管理员不能封禁管理员；只有站长能解封、指定 / 撤销管理员 |
| 站长配置 | 在线修改「管理员注册特殊密码」（BCrypt 哈希存储，重启不会被重置） |

### 防滥用设计

- 算术验证码：会话绑定、一次性使用
- 注册限流：按 IP（1 小时 5 个 / 24 小时 10 个）
- 发布限流：每个用户 5 分钟 1 条
- 账号封禁：登录时拦截并提示原因，旧 token 同步失效

## 三、技术栈

| 层 | 技术 |
| --- | --- |
| 后端 | Java 17 · Spring Boot 3.3.5 · MyBatis · MySQL 8 |
| 认证 | JWT（jjwt 0.12.6）+ BCrypt（spring-security-crypto） |
| 前端 | Vue 3 · Vite 5 · Vue Router 4 · Axios · 手写 CSS |
| 测试 | JUnit 5 + Mockito |
| 部署 | 腾讯云轻量服务器 · systemd 常驻 · 每日自动备份 |

## 四、项目结构

```text
story-sliver
├── fragment-story/                     # 后端（Spring Boot 3）
│   ├── src/main/java/com/storysliver/
│   │   ├── controller/                 # 接口层：Auth / User / Fragment / Admin
│   │   ├── service/                    # 业务接口
│   │   │   └── impl/                   # 业务实现（@Service + @Autowired 字段注入）
│   │   ├── mapper/                     # MyBatis Mapper（@Mapper + 注解 SQL）
│   │   ├── pojo/                       # 实体 + 请求/结果对象（Result / PageBean）
│   │   ├── auth/                       # JWT 工具、验证码、限流、登录拦截
│   │   ├── config/                     # CORS、拦截器注册、Bean、启动种子数据
│   │   └── common/                     # 统一错误码、业务异常、全局异常处理
│   ├── src/main/resources/
│   │   ├── application.properties      # 配置文件（数据库 / JWT / 上传目录）
│   │   ├── db/init.sql                 # 建库建表脚本（仅开发初始化用！线上严禁执行）
│   │   └── static/                     # 前端构建产物（由 Spring Boot 单端口托管）
│   └── deploy/                         # story.service / backup.sh / 部署指南.md
├── fragment-story-frontend/            # 前端（Vue 3）
│   └── src/
│       ├── api/                        # Axios 封装 + 认证/碎片/管理接口模块
│       ├── stores/                     # 用户状态（localStorage 持久化）
│       ├── router/                     # 路由 + 登录/管理员守卫
│       ├── views/                      # 首页 / 登录注册 / 我的碎片 / 管理后台 / 个人主页
│       ├── components/                 # 碎片卡片、发布框、验证码、确认弹窗等
│       └── styles/                     # 全局样式（温暖文字流风格）
├── 实现计划.md                         # 分阶段实现计划（Task 1-18 进度跟踪）
├── 制定计划.md                         # 产品设计与需求文档
└── 后端套路速查.md                    # 后端学习笔记（代码风格规范）
```

## 五、数据库设计（4 张表）

### `user`（用户）

| 字段 | 说明 |
| --- | --- |
| id | 主键 |
| username | 登录用户名（唯一） |
| nickname | 展示昵称 |
| password | BCrypt 密码哈希（绝不存明文） |
| email | 预留邮箱（当前不验证，为后续邮箱验证预留） |
| role | 角色：0 普通 / 1 管理员 / 2 站长 |
| status | 状态：0 正常 / 1 封禁 |
| avatar / avatar_pending | 当前头像 / 待审核头像 |
| avatar_reject_reason | 头像拒绝原因 |
| ban_expires_at / ban_reason | 封禁到期时间（null 且 status=1 表示永久）/ 封禁理由 |

### `story_fragment`（故事碎片）

| 字段 | 说明 |
| --- | --- |
| id | 主键 |
| user_id | 真实发布者 id（匿名时仍记录，便于管理员追溯） |
| content | 碎片内容（1000 字封顶） |
| like_count | 点赞数（冗余计数） |
| is_anonymous | 0 显示昵称 / 1 显示「匿名用户」 |
| status | 0 待审核 / 1 已发布 / 2 已隐藏 |

### `fragment_like`（点赞）

| 字段 | 说明 |
| --- | --- |
| id | 主键 |
| fragment_id / user_id | 碎片 id / 点赞用户 id |
| 唯一约束 | (fragment_id, user_id) 保证每人每条只能赞一次 |

### `system_config`（系统配置）

| 字段 | 说明 |
| --- | --- |
| config_key | 配置键（主键），如 admin_register_code |
| config_value | 配置值（管理员注册密码的 BCrypt 哈希） |

## 六、核心接口一览

### 认证 `/api/auth`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | /api/auth/captcha | 获取算术验证码 |
| POST | /api/auth/register | 注册（可勾选注册为管理员） |
| POST | /api/auth/login | 登录 |

### 用户 `/api/users`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | /api/users/me | 当前用户信息 |
| POST | /api/users/avatar | 上传头像（进入待审核） |

### 碎片 `/api/fragments`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | /api/fragments | 发布碎片（5 分钟限 1 条） |
| GET | /api/fragments | 首页分页列表（仅已发布） |
| GET | /api/fragments/my | 我的碎片 |
| POST | /api/fragments/{id}/like | 点赞 |
| DELETE | /api/fragments/{id}/like | 取消点赞 |
| PUT | /api/fragments/{id}/hide | 隐藏（软删除） |
| PUT | /api/fragments/{id}/unhide | 取消隐藏 |
| DELETE | /api/fragments/{id} | 硬删除（弹窗确认） |

### 管理 `/api/admin`（管理员 / 站长）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | /api/admin/fragments | 待审核列表 |
| POST | /api/admin/fragments/{id}/approve | 审核通过 |
| DELETE | /api/admin/fragments/{id} | 拒绝 / 硬删除 |
| GET | /api/admin/users | 用户管理列表 |
| PUT | /api/admin/users/{id}/role | 指定 / 撤销管理员（仅站长） |
| POST | /api/admin/users/{id}/ban | 封禁（天数或永久，理由必填） |
| POST | /api/admin/users/{id}/unban | 解封（仅站长） |
| GET | /api/admin/avatars | 头像审核列表 |
| POST | /api/admin/avatars/{userId}/approve | 头像通过 |
| POST | /api/admin/avatars/{userId}/reject | 头像拒绝（带原因） |
| PUT | /api/admin/config/admin-register-code | 修改管理员注册密码（仅站长） |

## 七、本地快速开始

**环境要求**：JDK 17 · MySQL 8 · Node.js 18+

1. **初始化数据库**：在本地 MySQL 执行 `fragment-story/src/main/resources/db/init.sql`，创建 `story_sliver` 库和 4 张表
2. **配置后端**：修改 `fragment-story/src/main/resources/application.properties` 里的数据库账号密码（默认配置仅限本地开发，生产环境通过部署环境变量覆盖）
3. **启动后端**：IDEA 打开 `fragment-story`，运行 `com.storysliver.StoryApplication`，访问 `http://localhost:8080`
4. **启动前端**：
   ```bash
   cd fragment-story-frontend
   npm install
   npm run dev
   ```
   访问 `http://localhost:5173`（开发环境 `/api` 自动代理到 8080）
5. 注册（验证码）→ 自动登录 → 发布 → 管理员审核 → 全流程体验

## 八、打包与上线（单端口 8080）

1. 构建前端：`cd fragment-story-frontend && npm run build`，生成 `dist/`
2. 拷贝到后端静态目录（工作区根目录执行）：
   ```powershell
   Copy-Item -Recurse -Force dist\* fragment-story\src\main\resources\static\
   ```
3. 打包后端：
   ```bash
   mvn -q -DskipTests package -f fragment-story/pom.xml
   ```
4. 上传 jar 到服务器：`/home/ubuntu/story/`
5. 重启服务：`sudo systemctl restart story`
6. 验证：浏览器打开 `http://服务器IP:8080`

> ⚠️ **线上严禁重新执行 `init.sql`**（开头有 `DROP TABLE` 会清空数据）；表结构变更必须写增量 SQL。

## 九、部署架构

- **服务器**：腾讯云轻量（Ubuntu 24.04 · 2 核 2G · 50G 磁盘）
- **常驻**：systemd 服务 `story.service`，开机自启、崩溃自动重启
- **配置覆盖**：通过 systemd 环境变量覆盖数据库账号 / 密码、JWT 密钥、头像目录（不改 jar 内配置）
- **头像存储**：服务器本地 `/home/ubuntu/story/headimage`（本地开发用 `D:/HeadImage`）
- **自动备份**：每天凌晨 3 点备份 MySQL 数据 + 头像目录，保留最近 14 天

## 十、上线安全清单

- [ ] JWT 密钥已改为随机长字符串（≥32 字节）
- [ ] 站长已注册并**立即修改管理员注册特殊密码**（默认值在仓库中公开）
- [ ] 数据库密码已改为强密码
- [ ] SSH 建议改为密钥登录
- [ ] 防火墙只放行必要端口（如 22 / 8080）
- [ ] 定期检查备份是否正常

## 十一、常见问题

- **为什么百度搜不到**：IP + 8080 端口的形式不会被搜索引擎收录；需要「域名 + 80 端口」，详见 `fragment-story/deploy/部署指南.md`
- **怎么在 IDEA 里查看线上数据库**：通过 SSH 隧道连接（避免把数据库端口暴露公网）；连接账号与密码属于敏感信息，请参考服务器部署配置填写，不在公开文档中列出
- **更新新功能**：改代码 → `npm run build` + 拷贝 static → `mvn package` → 上传 jar → `systemctl restart story`（不动数据库结构时仅此而已）

## 十二、许可证

本项目采用 **BSD 3-Clause License**，详见 [LICENSE](./LICENSE)。

## 十三、作者

h1ecc · Gitee：[story-sliver](https://gitee.com/h1ecc/story-sliver)
