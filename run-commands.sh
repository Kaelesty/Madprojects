#!/usr/bin/env bash
set -euo pipefail

descriptions=(
  "Run backend (gradle task runBackend)"
)

commands=(
  "./gradlew runBackend"
)

echo "Available commands:"
for i in "${!commands[@]}"; do
  display_idx=$((i + 1))
  printf "%d) %s\n" "$display_idx" "${descriptions[$i]}"
done

read -rp "Select command index: " idx

if [[ ! "${idx}" =~ ^[0-9]+$ ]]; then
  echo "Invalid index: ${idx}"
  exit 1
fi

if (( idx < 1 || idx > ${#commands[@]} )); then
  echo "Index out of range: ${idx}"
  exit 1
fi

command_idx=$((idx - 1))
echo "Running: ${descriptions[$command_idx]}"
eval "${commands[$command_idx]}"
