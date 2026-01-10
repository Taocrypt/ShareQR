# ShareQR

一个轻量分享→二维码工具：从系统分享面板唤起，自动提取分享文本/链接，立即复制到系统剪贴板、生成二维码弹窗并可一键保存图片。基于 Material Design 3 视觉，Android 12+ 支持动态取色与背景高斯模糊。

## 功能特性
- 系统分享唤起：Manifest 使用 `ACTION_SEND + */*`，覆盖常见分享来源
- 自动复制剪贴板：收到分享内容即复制并 Toast 提示
- 文本预览：底部文本框展示已复制内容（可选中、最多 4 行）
- 二维码生成：后台线程生成，像素数组直写，速度更快
- 视觉风格：MD3 + 动态取色；半透明遮罩 + 自适应圆角卡片；背景高斯模糊（Android 12+）
- 点击外部关闭：点击弹窗外（模糊层）自动关闭
- 体积优化：限制资源语言（zh/en），Release 预设 R8 + shrinkResources

## 安装与使用
1. 安装 APK（见发布包或自己构建）
2. 在任意 App 选择“分享”→ 选择“ShareQR”
3. 弹窗中会：
   - 自动复制分享内容到剪贴板并提示
   - 生成二维码；可点击“保存到本地”将二维码保存到相册
   - 底部文本框展示已复制的内容
4. 点击弹窗外部区域可直接关闭

## 构建环境
- JDK 17
- Gradle 8.7（项目已包含 Gradle Wrapper）
- Android SDK：platforms;android-34 / build-tools;34.0.0

## 本地构建
```bash
# 1) 准备 Android SDK 并同意 license（略）
# 2) 在项目根目录（ShareQR/ShareQR）执行：
./gradlew assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

## 关键配置
- 包名（applicationId）：`com.taocrypt.shareqr`
- 入口 Activity：`ShareQrActivity`（继承 `ComponentActivity`）
- 动态取色：`ShareQrApp` 中 `DynamicColors.applyToActivitiesIfAvailable(this)`
- 自适应图标：`mipmap-anydpi-v26/ic_launcher.xml`，前景位于 `drawable-nodpi/ic_launcher_foreground.xml`（Inset 缩放避免“撑出”）

## 自定义
- 图标：替换 `drawable-nodpi/ic_launcher_foreground_png.png` 并按需调整 `ic_launcher_foreground.xml` 的 inset（默认 12dp）
- 遮罩/模糊强度：见 `ShareQrActivity` 中 `blurLayer` 相关代码；Android 12- 自动降级为半透明遮罩
- 文本框表现：`res/layout/activity_share_qr.xml` 中 `tvCopied` 样式可改为更大字号/更多行

## License
MIT