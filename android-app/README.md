# SignalTrace Portfolio Android App

离线优先、可持续成长的个人原生 App。当前版本为`0.7.1`，包含：

- 风格化动态名片与三维信号场。
- 实习总览、现场日志、作品、能力图谱四个内部子界面。
- 氢气纯度仪双数据集响应曲线、触摸读数、指标和双重停止判定。
- 本地原始视频证据入口。
- 可完成、追加和删除本地条目的人生愿望清单。
- 几何翻转导航、卡片展开、页面转场和按压反馈。
- 成长资源包与APK能力包的分阶段更新仓库。
- 启动异常恢复页、崩溃诊断复制和不影响私人数据的缓存恢复。

## 更新机制

- 首次离线：读取APK内置`assets/portfolio.json`。
- 清单检查：读取`app-update.json`中的渠道、版本、下载地址、大小和摘要。
- 资源包更新：下载到暂存区，执行SHA-256与JSON结构验证，再原子切换到活动版本。
- APK更新：在App内下载并校验后调用Android系统安装器；系统确认不可绕过。
- 定时更新：`JobScheduler`在有网络时周期检查资源包。
- 失败保护：请求失败、结构错误、摘要不一致或文件超限时不触碰当前活动版本。
- 隐私隔离：原始视频、本地愿望和完成状态只保存在设备中。
- 手动兜底：可在“更新”页导入本地`portfolio.json`或恢复内置安全版本。

## 发布前配置

确认网站已发布以下文件，然后检查`app/src/main/res/values/sync_config.xml`：

```text
https://YOUR-DOMAIN/content/portfolio.json
https://YOUR-DOMAIN/content/app-update.json
```

`app-update.json`的`contentPack.sha256`必须匹配公开`portfolio.json`，`apk.sha256`必须匹配Release中的APK；两个摘要不匹配都会中止更新。

## 构建

在 Android Studio 打开 `android-app`，选择 Android 8.0（API 26）或更高版本的设备并运行 `app`。

命令行 debug 输出：`app/build/outputs/apk/debug/app-debug.apk`。
