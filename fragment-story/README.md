# fragment-story（匿名故事碎片墙 - 后端）

Spring Boot 3 + MyBatis + MySQL 的后端基础架构，开箱即用，可直接在 IDEA 中打开继续写代码。

## 已包含

- 分层结构：controller / service / mapper / entity / dto / common / config / admin
- 统一响应体 `Result<T>` + 全局异常处理
- 故事碎片基础接口：提交、分页列表、点赞
- 管理员接口：登录（内存 token）、删除违规碎片（软删除）
- 跨域配置（前端开发服务器可直接访问）
- 数据库初始化脚本

## 环境要求

- JDK 17+（本机已装 JDK 19）
- Maven 3.6+
- MySQL 8.x

## 快速开始

1. 在 MySQL 中执行初始化脚本：

   ```sql
   source src/main/resources/db/init.sql;
   ```

   或在 Navicat / Workbench 中直接运行 `src/main/resources/db/init.sql`。

2. 修改数据库账号密码（如与默认不一致）：

   `src/main/resources/application.yml` 中的 `spring.datasource.username / password`。

3. 用 IDEA 打开本目录（`fragment-story`），等待 Maven 导入依赖后，
   运行 `com.storysliver.StoryApplication` 的 `main` 方法。

启动成功后访问：`http://localhost:8080`

## 接口一览

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| POST | `/api/fragments` | 提交一条匿名碎片 | 无 |
| GET | `/api/fragments?pageNum=1&pageSize=10` | 分页获取碎片（最新在前） | 无 |
| POST | `/api/fragments/{id}/like` | 给碎片点赞 | 无 |
| POST | `/api/admin/login` | 管理员登录，返回 token | 无 |
| DELETE | `/api/admin/fragments/{id}` | 管理员删除碎片（软删除） | 管理员 token |

示例请求：

```bash
# 提交碎片
curl -X POST http://localhost:8080/api/fragments \
  -H "Content-Type: application/json" \
  -d '{"content":"今天路过一家旧书店，闻到了阳光的味道。"}'

# 管理员登录
curl -X POST http://localhost:8080/api/admin/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 管理员删除（将 TOKEN 换成登录返回的 token）
curl -X DELETE http://localhost:8080/api/admin/fragments/1 \
  -H "X-Admin-Token: TOKEN"
```

## 默认配置

| 配置 | 默认值 | 说明 |
| --- | --- | --- |
| 服务端口 | `8080` | `server.port` |
| MySQL 地址 | `localhost:3306/story_sliver` | `spring.datasource.url` |
| 数据库账号 | `root / root` | `spring.datasource` |
| 管理员账号 | `admin / admin123` | `app.admin` |

## 注意事项

- 评论功能暂未开放，按需求后续再加 `comment` 表与接口即可。
- 管理员 token 目前存内存，重启服务后失效；后续可替换为 JWT / Redis。
- 删除为软删除（`status = 1`），列表查询只返回 `status = 0` 的数据。

## 目录结构

```text
fragment-story
├── pom.xml
└── src/main
    ├── java/com/storysliver
    │   ├── StoryApplication.java        # 启动类
    │   ├── common/                      # 统一响应、异常处理
    │   ├── config/                      # 跨域、拦截器配置
    │   ├── controller/                  # 接口层
    │   ├── service/                     # 业务层
    │   ├── mapper/                      # MyBatis Mapper 接口
    │   ├── entity/                      # 实体类
    │   ├── dto/                         # 请求/响应对象
    │   └── admin/                       # 管理员登录与鉴权
    └── resources
        ├── application.yml              # 配置文件
        ├── mapper/                      # MyBatis XML
        └── db/init.sql                  # 数据库初始化脚本
```
