# 照片与机组信息补充说明

现场照片和机组编号由用户自行补充。应用必须在没有这些内容时也能完整运行。

## 建议目录

```text
field-media/public/
  boiler/
  turbine/
  environmental/
  instrumentation/
  overview/

data/public/site-overrides.json
```

原始照片先保存在公开仓库之外，例如 `D:\PrivateInternship\thermal-flow-lab\media-review`。确认可以公开并完成处理后，再复制到 `field-media/public`。

## 照片入库前检查

- 确认拍摄和公开许可，不使用工作群、内部系统或他人提供但未授权的图片。
- 裁掉或模糊人员正脸、胸牌、二维码、车牌、门禁和联系电话。
- 不显示 DCS 画面、实时参数、报警记录、内部图纸、网络设备和安全设施细节。
- 清除 EXIF 中的 GPS、设备序列号和拍摄者信息。
- 文件名使用内容而非内部位号，例如 `pressure-gauge-corrosion-01.jpg`。
- 记录拍摄日期、许可状态、处理动作和可使用场景。

## 机组编号覆盖格式

后续应用使用独立覆盖文件，不把编号写死在模型代码中：

```json
{
  "siteDisplayName": "某沿海火电厂",
  "units": [
    {
      "internalId": "unit-a",
      "displayName": "待补充机组",
      "publishApproved": false
    }
  ]
}
```

只有 `publishApproved` 明确为 `true` 的名称才能进入公开页面。未批准时显示“示例机组”或设备泛称。

