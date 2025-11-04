#!/bin/bash
./gradlew :backend:jibBuildTar
docker load < backend/build/jib-image.tar
docker tag backend:2.0.0 kaelesty/ktor-docker-image:release
docker push kaelesty/ktor-docker-image:release

