# 心绪日记 · Kotlin 离线安卓应用

一个可以在飞行模式下使用的个人情绪记录工具，支持 Android 8.0（API 26）及以上。采用 Kotlin、Jetpack Compose、Room、DataStore、ViewModel 和 StateFlow。没有后端或网络权限。

## 已实现

- 首页快速记录：整体感受 1–5 分、多情绪选择、每个情绪独立强度。
- 编辑、补记日期和时间、场景标签、事件/想法/需要文字记录。
- 图片、实况原图、视频附件，最多 9 个，每个最多 200 MiB；导入后复制到应用私有目录，离线保留。实况长按播放，照片双指缩放，视频支持播放/暂停。
- 16 条原创温柔文案，每次打开/回到前台轮换，进度保存在本机。
- Sketch 原创图标和微风兔插画；页面轻移淡入、按压回弹、弹窗进出动画。
- 删除记录与短暂撤销、关键词搜索、日历和情绪/场景筛选。
- 7/30 天整体感受趋势、情绪次数、常见场景和练习回顾。无记录日期保留空白。
- 呼吸节奏动画（1/2/3 分钟，吸气线性渐强、呼气线性渐弱的连续振动，默认关闭，羽毛/微风/轻拂三档，记住主动选择）、四步情绪整理、感官短暂停顿；练习前后主观强度记录。
- 浅色/深色/系统主题、自定义场景标签。
- 使用系统设备凭据的应用锁；启用时阻止截屏和最近任务预览。
- 可选每日本地通知；Android 13+ 请求通知权限。使用非精确闹钟，可能被省电策略延迟。
- 密码加密备份与事务恢复。恢复替换情绪、练习和全部附件数据，主题、提醒、应用锁及自定义标签设置不随备份迁移；历史记录里的标签保留。

## 安装

从源码构建调试 APK，将其传到安卓手机，允许对应文件管理器安装应用后打开。当前交付是使用 Android 调试证书签名的测试版，不适合直接作为应用商店发布包。首次安装没有示例数据。

## 构建

1. 使用 JDK 17、Android SDK Platform 35 和 Android SDK Build Tools 35。
2. Android Studio 打开本目录，在 Gradle JDK 设置中选择 JDK 17。
3. `local.properties` 中设置本机 `sdk.dir`，或设置 `ANDROID_HOME`。此文件不进入源码包。
4. 运行 `./scripts/build.sh`，或设置 `JAVA_HOME` 后执行：

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。Gradle Wrapper 已随项目提供。第一次下载构建依赖需要网络；安装后的应用运行完全离线。仓库中的 `.tools` 为本机验证环境，不属于源码交付。

只在独立测试模拟器中运行仪器测试。Gradle 的 connected 测试任务可能卸载目标应用，请不要对保存个人日记的真机执行。确认模拟器序列号后运行：

```bash
ANDROID_SERIAL=emulator-5556 ./scripts/build.sh :app:connectedDebugAndroidTest --no-parallel --max-workers=1
```

不要用 `pm clear` 测试保留数据，也不要随意更换签名证书；卸载会删除本机记录。测试逻辑使用独立测试数据库；这不能阻止 Gradle 测试安装器卸载主应用。保留真机数据应使用同签名 `adb install -r` 升级。

## 项目结构

- `app/src/main/java/com/xinxu/mood/ui`：四个页面、记录编辑器、练习、主题与组件。
- `data`：Room 实体与 DAO、DataStore 设置、按本机时区聚合的趋势算法。
- `backup`：版本兼容的 JSON、ZIP 附件归档、分块 AES-GCM 加密与事务恢复。
- `media`：原图/视频复制、Motion Photo 解析、附件引用校验与孤立文件清理。
- `design`：可编辑 Sketch 源文件、SVG/PNG 导出、可复现设计脚本。
- `MainActivity`：系统文件选择器、设备凭据、通知权限与应用锁生命周期。
- `ReminderReceiver`：本地提醒和开机后重新安排提醒。
- `app/schemas`：Room 数据库版本快照；升级数据结构时应增加数据库版本并提供迁移。

情绪及场景为体积较小的字符串字段；情绪名称取固定集合，场景支持自定义。Repository 目前用于设置，业务记录通过 DAO 和 ViewModel 连接，保持单模块实现。

## 数据保护与恢复

Room 数据库保存在应用私有目录；数据库本身未额外加密。系统云备份和设备迁移已排除应用数据。应用锁依赖设备已设置锁屏密码，备份保护独立于应用锁。

