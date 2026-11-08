param(
    [string]$JdkHome
)

$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot

if (-not $JdkHome) {
    $packager = Get-Command jpackage.exe -ErrorAction SilentlyContinue
    if (-not $packager) {
        throw 'Packaging requires a Windows x64 JDK with jpackage on PATH, or -JdkHome.'
    }
    $JdkHome = Split-Path (Split-Path $packager.Source -Parent) -Parent
}
$packageTool = Join-Path $JdkHome 'bin\jpackage.exe'
if (-not (Test-Path -LiteralPath $packageTool)) {
    throw "Cannot find jpackage.exe in $JdkHome"
}

$previousJavaHome = $env:JAVA_HOME
$previousGradleHome = $env:GRADLE_USER_HOME
try {
    $env:JAVA_HOME = $JdkHome
    $env:GRADLE_USER_HOME = Join-Path $projectRoot '.gradle-user'
    Push-Location $projectRoot
    try {
        & '.\gradlew.bat' --offline --console=plain :lwjgl3:installDist
        if ($LASTEXITCODE -ne 0) { throw 'Building the game failed.' }
    } finally {
        Pop-Location
    }

    $distribution = Join-Path $projectRoot 'dist'
    $staging = Join-Path $projectRoot ('.work\windows-package-' + [guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $distribution, $staging -Force | Out-Null
    $packageOptions = @(
        '--type', 'app-image',
        '--name', 'JaguarBlock',
        '--app-version', '0.1.0',
        '--description', 'Jaguar Block: Navigate the Unknown',
        '--input', (Join-Path $projectRoot 'lwjgl3\build\install\lwjgl3\lib'),
        '--main-jar', 'lwjgl3-0.1.0.jar',
        '--main-class', 'com.gamemid.memory.lwjgl3.Lwjgl3Launcher',
        '--dest', $staging,
        '--java-options', '-Dfile.encoding=UTF-8'
    )
    & $packageTool @packageOptions
    if ($LASTEXITCODE -ne 0) { throw 'Creating the Windows application failed.' }

    $appDirectory = Join-Path $staging 'JaguarBlock'
    @'
Jaguar Block: Navigate the Unknown

Windows 64비트용 배포본입니다. Java 실행 환경이 포함되어 있습니다.

1. ZIP 파일을 원하는 폴더에 모두 압축 해제하세요.
2. JaguarBlock 폴더 안의 JaguarBlock.exe를 실행하세요.

app, runtime 폴더를 실행 파일과 함께 보관하세요.

A/D 또는 좌우 방향키 이동 / Space 점프
F 섬광탄 / R 스테이지 다시하기 / Esc 일시정지
'@ | Set-Content -LiteralPath (Join-Path $appDirectory '실행안내.txt') -Encoding UTF8

    $archive = Join-Path $distribution 'JaguarBlock-Windows-x64.zip'
    $stagedArchive = Join-Path $staging 'JaguarBlock-Windows-x64.zip'
    Compress-Archive -LiteralPath $appDirectory -DestinationPath $stagedArchive
    Move-Item -LiteralPath $stagedArchive -Destination $archive -Force

    $resolvedStaging = [System.IO.Path]::GetFullPath($staging)
    $workRoot = [System.IO.Path]::GetFullPath((Join-Path $projectRoot '.work')) + [System.IO.Path]::DirectorySeparatorChar
    if (-not $resolvedStaging.StartsWith($workRoot, [System.StringComparison]::OrdinalIgnoreCase) -or
            (Split-Path $resolvedStaging -Leaf) -notmatch '^windows-package-[0-9a-f]{32}$') {
        throw 'Refusing to remove a staging directory outside the packaging workspace.'
    }
    Remove-Item -LiteralPath $resolvedStaging -Recurse -Force
    Write-Host "Share this ZIP: $archive"
} finally {
    $env:JAVA_HOME = $previousJavaHome
    $env:GRADLE_USER_HOME = $previousGradleHome
}
