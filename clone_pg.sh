#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 3 ]]; then
  echo "Usage: $0 /path/to/src.env /path/to/dst.env <postgres_container_name_or_id>"
  echo "Example: $0 ./src.env ./dst.env postgres-db"
  exit 1
fi

SRC_ENV="$1"
DST_ENV="$2"
PG_CONTAINER="$3"

command -v docker >/dev/null || { echo "docker not found"; exit 1; }

# Load source env
# shellcheck disable=SC1090
source "$SRC_ENV"
SRC_HOST="${PGHOST}"
SRC_PORT="${PGPORT:-5432}"
SRC_DB="${PGDATABASE}"
SRC_USER="${PGUSER}"
SRC_PASS="${PGPASSWORD}"
SRC_SSLMODE="${PGSSLMODE:-prefer}"

# Load target env
# shellcheck disable=SC1090
source "$DST_ENV"
DST_PORT="${PGPORT:-5432}"
DST_DB="${PGDATABASE}"
DST_USER="${PGUSER}"
DST_PASS="${PGPASSWORD}"
DST_SSLMODE="${PGSSLMODE:-prefer}"

# Ensure container exists
docker inspect "$PG_CONTAINER" >/dev/null 2>&1 || {
  echo "Container not found: $PG_CONTAINER"
  echo "Tip: docker ps"
  exit 1
}

echo "==> Recreating target database inside container: ${DST_DB}"
# Drop/create DB on target (inside container). Requires sufficient privileges.
docker exec -i \
  -e "PGPASSWORD=${DST_PASS}" \
  -e "PGSSLMODE=${DST_SSLMODE}" \
  "$PG_CONTAINER" \
  psql -v ON_ERROR_STOP=1 -h localhost -p "$DST_PORT" -U "$DST_USER" -d postgres <<SQL
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE datname = '${DST_DB}'
  AND pid <> pg_backend_pid();

DROP DATABASE IF EXISTS "${DST_DB}";
CREATE DATABASE "${DST_DB}";
SQL

echo "==> Streaming dump from source -> restore into target (inside container)"
# Stream dump (custom format) from source directly into pg_restore on target
docker exec -i \
  -e "PGPASSWORD=${SRC_PASS}" \
  -e "PGSSLMODE=${SRC_SSLMODE}" \
  "$PG_CONTAINER" \
  pg_dump -h "$SRC_HOST" -p "$SRC_PORT" -U "$SRC_USER" -d "$SRC_DB" \
    --format=custom --compress=6 --no-owner --no-privileges \
| docker exec -i \
  -e "PGPASSWORD=${DST_PASS}" \
  -e "PGSSLMODE=${DST_SSLMODE}" \
  "$PG_CONTAINER" \
  pg_restore -h localhost -p "$DST_PORT" -U "$DST_USER" -d "$DST_DB" \
    --no-owner --no-privileges --clean --if-exists

echo "==> Done. Cloned ${SRC_DB} -> ${DST_DB} via container ${PG_CONTAINER}"
