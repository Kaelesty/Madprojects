#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IMAGE_LOCAL="backend:2.0.0"
IMAGE_REMOTE="kaelesty/ktor-docker-image:release"

if ! command -v docker >/dev/null 2>&1; then
  echo "docker command not found. Install Docker Desktop first."
  exit 1
fi

if ! docker info >/dev/null 2>&1; then
  echo "Docker daemon is not running. Start Docker Desktop (Linux containers mode) and retry."
  exit 1
fi

if ! "${SCRIPT_DIR}/gradlew" :backend:jibBuildTar; then
  echo
  echo "Jib image build failed."
  echo "If you see unauthorized for Docker Hub base image, run: docker login"
  exit 1
fi

docker load < "${SCRIPT_DIR}/backend/build/jib-image.tar"
docker tag "${IMAGE_LOCAL}" "${IMAGE_REMOTE}"
docker push "${IMAGE_REMOTE}"

