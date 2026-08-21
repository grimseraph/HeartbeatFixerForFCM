# 用 GDI+ 渲染 launcher PNG 兜底图 (API 21-25, 无自适应图标)
# 几何与 ic_launcher_foreground.xml 保持一致 (viewport 108)
Add-Type -AssemblyName System.Drawing

$resRoot = "D:\ai\qoder\heartbeat\HeartbeatFixerForGCM\app\src\main\res"
$indigo  = [System.Drawing.Color]::FromArgb(255, 63, 81, 181)   # #3F51B5
$white   = [System.Drawing.Color]::White
$pink    = [System.Drawing.Color]::FromArgb(255, 255, 193, 227) # #FFC1E3

# 心跳折线在 108 视口下的点
$pts108 = @(
    @(30,56),@(46,56),@(49,56),@(53,40),@(58,72),@(63,30),@(68,60),@(71,56),@(78,56)
)

$densities = @{ "mdpi"=48; "hdpi"=72; "xhdpi"=96; "xxhdpi"=144; "xxxhdpi"=192 }

function New-Icon([int]$size, [bool]$round) {
    $bmp = New-Object System.Drawing.Bitmap($size, $size)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.Clear([System.Drawing.Color]::Transparent)

    $s = $size / 108.0
    $bg = New-Object System.Drawing.SolidBrush($indigo)
    if ($round) {
        $g.FillEllipse($bg, 0, 0, $size, $size)
    } else {
        # 圆角方形, 圆角半径约 18%
        $r = [int]($size * 0.18)
        $path = New-Object System.Drawing.Drawing2D.GraphicsPath
        $d = $r * 2
        $path.AddArc(0, 0, $d, $d, 180, 90)
        $path.AddArc($size - $d, 0, $d, $d, 270, 90)
        $path.AddArc($size - $d, $size - $d, $d, $d, 0, 90)
        $path.AddArc(0, $size - $d, $d, $d, 90, 90)
        $path.CloseFigure()
        $g.FillPath($bg, $path)
        $path.Dispose()
    }
    $bg.Dispose()

    # 心跳折线
    $penW = [float]($size * 0.05)
    $pen = New-Object System.Drawing.Pen($white, $penW)
    $pen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
    $pen.EndCap   = [System.Drawing.Drawing2D.LineCap]::Round
    $pen.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round
    for ($i = 0; $i -lt $pts108.Count - 1; $i++) {
        $x1 = [float]($pts108[$i][0]   * $s); $y1 = [float]($pts108[$i][1]   * $s)
        $x2 = [float]($pts108[$i+1][0] * $s); $y2 = [float]($pts108[$i+1][1] * $s)
        $g.DrawLine($pen, $x1, $y1, $x2, $y2)
    }
    $pen.Dispose()

    # 广播信号弧 (右上方两道)
    $penW2 = [float]($size * 0.028)
    $pen2 = New-Object System.Drawing.Pen($pink, $penW2)
    $pen2.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
    $pen2.EndCap   = [System.Drawing.Drawing2D.LineCap]::Round
    $cx = 63.0 * $s; $cy = 48.0 * $s
    foreach ($rr in @(16.0, 24.0)) {
        $rad = $rr * $s
        $g.DrawArc($pen2, [float]($cx - $rad), [float]($cy - $rad), [float]($rad*2), [float]($rad*2), -70, 55)
    }
    $pen2.Dispose()

    $g.Dispose()
    return $bmp
}

foreach ($den in $densities.Keys) {
    $size = $densities[$den]
    $dir = Join-Path $resRoot "mipmap-$den"
    New-Item -ItemType Directory -Force -Path $dir | Out-Null

    $b1 = New-Icon $size $false
    $b1.Save((Join-Path $dir "ic_launcher.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $b1.Dispose()

    $b2 = New-Icon $size $true
    $b2.Save((Join-Path $dir "ic_launcher_round.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $b2.Dispose()

    Write-Host "generated mipmap-$den ($size px)"
}
Write-Host "DONE"
