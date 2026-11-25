#Requires -Version 5.1
<#
.SYNOPSIS
    Regenerates src/main/resources/icon.png (128x128) for Fluid Routes.
#>
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$projectRoot = Split-Path -Parent $PSScriptRoot
$output = Join-Path $projectRoot 'src\main\resources\icon.png'
New-Item -ItemType Directory -Force -Path (Split-Path $output) | Out-Null

$size = 128
$bitmap = New-Object System.Drawing.Bitmap($size, $size)
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
$graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$graphics.Clear([System.Drawing.Color]::FromArgb(255, 10, 18, 28))

# Faint map grid
$gridPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(26, 90, 150, 220), 1)
for ($i = 16; $i -lt $size; $i += 24) {
    $graphics.DrawLine($gridPen, $i, 0, $i, $size)
    $graphics.DrawLine($gridPen, 0, $i, $size, $i)
}
$gridPen.Dispose()

$borderPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 36, 56, 80), 3)
$graphics.DrawRectangle($borderPen, 1.5, 1.5, $size - 4, $size - 4)
$borderPen.Dispose()

# Pipe run with a right angle
$haloPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(190, 4, 10, 20), 11)
$haloPen.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round
$pipePen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 61, 155, 255), 7)
$pipePen.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round

$path = @(
    (New-Object System.Drawing.PointF(20, 46)),
    (New-Object System.Drawing.PointF(72, 46)),
    (New-Object System.Drawing.PointF(72, 84)),
    (New-Object System.Drawing.PointF(108, 84))
)
$graphics.DrawLines($haloPen, $path)
$graphics.DrawLines($pipePen, $path)
$haloPen.Dispose()
$pipePen.Dispose()

# Nodes: pump (cyan), tank (blue), spout (violet)
$outlineBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(220, 4, 10, 20))
$pumpBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 53, 208, 255))
$tankBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 47, 123, 255))
$spoutBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 192, 140, 255))

foreach ($node in @(
    @{ X = 20; Y = 46; B = $tankBrush },
    @{ X = 72; Y = 46; B = $pumpBrush },
    @{ X = 72; Y = 84; B = $pumpBrush },
    @{ X = 108; Y = 84; B = $spoutBrush }
)) {
    $graphics.FillEllipse($outlineBrush, ($node.X - 13), ($node.Y - 13), 26, 26)
    $graphics.FillEllipse($node.B, ($node.X - 10), ($node.Y - 10), 20, 20)
}

$outlineBrush.Dispose()
$pumpBrush.Dispose()
$tankBrush.Dispose()
$spoutBrush.Dispose()

$graphics.Dispose()
$bitmap.Save($output, [System.Drawing.Imaging.ImageFormat]::Png)
$bitmap.Dispose()

Write-Host "Wrote $output"
