# 电商购物平台（题目 197）

## 技术栈

- **Frontend**: Vue 3 + Vite + Element Plus
- **Backend**: Java 17 + Spring Boot 3 + MyBatis + MySQL
- **Database**: MySQL 8.0（字符集 utf8mb4，排序 utf8mb4_0900_ai_ci）

## 启动方式

1. 确保已安装并启动 Docker Desktop。
2. 在项目根目录执行：
   ```bash
   docker compose up --build
   ```
3. 等待所有容器就绪（数据库初始化、后端启动、前端构建并挂载完成）。

## 服务地址

- **前端**: http://localhost:3197
- **后端 API**: http://localhost:8197/api
- **数据库**: localhost:3306（用户 root / 密码 root；业务库 shop 用户 shop / 密码 shop123）

## 测试账号

- 管理员: `admin` / `123456`
- 普通用户: `user1` / `123456`，`user2` / `123456`

## 功能模块

- **用户管理**: 注册、登录、个人信息（JWT）、**权限控制**（角色：管理员 admin / 普通用户 user）
- **商品管理**: 分类、列表、详情、搜索
- **购物车**: 添加、修改数量、勾选、删除、结算
- **订单管理**: 创建订单、订单列表、订单详情、支付（模拟）、取消
- **评价模块**: 商品评价、评分、**我的评价**（当前用户评价列表）、**评价管理**（管理员查看/删除全部评价）
- **管理员**: 用户管理（列表、编辑状态/角色）、评价管理（列表、删除）

## 商品图片

- 初始商品主图使用 **Unsplash** 真实商品图（手机、笔记本、衬衫、连衣裙等），以完整 URL 存入 `product.main_image`。
- 前端加载失败时会回退到默认占位图 `/images/default-product.svg`。

## 数据库与字符集

- 建表与连接均使用 **CHARSET=utf8mb4**、**COLLATE=utf8mb4_0900_ai_ci**，支持中文及 emoji。
- 连接串中已指定：`characterEncoding=utf8mb4`、`connectionCollation=utf8mb4_0900_ai_ci`。
- 初始化脚本位于 `scripts/init/`（建表 + Seed 演示数据）。

## Docker 镜像与构建

- 前端：Node 20 Alpine 构建，Nginx Alpine 运行；`/api` 代理到后端服务。
- 后端：Maven + Eclipse Temurin 17 构建与运行；使用 `backend/settings.xml` 阿里云 Maven 镜像。
- 数据库：MySQL 8.0，数据卷持久化；启动命令中指定字符集与排序规则。

## 项目文档（作业提交）

- **需求分析说明书**: `docs/需求分析说明书.md`
- **系统设计说明书**: `docs/系统设计说明书.md`（含 UML 类图、用例图、时序图，图源 `docs/uml/*.puml`）
- **数据库设计说明书**: `docs/数据库设计说明.md`（含 ER 图 `docs/uml/er.puml`）
- **用户操作手册**: `docs/用户操作手册.md`
- **测试报告**: `docs/测试报告.md`
- **项目开发总结报告**: `docs/项目开发总结报告.md`
- **UML/ER 图**: `docs/uml/` 下 `.puml` 文件可用 PlantUML 或 IDE 插件打开/导出图片

## 测试

后端单元测试与集成测试：在 `backend` 目录执行 `mvn test`。
