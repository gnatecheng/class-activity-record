# 班级事务记录

单班事务记录 Android 应用：出勤、缴费、费用分摊、清单。数据保存在本机（Room），无需登录或联网。

- **应用名称**：班级事务记录
- **applicationId**：`com.classrecord.app`
- **最低系统**：Android 8.0（API 26）
- **版本**：1.3.0（versionCode 5）
- **界面语言**：简体中文

## 功能

- 一个班级：可编辑班级名称
- 成员增删改、软归档（学号、备注可选）；批量粘贴导入名单
- 班内小团体：多选成员，一人可属于多个团体；空团体不能作为事务范围
- 四种事务：出勤 / 缴费 / 分摊 / 清单
- 事务范围：全班或某个小团体；创建时快照成员，之后改团体名单不影响历史
- 金额以「分」存储，界面以「元」显示；分摊余数补给前 N 人
- 事务详情默认「仅未完成」，点按更新状态；缴费/分摊可编辑金额
- 复制摘要 / 催缴文案（可分享）；再开一期按当前名单重新快照
- 班费账本：收入/支出流水与余额；缴费事务可将已收合计记入班费
- 数据备份 / 恢复（zip，含缴费凭证图片）；导出成员、事务进度、账本 CSV
- 分摊可排除同学、按权重或固定金额；缴费/分摊可附加备注与图片凭证
- 名单按学号排序（可关）；出勤事务支持连续点名
- 桌面小组件：显示近期进行中事务的未完成人数（多项时附全部合计），点按打开应用
- 首页按标题搜索事务，筛选进行中 / 已归档；详情菜单可归档或取消归档
- 深色主题：Material 3，可跟随系统或在设置中固定浅色 / 深色（DataStore）

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

## 技术栈

Kotlin · Jetpack Compose · Material 3 · Navigation Compose · Room · DataStore · App Widget · Flow · Coroutines · MVVM
