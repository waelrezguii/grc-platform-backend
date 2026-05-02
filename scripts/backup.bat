@echo off
REM ─────────────────────────────────────────────
REM  GRC — PostgreSQL Backup Script (Windows)
REM ─────────────────────────────────────────────

REM ── Config ────────────────────────────────────
set DB_HOST=localhost
set DB_PORT=5432
set DB_NAME=riskbdd
set DB_USER=postgres
set DB_PASS=
set BACKUP_DIR=C:\grc\backups
set KEEP_LAST=30
REM ──────────────────────────────────────────────

REM ── Generate timestamp for filename ──────────
for /f "tokens=1-6 delims=/: " %%a in ("%date% %time%") do (
    set YYYY=%%c
    set MM=%%a
    set DD=%%b
    set HH=%%d
    set MIN=%%e
    set SS=%%f
)
set SS=%SS: =0%
set FILENAME=backup_%YYYY%-%MM%-%DD%_%HH%-%MIN%-%SS%.sql
set FILEPATH=%BACKUP_DIR%\%FILENAME%
set LOG=%BACKUP_DIR%\backup.log

REM ── Create backup directory if missing ───────
if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"

echo [%YYYY%-%MM%-%DD% %HH%:%MIN%:%SS%] Starting backup: %FILENAME% >> "%LOG%"

REM ── Run pg_dump ───────────────────────────────
set PGPASSWORD=%DB_PASS%
pg_dump -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -F p -f "%FILEPATH%"

if %ERRORLEVEL% neq 0 (
    echo [%date% %time%] ERROR: pg_dump failed with code %ERRORLEVEL% >> "%LOG%"
    exit /b %ERRORLEVEL%
)

echo [%date% %time%] SUCCESS: %FILENAME% >> "%LOG%"

REM ── Rotation: delete oldest beyond KEEP_LAST ─
REM Count existing backups and remove oldest ones
set COUNT=0
for /f %%f in ('dir /b /o-d "%BACKUP_DIR%\backup_*.sql" 2^>nul') do set /a COUNT+=1

set /a TO_DELETE=COUNT-KEEP_LAST
if %TO_DELETE% gtr 0 (
    set DELETED=0
    for /f "skip=%KEEP_LAST% delims=" %%f in ('dir /b /o-d "%BACKUP_DIR%\backup_*.sql" 2^>nul') do (
        del /f /q "%BACKUP_DIR%\%%f"
        set /a DELETED+=1
    )
    echo [%date% %time%] Rotation: removed %DELETED% old backup^(s^) >> "%LOG%"
)

exit /b 0
