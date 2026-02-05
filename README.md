# controlPhoneDesk 管理后台（Java + React）

面向**本地运行**的 Android 设备管理后台（Android-only），支持通过浏览器远程管理 **USB / TCP** 连接的 Android 设备，实现投屏与控制。

## 鸣谢 / 致谢

- scrcpy: https://github.com/Genymobile/scrcpy
- ws-scrcpy: https://github.com/NetrisTV/ws-scrcpy

## 声明

- 纯 vibe coding 项目
- 开发工具：codex-cli 0.94.0
- 使用模型：gpt-5.2-codex (high)
- Skills：vercel-react-best-practices
  https://github.com/vercel-labs/agent-skills/tree/main/skills/react-best-practices

## 技术栈

### 后端

- Java 21
- Spring Boot 4.0.2
- SQLite
- JWT 鉴权
- ADB / scrcpy

### 前端（Web / H5）

- React
- Vite

## 项目功能

- 浏览器远程管理 Android 设备（USB / TCP）
- 基于 scrcpy 的实时投屏与控制
- Web / H5 双端控制界面
- 简单的后台管理与权限控制
- RBAC 权限模型（接口级）

## 界面截图

设备投屏：

![设备投屏](img/show.png)

设备管理：

![设备管理](img/sbgl.png)

设备分组：

![设备分组](img/sbfz.png)

用户管理：

![用户管理](img/yhgl.png)

用户分组：

![用户分组](img/yhfz.png)

## 目录结构

```text
controlPhoneDesk/
├─ backend/        # Spring Boot 4 后端（Java 21）
├─ frontend/       # Web 管理端（React + Vite）
├─ frontend-h5/    # H5 控制端（React + Vite）
└─ shared/         # 前后端共享代码
```

## 后端启动

```bash
cd backend
mvn spring-boot:run
```

默认端口：`8080`

### 后端配置（backend/src/main/resources/application.yml）

- `app.adb.bin`: adb 可执行文件路径（默认 `adb`）
- `app.scrcpy.serverPort`: scrcpy server 端口（默认 `8886`）
- `spring.datasource.url`: SQLite 数据库路径（默认 `./data/app.db`）
- `app.security.jwt.secret`: JWT 密钥（生产环境请替换）

## 前端启动

先在 `frontend/.env` 和 `frontend-h5/.env` 配置接口地址：

```env
VITE_API_BASE=http://localhost:8080
```

Web 管理端：

```bash
cd frontend
npm install
npm run dev
```

H5 控制端：

```bash
cd frontend-h5
npm install
npm run dev
```

默认端口：
- Web 端：`5173`
- H5 端：`5174`

## 使用方式

1. 确保 `adb devices` 能识别 Android 设备。
2. 访问 Web 端：http://localhost:5173
3. 访问 H5 端：http://localhost:5174
4. 使用默认超级管理员登录：`admin / admin123`
5. 在设备管理/分组管理页面连接设备进行投屏与控制。

## 说明

- RBAC 权限模型：用户-角色-权限，权限控制到接口级。
- 除登录接口外，所有接口均需 JWT 鉴权。
- 超级管理员拥有所有权限，不受 RBAC 限制。
- WebCodecs/MSE 自动降级：IP 访问非安全上下文时自动切换到 MSE 解码。
