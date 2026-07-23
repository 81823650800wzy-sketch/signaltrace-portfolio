# 网页部署交接

## 构建产物

```powershell
npm install
npm run build
```

部署目录为 `dist/`。项目不使用环境变量、后端地址、API 密钥或数据库连接。

## 路由与验证

- 首页：`/`
- 流程实验室：`/#lab`
- 实习日记入口：外链到 `https://app-d0firfl6tsld.appmiaoda.com`

这是静态 Vite 站点，使用 hash 路由，因此部署平台不需要为前端路由配置重写规则。

## 发布前检查

1. 执行 `npm run build`，确认数据校验、TypeScript 和 Vite 构建全部通过。
2. 打开首页，检查“流程实验室”跳转和“实习日记系统”外链。
3. 在手机宽度检查无页面级横向滚动。
4. 不注入或上传实习日志、私有证据台账、照片原件、API 令牌或任何内部资料。
