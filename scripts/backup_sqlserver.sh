#!/bin/bash
# ──────────────────────────────────────────────────
#  GRC — SQL Server 2019 Backup Script (Linux/macOS)
#  Uses sqlcmd + T-SQL BACKUP DATABASE
# ──────────────────────────────────────────────────

# ── Config ─────────────────────────────────────────
DB_HOST="localhost"
DB_PORT="1433"
DB_NAME="riskbdd"
DB_USER="sa"
DB_PASS="yourpassword"
BACKUP_DIR="/opt/grc/backups/sqlserver"
KEEP_LAST=30
# ───────────────────────────────────────────────────

TIMESTAMP=$(date +%Y-%m-%d_%H-%M-%S)
FILENAME="backup_${TIMESTAMP}.bak"
FILEPATH="$BACKUP_DIR/$FILENAME"
LOG="$BACKUP_DIR/backup.log"

mkdir -p "$BACKUP_DIR"

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Starting SQL Server backup: $FILENAME" >> "$LOG"

# Run BACKUP DATABASE via sqlcmd
# WITH COMPRESSION reduces file size
# WITH STATS=10 logs progress every 10%
sqlcmd -S "${DB_HOST},${DB_PORT}" \
       -U "${DB_USER}" \
       -P "${DB_PASS}" \
       -Q "BACKUP DATABASE [${DB_NAME}] TO DISK='${FILEPATH}' WITH COMPRESSION, STATS=10, FORMAT, INIT"

EXIT_CODE=$?

if [ $EXIT_CODE -ne 0 ]; then
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] ERROR: sqlcmd failed (exit $EXIT_CODE)" >> "$LOG"
  exit $EXIT_CODE
fi

SIZE=$(du -sh "$FILEPATH" | cut -f1)
echo "[$(date '+%Y-%m-%d %H:%M:%S')] SUCCESS: $FILENAME ($SIZE)" >> "$LOG"

# ── Rotation: keep only last N .bak backups ───
ls -t "$BACKUP_DIR"/backup_*.bak 2>/dev/null | tail -n +$((KEEP_LAST + 1)) | xargs -r rm -f

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Rotation: kept last $KEEP_LAST backups" >> "$LOG"

exit 0
