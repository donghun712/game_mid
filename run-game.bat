@echo off
setlocal
cd /d "%~dp0"
if not defined JAVA_HOME (
    if exist "%ProgramFiles%\Android\Android Studio\jbr\bin\java.exe" (
        set "JAVA_HOME=%ProgramFiles%\Android\Android Studio\jbr"
    )
)
if not defined GRADLE_USER_HOME set "GRADLE_USER_HOME=%~dp0.gradle-user"
call "%~dp0gradlew.bat" :lwjgl3:run %*
set "GAME_EXIT_CODE=%ERRORLEVEL%"
if not "%GAME_EXIT_CODE%"=="0" pause
exit /b %GAME_EXIT_CODE%
