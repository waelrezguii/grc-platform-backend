@echo off
REM ─────────────────────────────────────────────
REM  GRC — Oracle 19c Backup Script (Windows)
REM  Uses Oracle Data Pump (expdp)
REM ─────────────────────────────────────────────

REM ── Config ────────────────────────────────────
set DB_USER=grc_user
set DB_PASS=yourpassword
set DB_HOST=localhost
set DB_PORT=1521
set DB_SERVICE=orcl
set ORACLE_DIR_NAME=GRC_BACKUP_DIR
set BACKUP_DIR=C:\grc\backups\oracle
set KEEP_LAST=30
REM ──────────────────────────────────────────────

REM NOTE: Before running this script, a DBA must run once in Oracle:
REM   CREATE OR REPLACE DIRECTORY GRC_BACKUP_DIR AS 'C:\grc\backups\oracle';
REM   GRANT READ, WRITE ON DIRECTORY GRC_BACKUP_DIR TO grc_user;

REM ── Generate timestamp ────────────────────────
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
set DUMPFILE=backup_%TIMESTAMP%.dmp
set LOGFILE_EXP=backup_%TIMESTAMP%.log
set LOG=%BACKUP_DIR%\backup.log

if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"

echo [%YYYY%-%MM%-%DD% %HH%:%MIN%:%SS%] Starting Oracle backup: %DUMPFILE% >> "%LOG%"

REM ── Run expdp ─────────────────────────────────
expdp %DB_USER%/%DB_PASS%@%DB_HOST%:%DB_PORT%/%DB_SERVICE% ^
  DIRECTORY=%ORACLE_DIR_NAME% ^
  DUMPFILE=%DUMPFILE% ^
  LOGFILE=%LOGFILE_EXP% ^
  FULL=Y ^
  COMPRESSION=ALL

if %ERRORLEVEL% neq 0 (
    echo [%date% %time%] ERROR: expdp failed with code %ERRORLEVEL% >> "%LOG%"
    exit /b %ERRORLEVEL%
)

echo [%date% %time%] SUCCESS: %DUMPFILE% >> "%LOG%"

REM ── Rotation: keep only last N .dmp backups ──
set COUNT=0
for /f %%f in ('dir /b /o-d "%BACKUP_DIR%\backup_*.dmp" 2^>nul') do set /a COUNT+=1

set /a TO_DELETE=COUNT-KEEP_LAST
if %TO_DELETE% gtr 0 (
    for /f "skip=%KEEP_LAST% delims=" %%f in ('dir /b /o-d "%BACKUP_DIR%\backup_*.dmp" 2^>nul') do (
        del /f /q "%BACKUP_DIR%\%%f"
    )
    echo [%date% %time%] Rotation: kept last %KEEP_LAST% backups >> "%LOG%"
)

exit /b 0
