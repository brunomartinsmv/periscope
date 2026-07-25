#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

docker compose up --build -d

echo
echo "Periscope: http://localhost:8080/periscope/"
echo "Login padrão: admin / 123456"
echo "Logs: docker compose logs -f periscope"
