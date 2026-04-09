Add-Type -AssemblyName System.Drawing

function New-RoundedRectPath {
    param(
        [float]$X,
        [float]$Y,
        [float]$Width,
        [float]$Height,
        [float]$Radius
    )

    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $diameter = $Radius * 2
    $path.AddArc($X, $Y, $diameter, $diameter, 180, 90)
    $path.AddArc($X + $Width - $diameter, $Y, $diameter, $diameter, 270, 90)
    $path.AddArc($X + $Width - $diameter, $Y + $Height - $diameter, $diameter, $diameter, 0, 90)
    $path.AddArc($X, $Y + $Height - $diameter, $diameter, $diameter, 90, 90)
    $path.CloseFigure()
    return $path
}

function Draw-MascotIcon {
    param(
        [int]$Size,
        [string]$OutputPath,
        [bool]$Round = $false
    )

    $bitmap = New-Object System.Drawing.Bitmap($Size, $Size)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $graphics.Clear([System.Drawing.Color]::Transparent)

    $blue = [System.Drawing.ColorTranslator]::FromHtml("#2D8CFF")
    $mint = [System.Drawing.ColorTranslator]::FromHtml("#3FBF8F")
    $white = [System.Drawing.Color]::White
    $dark = [System.Drawing.ColorTranslator]::FromHtml("#20303A")
    $orange = [System.Drawing.ColorTranslator]::FromHtml("#F08C00")
    $ice = [System.Drawing.ColorTranslator]::FromHtml("#DCEFFF")
    $scale = $Size / 108.0

    if ($Round) {
        $bgBrush = New-Object System.Drawing.SolidBrush($blue)
        $graphics.FillEllipse($bgBrush, 4 * $scale, 4 * $scale, 100 * $scale, 100 * $scale)
        $highlightBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(24, 255, 255, 255))
        $graphics.FillEllipse($highlightBrush, 10 * $scale, 8 * $scale, 56 * $scale, 34 * $scale)
    } else {
        $bgPath = New-RoundedRectPath -X (6 * $scale) -Y (6 * $scale) -Width (96 * $scale) -Height (96 * $scale) -Radius (22 * $scale)
        $bgBrush = New-Object System.Drawing.SolidBrush($blue)
        $graphics.FillPath($bgBrush, $bgPath)
        $highlightBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(20, 255, 255, 255))
        $graphics.FillEllipse($highlightBrush, 10 * $scale, 6 * $scale, 58 * $scale, 32 * $scale)
    }

    $fridgePath = New-RoundedRectPath -X (24 * $scale) -Y (18 * $scale) -Width (54 * $scale) -Height (70 * $scale) -Radius (10 * $scale)
    $fridgePen = [System.Drawing.Pen]::new($white, [float](4.2 * $scale))
    $graphics.DrawPath($fridgePen, $fridgePath)
    $dividerBrush = New-Object System.Drawing.SolidBrush($white)
    $graphics.FillRectangle($dividerBrush, 24 * $scale, 38 * $scale, 54 * $scale, 8 * $scale)
    $handleBrush = New-Object System.Drawing.SolidBrush($mint)
    $graphics.FillRectangle($handleBrush, 66 * $scale, 56 * $scale, 5 * $scale, 20 * $scale)
    $iceBrush = New-Object System.Drawing.SolidBrush($ice)
    $graphics.FillRectangle($iceBrush, 35 * $scale, 22 * $scale, 11 * $scale, 9 * $scale)

    $eyeBrush = New-Object System.Drawing.SolidBrush($dark)
    $graphics.FillEllipse($eyeBrush, 36 * $scale, 53 * $scale, 6 * $scale, 6 * $scale)
    $graphics.FillEllipse($eyeBrush, 58 * $scale, 53 * $scale, 6 * $scale, 6 * $scale)
    $smilePen = [System.Drawing.Pen]::new($dark, [float](3 * $scale))
    $graphics.DrawArc($smilePen, 39 * $scale, 61 * $scale, 22 * $scale, 14 * $scale, 15, 150)

    $leafBrush = New-Object System.Drawing.SolidBrush($mint)
    $leafPoints = [System.Drawing.PointF[]]@(
        [System.Drawing.PointF]::new([float](61 * $scale), [float](21 * $scale)),
        [System.Drawing.PointF]::new([float](74 * $scale), [float](12 * $scale)),
        [System.Drawing.PointF]::new([float](81 * $scale), [float](19 * $scale)),
        [System.Drawing.PointF]::new([float](75 * $scale), [float](29 * $scale)),
        [System.Drawing.PointF]::new([float](63 * $scale), [float](31 * $scale))
    )
    $graphics.FillPolygon($leafBrush, $leafPoints)

    $bellBrush = New-Object System.Drawing.SolidBrush($orange)
    $graphics.FillEllipse($bellBrush, 74 * $scale, 13 * $scale, 18 * $scale, 18 * $scale)
    $graphics.FillRectangle($bellBrush, 82 * $scale, 29 * $scale, 3 * $scale, 5 * $scale)
    $whiteBrush = New-Object System.Drawing.SolidBrush($white)
    $graphics.FillEllipse($whiteBrush, 80 * $scale, 16 * $scale, 5 * $scale, 9 * $scale)
    $graphics.FillEllipse($whiteBrush, 81 * $scale, 27 * $scale, 3 * $scale, 3 * $scale)

    $bitmap.Save($OutputPath, [System.Drawing.Imaging.ImageFormat]::Png)

    $fridgePen.Dispose()
    $smilePen.Dispose()
    $bgBrush.Dispose()
    $highlightBrush.Dispose()
    $dividerBrush.Dispose()
    $handleBrush.Dispose()
    $iceBrush.Dispose()
    $eyeBrush.Dispose()
    $leafBrush.Dispose()
    $bellBrush.Dispose()
    $whiteBrush.Dispose()
    $graphics.Dispose()
    $bitmap.Dispose()
}

$targets = @(
    @{ Dir = "mipmap-mdpi"; Size = 48 },
    @{ Dir = "mipmap-hdpi"; Size = 72 },
    @{ Dir = "mipmap-xhdpi"; Size = 96 },
    @{ Dir = "mipmap-xxhdpi"; Size = 144 },
    @{ Dir = "mipmap-xxxhdpi"; Size = 192 }
)

$base = Join-Path $PSScriptRoot "..\\app\\src\\main\\res"

foreach ($target in $targets) {
    $dir = Join-Path $base $target.Dir
    Draw-MascotIcon -Size $target.Size -OutputPath (Join-Path $dir "ic_launcher.png") -Round:$false
    Draw-MascotIcon -Size $target.Size -OutputPath (Join-Path $dir "ic_launcher_round.png") -Round:$true

    $legacyWebp = Join-Path $dir "ic_launcher.webp"
    $legacyRoundWebp = Join-Path $dir "ic_launcher_round.webp"
    if (Test-Path $legacyWebp) { Remove-Item $legacyWebp -Force }
    if (Test-Path $legacyRoundWebp) { Remove-Item $legacyRoundWebp -Force }
}
