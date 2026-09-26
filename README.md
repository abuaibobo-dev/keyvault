# KeyVault 2.0

本地加密的密钥 + 记事本 Android App。主密码保护，数据以 AES-256-GCM 加密后落盘。

## v2 亮点

- 全新 **Jetpack Compose** 界面：玻璃霓虹风格，深色 / 浅色 / 跟随系统，强调色可切换。
- 底部导航四区（**密钥 / 笔记 / 生成器 / 设置**）+ 悬浮新建；每屏顶部常驻搜索。
- **生物识别解锁**（指纹 / 面容），失败或不可用时回退主密码。
- **自动锁定**：切后台即锁，或按设定超时（立即 / 1 / 5 / 15 分钟）。
- **加密导出 / 导入**（`.kvault`），支持合并或覆盖。
- 标签分类、**置顶 / 收藏**；跨密钥、笔记、标签的实时搜索。
- 密码生成器 + 强度检测；复制敏感值后约 30 秒自动清空剪贴板。

## 安全与数据格式

- 密钥派生：PBKDF2-HMAC-SHA256（210,000 次）。
- 加密：AES/GCM/NoPadding；密文 = 12 字节随机 IV + 密文（含 128-bit tag）。
- 文件：`filesDir/meta.json`（version/salt/iterations）+ `filesDir/vault.bin`（密文）。
- **格式与 v1 兼容**：v2 仅在内部 JSON 增加可选字段（`pinned` / `favorite` / `category`），
  旧数据可直接读取，保存后标记 `schemaVersion = 2`。
- 日志不打印明文、密钥或密码。

## 构建

- 走 GitHub Actions（`.github/workflows/build.yml`）：`testDebugUnitTest` → `assembleDebug` → 发 Release。
- 本机（aarch64 Android/PRoot）无法运行 aapt2，故不能本地构建。

## 签名

- 正式分发使用**固定 keystore** 以 `apksigner` 重新签名（APK Signature Scheme v2 + v3），
  保证后续版本可覆盖安装、数据不丢。
- keystore 与口令存放在工作区 `.secrets/`，**不入库**。
- 如需在 CI 内直接签名，请添加以下 GitHub Secrets：
  `KEYSTORE_BASE64`、`KEYSTORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD`。

## 版本

- `versionCode 2` / `versionName 2.0`（包名 `com.keyvault.app` 不变，可覆盖安装）。
