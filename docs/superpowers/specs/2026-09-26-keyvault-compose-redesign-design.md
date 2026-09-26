# KeyVault 全面升级（Compose 重构）设计规格

日期：2026-09-26
状态：已获用户批准（"可以，一起按你的建议来"）

## 目标

把 KeyVault（本地加密的密钥 + 记事本 Android App）从传统 XML 视图全面重构为
Jetpack Compose，视觉采用「玻璃霓虹」风格，补齐安全与组织类功能，并保证
数据兼容与可持续发版。

## 现有实现（重构基线）

- 语言/UI：Kotlin + Android View（XML 布局），Material3 深色主题。
- 包名 / 应用名：`com.keyvault.app` / KeyVault。`minSdk 26`，`compile/target 35`，Java 17。
- 数据：`filesDir` 下 `meta.json`（version/kdf/iterations/salt）与 `vault.bin`（密文）。
- 加密：PBKDF2-HMAC-SHA256（210,000 次）派生 AES-256 密钥；AES/GCM/NoPadding，
  密文格式 = 12 字节随机 IV + GCM 密文（含 128-bit tag）。
- 数据类型：`Vault { keys: List<KeyItem>, notes: List<NoteItem> }`，序列化为 JSON 后加密。
- 标签：`TagParser` 按逗号拆分去重。
- 测试：`VaultCryptoTest`、`VaultRepositoryTest`、`TagParserTest`、`KeyGeneratorTest`、`SearchFilterTest`。
- CI：`.github/workflows/build.yml`（JDK17 + Gradle 8.11.1）跑单测、出 debug APK、发 Release。

## 决策（已确认）

- 范围：全面重构到 Compose，重做 UI/交互并补齐功能。
- 功能：生物识别解锁、自动锁定、加密导出/导入/备份、分类与置顶、搜索增强、
  密码强度检测、主题（深/浅/跟随系统 + 强调色）、保留全部现有功能。
- 视觉：玻璃霓虹（深色渐变 + 半透明玻璃卡片 + 柔光描边，紫青渐变强调色）。
- 导航：底部导航 4 区（密钥 / 笔记 / 生成器 / 设置）+ 悬浮新建按钮。
- 解锁：主密码为主 + 生物识别快捷解锁，失败回退主密码。
- 数据：一次性迁移，**加密格式与文件格式保持不变**。
- 包名/应用名不变，`versionCode` 递增，保证覆盖安装。
- 交付：CI 出包；由本机用固定 keystore 重新签名后放入 `/workspace/.andcode/apks/`。

## 架构

- 单模块 `app`。
- 技术栈：Kotlin + Jetpack Compose（Material 3）、Navigation-Compose、
  ViewModel + StateFlow、Kotlin Coroutines；`androidx.biometric` 做生物识别。
- 分层：
  - `crypto/`：`VaultCrypto`（**保持不变**）。
  - `data/`：`VaultRepository`（**保持加密与文件格式**，新增 schema v2 可选字段与迁移）、
    `VaultItem`（新增 `pinned`/`favorite`/`category`）。
  - `di/`：轻量手写容器（不引第三方 DI）。
  - `ui/`：`theme/`（玻璃霓虹主题）、`components/`、`lock/`、`main/`、`keyedit/`、
    `noteedit/`、`generator/`、`settings/`、`export/`。
  - 保留 `Session`（内存中的 key + vault），由 ViewModel 持有并可清除。

## 数据与迁移

- `meta.json` 与 `vault.bin` 结构与加密参数**不变**。
- 数据 JSON 升级为 `schemaVersion = 2`，仅**新增可选字段**：
  - `KeyItem`：`pinned: Boolean = false`、`category: String = ""`。
  - `NoteItem`：`pinned: Boolean = false`、`favorite: Boolean = false`、`category: String = ""`。
- 读取：使用 `opt*`，旧数据缺字段时取默认值（向前兼容）。
- 写入：始终写 v2；`meta.version` 由 1 升为 2（仅版本标记，不影响解密）。
- 迁移在首次解锁加载后透明完成；迁移逻辑可单测（旧格式 fixture → 解锁 → 读 → 存 → 再读）。

## 界面设计

- 主题：玻璃霓虹；深色默认，支持浅色 / 跟随系统；强调色可切换（默认紫 `#7C5CFF` → 青 `#22D3EE`）。
- 解锁页：品牌标识 + 主密码输入（可显隐）+ 生物识别按钮；失败限速提示。
- 主界面：底部导航 4 区 + FAB 新建；每区顶部常驻搜索框；星标置顶；标签胶囊过滤。
- 密钥编辑：名称 / 值（可显隐、一键复制）/ 备注 / 标签 / 分类 / 置顶。
- 笔记编辑：标题 / 正文 / 标签 / 分类 / 置顶 / 收藏。
- 生成器：长度、字符集、排除易混字符；强度提示；一键复制/保存为密钥。
- 设置：自动锁定时长（立即 / 1 / 5 / 15 分钟）、生物识别开关、主题、导出/导入、关于。

## 安全与错误处理

- 自动锁定：切后台立即锁定（或按设定超时）；进程被杀后内存密钥不残留。
- 生物识别：`BiometricPrompt`，失败/不可用回退主密码。
- 剪贴板：复制敏感值后自动清除（默认 30 秒）。
- 文件写入保持原子（tmp + rename）。
- 解密失败：给出可读错误，不崩溃、不覆盖原文件。
- 日志不打印明文、密钥、密码。

## 导出 / 导入

- 导出：`.kvault` 文件 = 与库相同的加密 JSON（用主密码派生的密钥加密）。
- 导入：选择 合并 或 覆盖；导入前校验解密成功与 schema。

## 测试

- 保留现有单测并全部通过。
- 新增：迁移（v1→v2）、导出/导入往返、自动锁定状态、搜索/过滤（含标签与置顶）、
  密码强度、加解密不变性（固定向量）。

## 构建与交付

- CI：`testDebugUnitTest` → `assembleDebug` → 上传 APK artifact → 发 Release。
- 签名：本机生成固定 keystore；CI 产物下载后由本机 `apksigner` 重新签名（v2/v3），
  再上传回 Release 并复制到 `/workspace/.andcode/apks/`。
- 版本：`versionCode` 1 → 2，`versionName` 1.0 → 2.0。
- 后续如具备 Secrets 写权限，再改为 CI 内直接固定签名。

## 验收标准

1. 编译通过，单测全绿。
2. 产出可安装的签名 APK，放在 `/workspace/.andcode/apks/`。
3. 旧数据（v1 格式）能被新版读取，保存后升级为 v2 且数据不丢。
4. 主密码解锁、生物识别回退、自动锁定、搜索/置顶/标签、导出/导入可用。
5. 视觉为玻璃霓虹，深/浅色与强调色可切换。