`.xinxu` 文件格式：8 字节版本标识 `XINXU001`、16 字节随机 salt、12 字节随机 nonce、AES-256-GCM 密文与 128 位认证标签。使用 PBKDF2-HMAC-SHA256 150,000 次派生，每次备份随机化 salt/nonce。密码至少 8 位，遗忘后无法恢复。这是旧版格式，新版仍支持导入。

新版 `.xinxu` 使用 `XINXU002`：ZIP 内含 JSON 和原始附件，以 64 KiB 帧分别进行 AES-256-GCM 认证加密，序号、长度、结束标识绑定到认证数据；独立的认证结束帧防止截断。PBKDF2 参数沿用 150,000 次，随机 salt 16 字节、nonce 前缀 8 字节加 4 字节帧序号。归档上限 512 MiB，避免整段视频装入内存。

恢复先完成全部解密认证，再在缓存目录验证、解包和检查附件引用；媒体使用新 UUID 文件名，全部文件就绪后才在 Room 事务中替换记录。校验失败不修改原数据；事务写入失败删除新文件并回滚记录。未引用的旧附件和取消导入产生的文件在下一次打开时清理，保留 24 小时以便撤销；清空记录同时清理附件。媒体在应用私有目录中以原始文件保存，未另做静态加密。

## 当前验证范围

详见 `docs/验收记录.md`。尚需在实际目标手机上验证通知到达、系统凭据交互、文件提供程序兼容性及长期使用体验。当前锁使用系统设备凭据，未加入独立 PIN 或应用内指纹提示。统计和练习用于自我观察，不提供疾病诊断。

## 页面预览

[新版首页](docs/screenshots/v1.1.0/home-emulator.png) · [新版媒体记录](docs/screenshots/v1.1.0/media-emulator.png) · [趋势](docs/screenshots/trends.png) · [深色设置](docs/screenshots/settings-dark.png)

## 1.0.2 更新

呼吸训练使用连续渐变振动：4 秒吸气振幅线性渐强，6 秒呼气振幅线性渐弱，循环衔接不插入停振。每 50 ms 一个振幅采样，范围 8–48；圆形动画使用同一进度。使用有明确结束时间的 waveform，不无限重复；关闭开关、结束、离开页面、锁屏或退到后台时主动 cancel。回到前台按当前训练时间继续，不从第一口呼吸重播。

需要 VIBRATE 普通权限和硬件振幅控制；不支持的设备会显示提示并禁用开关，不替换为恒定振动。实际振动效果及是否使用线性马达由手机硬件决定。系统省电、振动策略仍可能限制输出。开关选择保存在本机。

实现参考：[Android 振幅波形 API](https://developer.android.com/reference/android/os/VibrationEffect#createWaveform(long[],%20int[],%20int))、[振幅控制能力](https://developer.android.com/reference/android/os/Vibrator#hasAmplitudeControl())。

[呼吸训练预览](docs/screenshots/breath-haptics.png)

## 1.0.3 更新

根据真机反馈，将连续渐变振幅从 24–144 降为 8–48（指令振幅约为之前三分之一），保持 4 秒渐强、6 秒渐弱。实际感受由马达与系统策略决定。

## 1.1.0 更新

- 默认关闭轻振，包括从旧版升级。使用新偏好键，不继承旧版自动开启状态。
- 连续线性包络保持 4 秒吸气、6 秒呼气；羽毛档指令振幅 1–3，微风 1–6，轻拂 1–10（Android 指令范围 1–255）。通过 TOUCH 用途请求交互反馈，避免未指定用途时使用通知类强度。系统与马达会影响实际体感，应用不能强制改变马达类型或保证所有机型同样轻柔。
- Android 标准 Motion Photo 的 XMP 偏移、Container Item 长度和 JPEG 尾部 MP4 均可识别；厂商私有格式不保证识别。无法识别动态部分时可以配对一段不超过 15 秒的短视频，或保留普通照片。需从系统文件选择器选原图；相册导出普通照片可能丢失动态部分。
- 照片和视频通过系统文件选择器导入，不申请整个相册的读取权限；实况和视频都在本地播放。
- Room 从版本 1 迁移到 2，仅新增默认空附件字段，保留现有文字与练习记录。
- 设计源文件：`design/心绪-微风花园.sketch`。32 个资产包含 31 个图标和 1 个微风兔，Sketch 内是可编辑矢量，应用使用对应的 PNG 导出，SVG 同时交付。

格式实现参考：[Android Motion Photo 格式](https://developer.android.com/media/platform/motion-photo-format)、[触觉用途与振幅控制](https://developer.android.com/develop/ui/views/haptics/haptics-apis)。

## 开源许可

本项目采用 [MIT License](LICENSE)，包含源码及原创设计素材。
