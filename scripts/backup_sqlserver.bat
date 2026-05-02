@echo off
REM ──────────────────────────────────────────────────
REM  GRC — SQL Server 2019 Backup Script (Windows)
REM  Uses sqlcmd + T-SQL BACKUP DATABASE
REM ──────────────────────────────────────────────────

REM ── Config ─────────────────────────────────────────
set DB_HOST=localhost
set DB_PORT=1433
set DB_NAME=riskbdd
set DB_USER=sa
set DB_PASS=yourpassword
set BACKUP_DIR=C:\grc\backups\sqlserver
set KEEP_LAST=30
REM ───────────────────────────────────────────────────

REM ── Generate timestamp ────────────────────────────
for /f "tokens=1-6 delims=/: " %%a in ("%date% %time%") do (
    set YYYY=%%c
    set MM=%%a
    set DD=%%b
    set HH=%%d
    set MIN=%%e
    set SS=%%f
)
set SS=%SS: =0%
set TIMESTAMP=%YYYY%-%MM%-%DD%_%HH%-%MIN%-%SS%
set FILENAME=backup_%TIMESTAMP%.bak
set FILEPATH=%BACKUP_DIR%\%FILENAME%
set LOG=%BACKUP_DIR%\backup.log

if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"

echo [%YYYY%-%MM%-%DD% %HH%:%MIN%:%SS%] Starting SQL Server backup: %FILENAME% >> "%LOG%"

REM ── Run BACKUP DATABASE via sqlcmd ────────────────
sqlcmd -S %DB_HOST%,%DB_PORT% ^
       -U %DB_USER% ^
       -P %DB_PASS% ^
       -Q "BACKUP DATABASE [%DB_NAME%] TO DISK='%FILEPATH%' WITH COMPRESSION, STATS=10, FORMAT, INIT"

if %ERRORLEVEL% neq 0 (
    echo [%date% %time%] ERROR: sqlcmd failed with code %ERRORLEVEL% >> "%LOG%"
    exit /b %ERRORLEVEL%
)

echo [%date% %time%] SUCCESS: %FILENAME% >> "%LOG%"

REM ── Rotation: keep only last N .bak backups ──────
set COUNT=0
for /f %%f in ('dir /b /o-d "%BACKUP_DIR%\backup_*.bak" 2^>nul') do set /a COUNT+=1

set /a TO_DELETE=COUNT-KEEP_LAST
if %TO_DELETE% gtr 0 (
    for /f "skip=%KEEP_LAST% delims=" %%f in ('dir /b /o-d "%BACKUP_DIR%\backup_*.bak" 2^>nul') do (
        del /f /q "%BACKUP_DIR%\%%f"
    )
    echo [%date% %time%] Rotation: kept last %KEEP_LAST% backups >> "%LOG%"
)

exit /b 0
