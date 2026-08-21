# Heartbeat Fixer for FCM - 有效性检查脚本
#
# 用途: 通过 adb 一键检查 app 是否正常工作(闹钟是否排上、权限、Doze 白名单等)。
# 用法: 手机开启 USB 调试并连接电脑后, 在 PowerShell 里运行:
#     .\scripts\verify.ps1
#
# 可选: 传入 adb 路径, 默认使用 D:\android-sdk\platform-tools\adb.exe
param(
    [string]$Adb = "D:\android-sdk\platform-tools\adb.exe"
)

$ErrorActionPreference = "Stop"
$pkg = "io.github.grimseraph.heartbeatfixerforgcm"

function Section($title) {
    Write-Host ""
    Write-Host "=== $title ===" -ForegroundColor Cyan
}

# 0. 设备连接检查
Section "设备连接"
$devices = & $Adb devices | Select-String "\sdevice\s*$"
if (-not $devices) {
    Write-Host "未检测到设备。请确认已用 USB 连接手机并开启 USB 调试。" -ForegroundColor Red
    exit 1
}
& $Adb devices -l

# 1. 是否已安装
Section "是否已安装"
$installed = & $Adb shell pm list packages | Select-String $pkg
if ($installed) {
    Write-Host "已安装: $pkg" -ForegroundColor Green
} else {
    Write-Host "未安装 $pkg，请先安装 release APK。" -ForegroundColor Red
    exit 1
}

# 2. 精确闹钟权限 (Android 12+)
Section "精确闹钟权限 (SCHEDULE_EXACT_ALARM)"
$exact = & $Adb shell "appops get $pkg SCHEDULE_EXACT_ALARM"
Write-Host $exact
if ($exact -match "allow") {
    Write-Host "已授予: 心跳会精确触发。" -ForegroundColor Green
} else {
    Write-Host "未授予: 心跳会走非精确降级模式。可在 app 内点『Exact alarm permission』授予," -ForegroundColor Yellow
    Write-Host "        或运行: adb shell appops set $pkg SCHEDULE_EXACT_ALARM allow" -ForegroundColor Yellow
}

# 3. 电池优化白名单 (Doze)
Section "电池优化白名单 (Doze whitelist)"
$whitelist = & $Adb shell "dumpsys deviceidle whitelist" | Select-String $pkg
if ($whitelist) {
    Write-Host "已在白名单: Doze 深度休眠下也能触发。" -ForegroundColor Green
} else {
    Write-Host "不在白名单: Doze 下心跳可能被延迟。建议在 app 内点『Battery optimization』设为不受限制。" -ForegroundColor Yellow
}

# 4. 心跳闹钟是否已排上 (核心)
Section "心跳闹钟 (RTC_WAKEUP + HeartbeatReceiver)"
$alarm = & $Adb shell "dumpsys alarm | grep -B2 -A6 HeartbeatReceiver"
if ($alarm) {
    Write-Host $alarm
    Write-Host ""
    if ($alarm -match "\*walarm\*") {
        Write-Host "OK: 闹钟已排上, 且为 while-idle 精确闹钟 (可穿透 Doze)。" -ForegroundColor Green
    } else {
        Write-Host "OK: 闹钟已排上。" -ForegroundColor Green
    }
    if ($alarm -match "exactAllowReason=permission") {
        Write-Host "OK: 系统确认正在使用已授予的精确闹钟权限。" -ForegroundColor Green
    }
} else {
    Write-Host "未查到 pending 心跳闹钟。请确认 app 内开关已打开。" -ForegroundColor Red
}

Section "说明"
Write-Host "以上检查证明 app 本身正常发出心跳请求。心跳广播发给 Google Play"
Write-Host "services 后是否真的去 ping 推送服务器, 属于 Google 内部黑盒, adb 无法观测。"
Write-Host "要验证最终推送延迟改善, 需做端到端对照: 关闭 fixer 锁屏静置 30 分钟收一条"
Write-Host "FCM 推送记延迟, 再开启 fixer 重复对比。"
