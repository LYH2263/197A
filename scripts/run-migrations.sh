#!/bin/bash
MYSQL_HOST="${MYSQL_HOST:-db}"
MYSQL_USER="${MYSQL_USER:-shop}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-shop123}"
MYSQL_DATABASE="${MYSQL_DATABASE:-shop}"
SCRIPTS_DIR="${SCRIPTS_DIR:-/scripts}"

echo "Waiting for MySQL at ${MYSQL_HOST}..."
until mysql -h"$MYSQL_HOST" -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" -e "SELECT 1" >/dev/null 2>&1; do
  sleep 2
done

MYSQL="mysql -h${MYSQL_HOST} -u${MYSQL_USER} -p${MYSQL_PASSWORD} ${MYSQL_DATABASE}"

for f in "$SCRIPTS_DIR"/*.sql; do
  name=$(basename "$f")
  # Skip base schema/seed on re-run (tables already exist)
  if [ "$name" = "01-schema.sql" ] || [ "$name" = "02-seed.sql" ]; then
    continue
  fi
  echo "=== $name ==="
  $MYSQL < "$f" || echo "WARN: $name failed (may be already applied)"
done

echo "=== Migration complete ==="
$MYSQL -e "SHOW TABLES;"
