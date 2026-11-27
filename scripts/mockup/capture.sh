#!/bin/sh
# ---------------------------------------------------------------------------
# mockup/capture.sh - check the mock-up is up, then print the URLs to open.
#
# WHAT THIS SOLVES
#   The value of a screenshot depends on the screenshot actually showing the
#   screen under review. Two things quietly ruin that: an app that is not
#   running, and a route that does not resolve (which renders a not-found page
#   that still looks like a screenshot). This script refuses rather than
#   producing either.
#
# WHY IT DOES NOT LOG IN
#   session-proxy.js in front of the app already holds a session, so every URL
#   on the port it listens on is authenticated. Logging in here as well would
#   mean guessing which of the two ports is the right one to talk to, and
#   getting it wrong is invisible: the request succeeds and returns the login
#   page.
#
# USAGE
#   ./scripts/mockup/capture.sh              # all routes in routes.txt
#   ./scripts/mockup/capture.sh projects-list  # only the named ones
#   ./scripts/mockup/capture.sh --list
#
# OUTPUT
#   The URLs to hand to a browser tool. Screenshots are written by that tool,
#   not here -- the mock-up does not assume a particular one is installed.
# ---------------------------------------------------------------------------

set -eu

REPO=$(cd "$(dirname "$0")/../.." && pwd)
PORT=${DM_MOCKUP_PORT:-48013}
ROUTES="$REPO/scripts/mockup/routes.txt"
BASE="http://localhost:$PORT"

if [ ! -f "$ROUTES" ]; then
  echo "missing $ROUTES" >&2
  exit 1
fi

select_routes() {
  grep -v '^#' "$ROUTES" | grep -v '^$'
}

if [ "${1:-}" = "--list" ]; then
  echo "routes in scripts/mockup/routes.txt:"
  select_routes | sed 's/|/  ->  /' | sed 's/^/  /'
  exit 0
fi

if ! curl -sf -o /dev/null -m 5 "$BASE/login"; then
  echo "nothing is answering on $BASE" >&2
  echo "start the mock-up first: ./scripts/mockup/run.sh" >&2
  exit 1
fi

# The proxy is what makes the URLs authenticated. If it is not in front of the
# app, every protected route below would render the login form instead.
if ! curl -s -o /dev/null -w '%{http_code}' -m 10 "$BASE/projects/list" | grep -q '^200$'; then
  echo "$BASE/projects/list does not return 200." >&2
  echo "Is session-proxy.js still running? It is started by run.sh and holds the session." >&2
  exit 2
fi

WANTED=""
for arg in "$@"; do
  case "$arg" in
    -*) ;;
    *) WANTED="$WANTED $arg" ;;
  esac
done

LIST=$(select_routes)
if [ -n "$(echo "$WANTED" | tr -d ' ')" ]; then
  FILTERED=""
  for name in $WANTED; do
    match=$(echo "$LIST" | grep "^$name|" || true)
    if [ -z "$match" ]; then
      echo "unknown route: $name (see --list)" >&2
      exit 3
    fi
    FILTERED="$FILTERED
$match"
  done
  LIST=$(echo "$FILTERED" | grep -v '^$')
fi

echo "ready -- each URL is already authenticated:"
echo
printf '%s\n' "$LIST" | while IFS='|' read -r name path; do
  printf '  %-32s %s%s\n' "$name" "$BASE" "$path"
done
echo
echo "Open one with a browser tool to record it. Routes not listed in"
echo "scripts/mockup/routes.txt have not been confirmed to resolve."
