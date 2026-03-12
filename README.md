# dmanager-desktop

一个跨平台桌面工具，用于通过 SSH 管理远程 Docker 主机。

## 核心能力（v1）
- 管理远程主机（SSH）
- 查看容器列表与状态
- 启动容器
- 进入容器命令行（WebSocket 命令会话）
- 删除容器
- 深度删除容器（包含卷清理与残留清理 best-effort）

## 技术栈
- 桌面端：Tauri 2 + Vue 3 + TypeScript + Naive UI
- 后端：Kotlin + Ktor
- 测试：JUnit5 + MockK + Vitest
- 代码质量：ESLint + Prettier + Spotless(ktlint) + JaCoCo

## 本地开发
### 1) 启动后端
```bash
gradle -p backend run
```

### 2) 启动前端
```bash
npm --prefix desktop install
npm --prefix desktop run dev
```

## 质量脚本
### 前端
- `npm --prefix desktop run format:check` 仅校验不改写
- `npm --prefix desktop run format:fix` 校验并改写

### 后端
- `gradle -p backend spotlessCheck` 仅校验不改写
- `gradle -p backend spotlessApply` 校验并改写

## 说明
- 当前仓库重视可扩展设计，核心能力通过接口分层，方便后续增加 Compose、镜像管理、多运行时适配器。
- 中文注释规范见：`docs/comment-style.zh-CN.md`
