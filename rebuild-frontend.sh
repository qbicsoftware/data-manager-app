#!/usr/bin/env bash
#
# Rebuilds the Data Manager frontend assets.
#
# Usage:
#   ./rebuild-frontend.sh          # development mode
#   ./rebuild-frontend.sh --prod   # production mode (full optimised bundle)
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

PROD=false
if [[ "${1:-}" == "--prod" ]]; then
  PROD=true
fi

echo "==> Compiling project..."
./mvnw compile -pl datamanager-app -q

echo "==> Building Vaadin frontend..."
if $PROD; then
  ./mvnw vaadin:build-frontend -pl datamanager-app -Pproduction -q
else
  ./mvnw vaadin:build-frontend -pl datamanager-app -q
fi

echo "==> Done."
