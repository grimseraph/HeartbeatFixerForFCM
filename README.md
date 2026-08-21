## Heartbeat Fixer for FCM

> **Forked from** [shaobin0604/HeartbeatFixerForGCM](https://github.com/shaobin0604/HeartbeatFixerForGCM). Original © 2015 Bin Shao; modernized & maintained by [@grimseraph](https://github.com/grimseraph).

Have you ever experienced **Push Notification Delay**, missing something important? Here is the tool for fixing the FCM (formerly GCM) heartbeat interval issue.

> **2026 update:** this project has been modernized. GCM was shut down in 2019, but its successor FCM (Firebase Cloud Messaging) uses the same persistent connection inside Google Play services, and the same heartbeat trick still applies.

### The Root Cause

Android keeps a single long-lived TCP connection to Google's push servers and pings it at a fixed interval (up to 28 minutes on mobile networks). On some networks (especially carrier NAT / aggressive WiFi routers), the connection is silently dropped long before the next heartbeat, so pushes are delayed until the system notices the dead connection.

### How Heartbeat Fixer resolves this issue

It sends a heartbeat request (`GTALK_HEARTBEAT` / `MCS_HEARTBEAT` broadcast to Google Play services) every x minutes, where you can choose the interval. Setting it to 5 minutes will keep alive the FCM connection used for push notifications.

### What was modernized (v2.0)

- Gradle 8.7 + Android Gradle Plugin 8.5, `compileSdk`/`targetSdk` 34, `minSdk` 21, AndroidX + Material 3
- Heartbeat broadcasts are now sent **explicitly** to `com.google.android.gms` / `com.google.android.gsf` (implicit broadcasts no longer reach other apps since Android 8.0)
- `setExactAndAllowWhileIdle()` alarms with **exact-alarm permission** handling on Android 12+, and a graceful inexact fallback
- In-app shortcuts to grant the exact-alarm permission and exempt the app from **battery optimization** (needed for reliable heartbeats in Doze mode)
- Reschedules automatically after reboot and app updates
- Removed all obsolete baggage: ads, in-app billing, Firebase Analytics, jcenter-only libraries (Crouton, Calligraphy, etc.)

### Note on modern Android

On Android 6.0+ Doze mode batches alarms; even exact "while idle" alarms are limited to roughly one per 9 minutes per app. For the most reliable results:

1. Enable the fixer and grant the exact-alarm permission when prompted
2. Tap *Battery optimization* in the app and set it to *Unrestricted*

## Build & Release

### Prerequisites

- Android SDK with `compileSdk` 34 + build-tools, and **JDK 17**
- A signing keystore (see below); the repo does **not** contain one

### Local build

Debug (unsigned, for on-device testing):

```bash
./gradlew assembleDebug
# or install straight to a connected device/emulator
./gradlew installDebug
```

Release (signed). The build reads `<project-root>/keystore.properties`, which is git-ignored and never committed:

```properties
storeFile=release.keystore
storePassword=*****
keyAlias=*****
keyPassword=*****
```

Drop your `release.keystore` next to it, then:

```bash
./gradlew assembleRelease
```

The signed APK lands in `app/build/outputs/apk/release/` as `HeartbeatFixerForFCM-v2.0.0.apk` (the file name embeds the version).

### One-click release via GitHub Actions

Pushing a tag such as `v2.0.0` (or running the workflow manually from the **Actions** tab) builds a signed release APK on GitHub and publishes it as a GitHub Release.

The signing keystore is reconstructed in CI from repository **secrets** — it is never stored in the repo. Add these four secrets under **Settings → Secrets and variables → Actions**:

| Secret | Value |
| --- | --- |
| `KEYSTORE_BASE64` | base64 of your `release.keystore` (single line, no line wraps) |
| `KEYSTORE_PASSWORD` | keystore store password |
| `KEY_ALIAS` | key alias |
| `KEY_PASSWORD` | key password |

To produce `KEYSTORE_BASE64` locally (from the project root, where `release.keystore` lives):

```bash
base64 -w0 release.keystore
```

Copy the output — one long line — into the `KEYSTORE_BASE64` secret. Then tag and push to trigger the build:

```bash
git tag v2.0.0
git push origin v2.0.0
```

Watch the run under the **Actions** tab; when it finishes, the signed `HeartbeatFixerForFCM-v2.0.0.apk` is attached to the release on the **Releases** page.

### Known limitations

- **Effectiveness is empirical.** The heartbeat intents (`GTALK_HEARTBEAT` / `MCS_HEARTBEAT`) are internal, undocumented Google actions. Whether the current Google Play services version still honors them is not guaranteed — the only real proof is to measure push-delivery latency with the fixer on vs. off.
- **Force-stop breaks the chain.** If the system or a third-party task killer force-stops this app, the alarm chain stops and will not restart until the next reboot or app update (`MY_PACKAGE_REPLACED`). Keep *Battery optimization* set to *Unrestricted* and never manually force-stop the app.
- **Requires Google Play services.** On devices without GMS installed there is no connection to keep alive, so the tool has no effect. It targets devices that ship with (or have flashed) GMS but suffer aggressive push killing by the local ROM.
- **Exact-alarm permission matters.** On Android 12+, if the exact-alarm permission is not granted, heartbeats fall back to inexact `setAndAllowWhileIdle` scheduling and may be delayed by Doze batching (roughly one alarm per 9 minutes per app).

License
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
