[简体中文](README_zh.md) · [English](README.md)

## Heartbeat Fixer for FCM（FCM 心跳保活工具）

> **Fork 自** [shaobin0604/HeartbeatFixerForGCM](https://github.com/shaobin0604/HeartbeatFixerForGCM)。原作者 © 2015 Bin Shao；由 [@grimseraph](https://github.com/grimseraph) 现代化维护。

你是否遇到过**推送通知延迟**、因此错过重要消息的情况？这款工具用于修复 FCM（前身是 GCM）的心跳间隔问题。

> **2026 更新：** 本项目已现代化。GCM 已于 2019 年关停，但其继任者 FCM（Firebase Cloud Messaging）在 Google Play 服务中沿用同一套长连接，同样的心跳技巧依然有效。

### 问题根源

Android 与 Google 推送服务器之间维持着一条长期存在的 TCP 连接，并以固定间隔（移动网络下最长可达 28 分钟）发送心跳。在某些网络环境（尤其是运营商 NAT / 激进的 WiFi 路由器）下，这条连接在下次心跳到来之前就被悄悄丢弃，于是推送会被延迟，直到系统察觉连接已死。

### Heartbeat Fixer 如何解决这个问题

它每隔 x 分钟向 Google Play 服务发送一次心跳请求（`GTALK_HEARTBEAT` / `MCS_HEARTBEAT` 广播），间隔可由你自行设定。设为 5 分钟就能保活用于推送通知的 FCM 连接。

### 现代化改动（v2.0）

- Gradle 8.7 + Android Gradle Plugin 8.5，`compileSdk`/`targetSdk` 34，`minSdk` 21，AndroidX + Material 3
- 心跳广播现在**显式**发送给 `com.google.android.gms` / `com.google.android.gsf`（自 Android 8.0 起，隐式广播不再送达其他应用）
- `setExactAndAllowWhileIdle()` 闹钟，配合 Android 12+ 的**精确闹钟权限**处理，并在未授权时优雅降级为不精确闹钟
- App 内提供快捷入口，用于授予精确闹钟权限、并将应用**排除出电池优化**（Doze 模式下稳定心跳所必需）
- 重启和应用更新后自动重新排程
- 移除了所有过时包袱：广告、应用内购买、Firebase Analytics、仅 jcenter 可用的库（Crouton、Calligraphy 等）

### 关于现代 Android 的说明

Android 6.0+ 的 Doze 模式会对闹钟进行批处理；即便是"空闲时精确"闹钟，每个应用也大约被限制为每 9 分钟一次。为获得最可靠的效果：

1. 开启本工具，并在提示时授予精确闹钟权限
2. 在 App 内点击*电池优化*，将其设为*无限制*

### 已知限制

- **效果需实测验证。** 心跳 intent（`GTALK_HEARTBEAT` / `MCS_HEARTBEAT`）是 Google 内部、未公开的 action。当前版本的 Google Play 服务是否仍响应它们并无官方保证——唯一可靠的证明，是开关本工具分别测量推送到达时延。
- **被强制停止即断链。** 如果系统或第三方清理工具 force-stop 本应用，闹钟链会停止，且不会自动重启，直到下次重启或应用更新（`MY_PACKAGE_REPLACED`）。请将*电池优化*设为*无限制*，且切勿手动强制停止本应用。
- **需要 Google Play 服务。** 未安装 GMS 的设备没有可保活的连接，工具毫无作用。它的目标设备是出厂预装（或自行刷入）了 GMS、却被本地 ROM 激进杀推送的机型。
- **精确闹钟权限很关键。** Android 12+ 上若未授予精确闹钟权限，心跳会降级为不精确的 `setAndAllowWhileIdle` 排程，可能被 Doze 批处理拖慢（每个应用大约每 9 分钟一次闹钟）。

许可证
-------

    Copyright 2015 Bin Shao
    Copyright 2026 grimseraph

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
