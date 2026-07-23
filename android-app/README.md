# SignalTrace Portfolio Android App

离线优先、网络可更新的个人作品集 App。它展示个人定位、能力证据、项目成果和后续流程模型入口，不承担实习日记记录功能。

## 更新机制

- 首次离线：读取 APK 内置 `assets/portfolio.json`。
- 联网启动：从 `sync_config.xml` 配置的 HTTPS 地址拉取公开内容。
- 定时更新：Android `JobScheduler` 在有网络时周期检查。
- 失败保护：请求失败、结构错误或文件过大时继续使用最后成功缓存。
- 手动兜底：仍可在“更新”页导入本地 `portfolio.json`。
- APK 升级：读取 `app-update.json`，发现更高 `versionCode` 后显示发布页入口；安装由用户或应用商店确认。

## 发布前配置

确认网站已发布以下文件，然后检查 `app/src/main/res/values/sync_config.xml`：

```text
https://YOUR-DOMAIN/content/portfolio.json
https://YOUR-DOMAIN/content/app-update.json
```

## 构建

在 Android Studio 打开 `android-app`，选择 Android 8.0（API 26）或更高版本的设备并运行 `app`。

命令行 debug 输出：`app/build/outputs/apk/debug/app-debug.apk`。
