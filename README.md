> 🚀 ** 1.0 匿名故事墙测试版已上线**：http://101.35.148.131:8080

# story-sliver 匿名故事碎片墙

一个匿名的「故事碎片墙」：注册用户发布小段故事或生活碎片（可选匿名），发布后进入待审核，管理员通过后展示在墙上，其他人可以点赞。

## 技术栈

- 后端：Java 17 + Spring Boot 3.3.5 + MyBatis + MySQL 8
- 前端：Vue3 + Vite + JavaScript + Vue Router + Axios（手写 CSS，温暖文字流风格）
- 认证：JWT（BCrypt 密码加密）
- 部署：腾讯云轻量服务器，单端口 8080，`http://IP:8080` 直连（无需备案）

## 目录结构

| 目录 / 文件 | 说明 |
| --- | --- |
| `fragment-story/` | 后端（Spring Boot，已可编译运行） |
| `fragment-story-frontend/` | 前端（Vue3，待初始化） |
| `制定计划.md` | 产品设计与需求文档 |
| `实现计划.md` | 分阶段实现计划（Task 进度，做完的会划掉） |

## 快速开始（后端）

1. 在 MySQL 中执行 `fragment-story/src/main/resources/db/init.sql`，创建 `story_sliver` 库和 4 张表
2. 修改 `fragment-story/src/main/resources/application.properties` 里的数据库密码
3. 用 IDEA 打开 `fragment-story`，运行 `com.storysliver.StoryApplication`，访问 `http://localhost:8080`

前端初始化后：`cd fragment-story-frontend && npm install && npm run dev`（端口 5173，`/api` 代理到 8080）。

## 功能

- 注册 / 登录（JWT）；注册可选「注册为管理员」（需填写站长才知道的特殊密码）
- 发布碎片：可选匿名、1000 字封顶、5 分钟限发 1 条，发布后进入待审核
- 管理员审核：通过后上墙；审核不通过或违规内容硬删除
- 点赞 / 取消点赞：登录后每人每条一次
- 我的碎片：隐藏 / 取消隐藏（软隐藏可恢复）、删除（弹窗确认，不可恢复）
- 站长权限：指定 / 撤销管理员、修改管理员注册密码

## 部署说明

腾讯云学生机（Ubuntu 22.04 + JDK 17 + MySQL 8），前端打包后由后端单端口托管，详情见 `实现计划.md` Task 18。
