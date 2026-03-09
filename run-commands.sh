#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "Available commands:"
echo "1) Run backend (gradle task runBackend)"
echo "2) Build and push backend docker image"
echo "3) Clone postgres db (src.env -> dst.env)"

read -rp "Select command index: " idx

if [[ ! "${idx}" =~ ^[0-9]+$ ]]; then
  echo "Invalid index: ${idx}"
  exit 1
fi

case "${idx}" in
  1)
    echo "Running: Run backend (gradle task runBackend)"
    "${SCRIPT_DIR}/gradlew" runBackend
    ;;
  2)
    echo "Running: Build and push backend docker image"
    bash "${SCRIPT_DIR}/push-backend.sh"
    ;;
  3)
    echo "Running: Clone postgres db (src.env -> dst.env)"
    bash "${SCRIPT_DIR}/clone_pg.sh" "${SCRIPT_DIR}/src.env" "${SCRIPT_DIR}/dst.env" postgres-local
    ;;
  *)
    echo "Index out of range: ${idx}"
    exit 1
    ;;
esac
