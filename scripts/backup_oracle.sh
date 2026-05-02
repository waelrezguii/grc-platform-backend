#!/bin/bash
# ─────────────────────────────────────────────
#  GRC — Oracle 19c Backup Script (Linux/macOS)
#  Uses Oracle Data Pump (expdp)
# ─────────────────────────────────────────────

# ── Config ────────────────────────────────────
DB_USER="grc_user"
DB_PASS="yourpassword"
DB_HOST="localhost"
DB_PORT="1521"
DB_SERVICE="orcl"
# Oracle directory object name (must be created in DB first — see note below)
ORACLE_DIR_NAME="GRC_BACKUP_DIR"
BACKUP_DIR="/opt/grc/backups/oracle"
KEEP_LAST=30
# ──────────────────────────────────────────────

# NOTE: Before running this script, a DBA must run once in Oracle:
#   CREATE OR REPLACE DIRECTORY GRC_BACKUP_DIR AS '/opt/grc/backups/oracle';
#   GRANT READ, WRITE ON DIRECTORY GRC_BACKUP_DIR TO grc_user;

TIMESTAMP=$(date +%Y-%m-%d_%H-%M-%S)
DUMPFILE="backup_${TIMESTAMP}.dmp"
LOGFILE_EXP="backup_${TIMESTAMP}.log"
LOG="$BACKUP_DIR/backup.log"

mkdir -p "$BACKUP_DIR"

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Starting Oracle backup: $DUMPFILE" >> "$LOG"

# Run expdp
expdp "${DB_USER}/${DB_PASS}@${DB_HOST}:${DB_PORT}/${DB_SERVICE}" \
  DIRECTORY="${ORACLE_DIR_NAME}" \
  DUMPFILE="${DUMPFILE}" \
  LOGFILE="${LOGFILE_EXP}" \
  FULL=Y \
  COMPRESSION=ALL

EXIT_CODE=$?

if [ $EXIT_CODE -ne 0 ]; then
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] ERROR: expdp failed (exit $EXIT_CODE)" >> "$LOG"
  exit $EXIT_CODE
fi

SIZE=$(du -sh "$BACKUP_DIR/$DUMPFILE" | cut -f1)
echo "[$(date '+%Y-%m-%d %H:%M:%S')] SUCCESS: $DUMPFILE ($SIZE)" >> "$LOG"

# ── Rotation: keep only last N .dmp backups ───
ls -t "$BACKUP_DIR"/backup_*.dmp 2>/dev/null | tail -n +$((KEEP_LAST + 1)) | xargs -r rm -f
# Also clean up their matching .log files
ls -t "$BACKUP_DIR"/backup_*.log 2>/dev/null | grep -v "^$LOG$" | tail -n +$((KEEP_LAST + 1)) | xargs -r rm -f

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Rotation: kept last $KEEP_LAST backups" >> "$LOG"

exit 0
