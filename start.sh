#!/bin/bash

cd /app

export SPRING_DATASOURCE_URL="jdbc:mysql://${DB_HOST:-localhost}:${DB_PORT:-3306}/${DB_NAME:-seb}"
export SPRING_DATASOURCE_USERNAME="${DB_USER:-root}"
export SPRING_DATASOURCE_PASSWORD="${DB_PASS:-}"

nginx

java -jar seb-backend.jar
