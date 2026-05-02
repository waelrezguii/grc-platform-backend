#!/bin/bash
# ─────────────────────────────────────────────
#  GRC — PostgreSQL Backup Script (Linux/macOS)
# ─────────────────────────────────────────────

# ── Config ────────────────────────────────────
DB_HOST="localhost"
DB_PORT="5432"
DB_NAME="riskbdd"
DB_USER="postgres"
DB_PASS=""
BACKUP_DIR="/opt/grc/backups"
KEEP_LAST=30
# ──────────────────────────────────────────────

FILENAME="backup_$(date +%Y-%m-%d_%H-%M-%S).sql.gz"
FILEPATH="$BACKUP_DIR/$FILENAME"
LOG="$BACKUP_DIR/backup.log"

mkdir -p "$BACKUP_DIR"

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Starting backup: $FILENAME" >> "$LOG"

# Dump + compress in one pipe (no temp file, no service interruption via MVCC)
PGPASSWORD="$DB_PASS" pg_dump \
  -h "$DB_HOST" \
  -p "$DB_PORT" \
  -U "$DB_USER" \
  -d "$DB_NAME" \
  -F p \
  | gzip > "$FILEPATH"

EXIT_CODE=$?

if [ $EXIT_CODE -ne 0 ]; then
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] ERROR: pg_dump failed (exit $EXIT_CODE)" >> "$LOG"
  exit $EXIT_CODE
fi

SIZE=$(du -sh "$FILEPATH" | cut -f1)
echo "[$(date '+%Y-%m-%d %H:%M:%S')] SUCCESS: $FILENAME ($SIZE)" >> "$LOG"

# ── Rotation: keep only last N backups ────────
ls -t "$BACKUP_DIR"/backup_*.sql.gz 2>/dev/null | tail -n +$((KEEP_LAST + 1)) | xargs -r rm -f
DELETED=$?
if [ $DELETED -eq 0 ]; then
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] Rotation: kept last $KEEP_LAST backups" >> "$LOG"
fi

exit 0
