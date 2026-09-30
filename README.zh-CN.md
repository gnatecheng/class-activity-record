[English](README.md) | **中文**

[主页：Etai 应用集 — 团团记](https://etais.dev/#group-matters)

# 团团记

面向任意小团体的 Android 事务记录应用：出勤、缴费、费用分摊、清单。数据保存在本机（Room），无需登录或联网。

- **应用名称**：团团记（英文界面：**Group Matters**）
- **applicationId**：`com.classrecord.app`
- **最低系统**：Android 8.0（API 26）
- **版本**：1.5.2（versionCode 9）
- **界面语言**：简体中文 / English（设置内可切换）

## 功能

- 一个团体：可编辑团体名称
- 成员增删改、软归档（编号、备注可选）；批量粘贴导入名单
- 团内小团体：多选成员，一人可属于多个小团体；空团体不能作为事务范围
- 四种事务：出勤 / 缴费 / 分摊 / 清单
- 事务范围：全团或某个小团体；创建时快照成员，之后改团体名单不影响历史
- 金额以「分」存储，界面以「元」显示；分摊余数补给前 N 人
- 事务详情默认「仅未完成」，点按更新状态；缴费/分摊可编辑金额
- 复制摘要 / 催缴文案（可分享）；再开一期按当前名单重新快照
- 团费账本：收入/支出流水与余额；缴费事务可将已收合计记入团费
- 数据备份 / 恢复（zip，含缴费凭证图片）；导出成员、事务进度、账本 CSV
- 分摊可排除成员、按权重或固定金额；缴费/分摊可附加备注与图片凭证
- 名单按编号排序（可关）；出勤事务支持连续点名
- 桌面小组件：显示近期进行中事务的未完成人数（多项时附全部合计），点按打开应用
- 首页按标题搜索事务，筛选进行中 / 已归档；详情菜单可归档或取消归档
- 深色主题：Material 3，可跟随系统或在设置中固定浅色 / 深色（DataStore）
- 催缴文案按微信群可读格式（一人一行、含金额）；详情可复制/分享未完成名单与 CSV
- 批量导入支持编号在前、Tab 粘贴，重名/同编号会提示；可恢复已归档成员且保留历史事务
- 新建事务与再开一期提供点名/团费/场地分摊等快捷标题模板
- **设置 → 关于**：版本号、构建时间、GitHub 仓库链接

## 更新日志

### 1.5.2

- 中文应用名 **团团记**（多人事务更名为团团记；英文 **Group Matters** 不变）
- 统一 **团费账本 / 团费** 用语（原「班费账本 / 班费」）
- 新启动图标（「团」字）；首页截图集已重新生成
- 界面用语改为通用 **团体 / 成员**（不再使用班级、学号等场景化文案）；示例数据改为俱乐部式小团体

### 1.5.1

- 英文文案全面改写（更自然的团体/成员事务用语）
- 修复 Android 13/14 应用内语言切换（AppCompat 应用语言 + `localeConfig`）
- 示例团体按当前界面语言加载（仅空数据库时可用）
- 英文界面金额显示为 ¥，不再出现裸「元」

### 1.5.0

- 应用更名为「多人事务」/ **Group Matters**
- 设置内可切换简体中文 / English
- 深色主题：跟随系统或固定浅色 / 深色
- 设置内新增「关于」页（版本、最后更新、GitHub）

## 本地运行

需要 JDK 17+ 与 Android SDK。在项目根目录创建 `local.properties`：

```
sdk.dir=/path/to/Android/Sdk
```

然后：

```bash
./gradlew assembleDebug
```

Debug APK 输出：

```
app/build/outputs/apk/debug/app-debug.apk
```

## 安装

Debug 包已用调试密钥签名，可直接侧载。手机需允许「未知来源」或「安装未知应用」。

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

也可把 APK 传到手机后打开安装。桌面小组件在系统桌面小部件列表中名为「未完成人数」。

## GitHub 发布

推送 **semver 标签** `vX.Y.Z`，且须与 `app/build.gradle.kts` 中的 `versionName` **完全一致**（例如 `versionName = "1.5.1"` 时标签为 `v1.5.1`）。**Release APK** 工作流会构建 **已签名、不可调试** 的 release 包并发布附件：

`group-matters-<versionName>.apk`（例如 `group-matters-1.5.1.apk`）。

下载入口：[Releases](https://github.com/gnatecheng/group-matters/releases/latest)。

手动 **workflow_dispatch** 仅用于验证签名 release 构建，**不会**创建 GitHub Release。缺少签名 Secrets 时工作流会 **明确失败**，不会发布 debug 包。

### Release 签名与 GitHub Secrets

GitHub Actions [`.github/workflows/release-apk.yml`](.github/workflows/release-apk.yml) 使用仓库 Secrets 中的发布 keystore 执行 `assembleRelease`。若缺少必需 Secrets，工作流 **失败并提示**——不会发布未签名或 debug APK。

| Secret | 说明 |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | 发布 keystore 文件的 Base64 |
| `ANDROID_KEYSTORE_PASSWORD` | keystore 密码 |
| `ANDROID_KEY_ALIAS` | 密钥别名 |
| `ANDROID_KEY_PASSWORD` | （可选）密钥密码；省略则使用 store 密码 |

在本地生成 keystore（**切勿提交**）：

```bash
keytool -genkey -v -keystore release.keystore -alias group-matters \
  -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 release.keystore   # 填入 ANDROID_KEYSTORE_BASE64
```

本地 **debug** 构建（`assembleDebug`）无需上述变量。本地 signed release：设置 `ANDROID_KEYSTORE_FILE`、`ANDROID_KEYSTORE_PASSWORD`、`ANDROID_KEY_ALIAS`，可选 `ANDROID_KEY_PASSWORD`（见 `app/build.gradle.kts`）。

### 从 debug 包迁移到 signed Release

Release APK 使用 **发布证书** 签名，**无法直接覆盖安装** 在同一 `applicationId`（`com.classrecord.app`）下的 **debug 签名** 旧版本。建议步骤：

1. 在应用内 **设置 → 备份到 zip**。
2. 安装新的 **signed release** APK（若系统阻止更新，可先卸载 debug 包）。
3. 在设置中 **从备份恢复**。

## 技术栈

Kotlin · Jetpack Compose · Material 3 · Navigation Compose · Room · DataStore · App Widget · Flow · Coroutines · MVVM

## 许可证

MIT — 见 [LICENSE](LICENSE)。
