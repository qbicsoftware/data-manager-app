#!/bin/sh
# ---------------------------------------------------------------------------
# mockup/run.sh - start the Data Manager as a self-contained mock-up.
#
# WHAT THIS SOLVES
#   Booting the real application normally needs two MariaDB databases, an
#   Artemis broker, an SMTP server, a provisioned PKCS12 vault and eight
#   environment variables. For looking at a screen, all of that is noise.
#   This script starts the app against:
#     * H2 file databases, built from sql/complete-schema.sql
#     * the embedded Artemis broker
#     * a throwaway vault generated on first run
#     * two fixture users, one of them automatically logged in for tools
#       that cannot fill in a login form
#
# USAGE
#   ./scripts/mockup/run.sh              # start (builds if needed)
#   ./scripts/mockup/run.sh --no-proxy    # app on 48013 directly; log in by hand
#   ./scripts/mockup/run.sh --rebuild    # rebuild the jar first
#   ./scripts/mockup/run.sh --reset-db   # recreate the databases, keep the vault
#   ./scripts/mockup/run.sh --clean      # forget everything, including the vault
#
# RESULTS
#   http://localhost:48013            the application
#   <work>/mockup.log                 the application log
#   <work>/screens/<name>.png         screenshots, if capture.sh was run
#
# The work directory defaults to <repo>/.mockup and is git-ignored.
# ---------------------------------------------------------------------------

set -eu

REPO=$(cd "$(dirname "$0")/../.." && pwd)
WORK=${DM_MOCKUP_WORK:-$REPO/.mockup}
# Browsers reach 48013; the application itself listens one port up so the
# session proxy can sit in front of it.
PORT=${DM_MOCKUP_PORT:-48013}
APP_PORT=${DM_MOCKUP_APP_PORT:-48014}
VAULT_PASS='Zx9!vQ2#mK7$wR4^pL8&nB3*tY6'   # fixed: the vault needs >100 bits of entropy
JAR="$REPO/datamanager-app/target/datamanager-app-1.19.2.jar"

REBUILD=0
RESET_DB=0
CLEAN=0
NO_PROXY=0
for arg in "$@"; do
  case "$arg" in
    --rebuild) REBUILD=1 ;;
    --reset-db) RESET_DB=1 ;;
    --clean) CLEAN=1; RESET_DB=1; REBUILD=0 ;;
    # The proxy exists only so a tool that cannot fill in a login form can still
    # reach a protected page. It costs a hop on every request, so it is optional:
    # with --no-proxy the app listens on PORT directly and you log in by hand.
    --no-proxy) NO_PROXY=1 ;;
    -h|--help) sed -n '2,34p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "unknown option: $arg" >&2; exit 1 ;;
  esac
done

if [ "$NO_PROXY" = 1 ]; then
  APP_PORT=$PORT
fi

# --- Java -------------------------------------------------------------------
# The JDK is often absent from PATH even when installed; look where it usually is.
if [ -z "${JAVA_HOME:-}" ]; then
  for candidate in "$HOME/.sdkman/candidates/java/21.0.13.1+2-tem" \
                   "$HOME/.sdkman/candidates/java/current" \
                   /Library/Java/JavaVirtualMachines/*/Contents/Home; do
    if [ -x "$candidate/bin/java" ]; then JAVA_HOME=$candidate; break; fi
  done
fi
if [ -z "${JAVA_HOME:-}" ] || [ ! -x "$JAVA_HOME/bin/java" ]; then
  echo "No Java 21 found. Set JAVA_HOME." >&2
  exit 1
fi
PATH="$JAVA_HOME/bin:$PATH"
export JAVA_HOME PATH

mkdir -p "$WORK"

if [ "$CLEAN" = 1 ]; then
  echo "==> removing $WORK"
  rm -rf "$WORK"
  mkdir -p "$WORK"
fi

# --- database ---------------------------------------------------------------
if [ "$RESET_DB" = 1 ]; then
  rm -f "$WORK"/db-*.mv.db "$WORK"/db-*.trace.db "$WORK"/.database-ready
fi

# The schema and fixture programs talk to H2 directly, so they need the driver
# on the classpath. Take it from the local Maven repository rather than adding
# a build step: the dependency is already there once the app has been built.
h2_jar() {
  for candidate in "$HOME"/.m2/repository/com/h2database/h2/*/h2-*.jar; do
    case "$candidate" in *sources*) continue ;; esac
    if [ -f "$candidate" ]; then echo "$candidate"; return 0; fi
  done
  return 1
}

# Guard on a marker written only after BOTH steps succeed, never on the database
# file. The database file exists as soon as schema loading starts, so checking
# for it treats a half-built database as ready -- which is exactly the state a
# failed load leaves behind.
READY="$WORK/.database-ready"

if [ ! -f "$READY" ]; then
  echo "==> building the mock-up database"
  rm -f "$READY"
  H2=$(h2_jar) || {
    echo "H2 is not in the local Maven repository yet; build the app once first:" >&2
    echo "  ./mvnw -Duser.home=$WORK/home -Dmaven.test.skip=true -P '!sigcheck' install" >&2
    exit 2
  }
  node "$REPO/scripts/mockup/schema-to-h2.js" \
       "$REPO/sql/complete-schema.sql" "$WORK/schema-h2.sql"
  # Single-file program: java runs it directly, no compile step.
  java -cp "$H2" "$REPO/scripts/mockup/MockDatabase.java" "$WORK/schema-h2.sql" "$WORK" \
    || { echo "schema load failed" >&2; exit 2; }
  java -cp "$H2" "$REPO/scripts/mockup/Fixtures.java" "$REPO/sql/insert-default-values.sql" "$WORK/db-dm" \
    || { echo "fixture seeding failed" >&2; exit 2; }
  : > "$READY"
fi

# --- vault ------------------------------------------------------------------
# The vault has no dev bypass: it insists on a real PKCS12 keystore holding a
# 32-byte AES key under the alias the app reads at startup.
if [ ! -f "$WORK/keystore.p12" ]; then
  echo "==> generating a throwaway vault"
  keytool -genkeypair -alias dummy -keyalg RSA -keysize 2048 \
    -keystore "$WORK/keystore.p12" -storetype PKCS12 -storepass "$VAULT_PASS" \
    -dname "CN=Mockup,OU=Dev,O=QBiC,L=Tuebingen,ST=BW,C=DE" -validity 3650 >/dev/null 2>&1
  java "$REPO/scripts/mockup/VaultKey.java" "$WORK/keystore.p12" "$VAULT_PASS" \
    external-credential-master-key
fi

# --- jar --------------------------------------------------------------------
if [ "$REBUILD" = 1 ] || [ ! -f "$JAR" ]; then
  echo "==> building the application (this takes a few minutes)"
  # -Duser.home: Vaadin insists on ~/.vaadin, which is not writable in every sandbox.
  ( cd "$REPO" && ./mvnw -B -Duser.home="$WORK/home" -Dmaven.test.skip=true \
      -Dsigstore.skip=true -P '!sigcheck' install )
fi

# --- stopping an earlier run -------------------------------------------------
# The previous run records what it started. Do NOT rediscover processes by port
# with "lsof -ti:PORT": on this platform Tomcat binds an IPv6 socket (netstat
# shows tcp46 *.48014) and lsof reports no pid for it, so the lookup silently
# matched nothing, the old JVM survived, and every restart leaked another one.
# A pile of orphaned JVMs competing for the same database is what made the
# application feel slow -- not any single request being expensive.
stop_recorded() {
  pidfile=$1
  label=$2
  [ -f "$pidfile" ] || return 0
  pid=$(cat "$pidfile" 2>/dev/null || true)
  rm -f "$pidfile"
  case "$pid" in
    ''|*[!0-9]*) return 0 ;;
  esac
  kill -0 "$pid" 2>/dev/null || return 0
  echo "==> stopping the $label from the previous run (pid $pid)"
  # TERM first: the JVM shuts down and H2 closes its files cleanly. kill -9
  # leaves the databases locked and the next start fails with "Database may be
  # already in use", which reads like corruption rather than a leftover.
  kill -TERM "$pid" 2>/dev/null || true
  for _ in $(seq 1 20); do
    kill -0 "$pid" 2>/dev/null || break
    sleep 1
  done
  if kill -0 "$pid" 2>/dev/null; then
    echo "    did not stop within 20s; forcing"
    kill -9 "$pid" 2>/dev/null || true
    sleep 2
  fi
}

stop_recorded "$WORK/proxy.pid" "session proxy"
stop_recorded "$WORK/app.pid" "application"

# The pidfiles only cover instances this script started. Anything else holding
# our ports -- an earlier run whose pidfile was deleted, or a process started by
# hand -- has to be found another way, or the next start dies with
# "Port 48014 was already in use".
#
# "lsof -ti:PORT" is NOT reliable here: Tomcat binds an IPv6 socket (netstat
# shows tcp46 *.48014) and lsof reports no pid for it. "netstat -anv" prints the
# owning process in its last column, which works for those.
pid_on_port() {
  port=$1
  pid=$(lsof -nP -iTCP:"$port" -sTCP:LISTEN -t 2>/dev/null | head -1 || true)
  if [ -z "$pid" ]; then
    pid=$(netstat -anv 2>/dev/null \
      | grep "[.]$port " | grep LISTEN \
      | grep -oE '[A-Za-z_.-]+:[0-9]+' | head -1 | cut -d: -f2 || true)
  fi
  echo "$pid"
}

free_port() {
  port=$1
  pid=$(pid_on_port "$port")
  [ -n "$pid" ] || return 0
  case "$pid" in *[!0-9]*) return 0 ;; esac
  echo "==> port $port is held by pid $pid; stopping it"
  kill -TERM "$pid" 2>/dev/null || true
  for _ in $(seq 1 15); do
    kill -0 "$pid" 2>/dev/null || return 0
    sleep 1
  done
  echo "    did not stop within 15s; forcing"
  kill -9 "$pid" 2>/dev/null || true
  sleep 2
}

free_port "$PORT"
free_port "$APP_PORT"

# A port can stay bound by a socket whose process is already gone -- the kernel
# has not reclaimed it yet. Nothing the script can do will free that: the pid is
# dead, so there is nothing to signal. Rather than fail with "Port already in
# use" after a 3 minute start, move to the next free port and say so.
port_taken() {
  port=$1
  pid=$(pid_on_port "$port")
  [ -n "$pid" ] || return 1
  case "$pid" in
    *[!0-9]*) return 1 ;;
  esac
  # A live pid means the stop above did not work; treat that as taken too.
  return 0
}

find_free_port() {
  start=$1
  candidate=$start
  while [ "$candidate" -lt $((start + 20)) ]; do
    if ! port_taken "$candidate"; then echo "$candidate"; return 0; fi
    candidate=$((candidate + 1))
  done
  return 1
}

if port_taken "$APP_PORT"; then
  HOLDER=$(pid_on_port "$APP_PORT")
  if [ -n "$HOLDER" ] && kill -0 "$HOLDER" 2>/dev/null; then
    echo "port $APP_PORT is still held by pid $HOLDER after stopping it" >&2
    exit 4
  fi
  if [ "$NO_PROXY" = 1 ]; then
    NEW_PORT=$(find_free_port $((APP_PORT + 10))) || {
      echo "no free port found near $APP_PORT" >&2; exit 4; }
    echo "==> port $APP_PORT is bound by a socket whose process is gone"
    echo "    (the kernel has not reclaimed it) -- using $NEW_PORT instead"
    PORT=$NEW_PORT
    APP_PORT=$NEW_PORT
  else
    NEW_PORT=$(find_free_port $((APP_PORT + 10))) || {
      echo "no free port found near $APP_PORT" >&2; exit 4; }
    echo "==> port $APP_PORT is bound by a socket whose process is gone"
    echo "    (the kernel has not reclaimed it) -- using $NEW_PORT instead"
    APP_PORT=$NEW_PORT
  fi
fi

if port_taken "$PORT"; then
  NEW_PORT=$(find_free_port $((PORT + 10))) || {
    echo "no free port found near $PORT" >&2; exit 4; }
  echo "==> port $PORT is bound by a socket whose process is gone -- using $NEW_PORT instead"
  PORT=$NEW_PORT
  if [ "$NO_PROXY" = 1 ]; then APP_PORT=$NEW_PORT; fi
fi

# Only reached if a forced stop happened: the ports are free, so any lock left
# behind belongs to a process that no longer exists.
for stale in "$WORK"/db-*.lock.db; do
  [ -e "$stale" ] || continue
  echo "==> removing stale database lock $(basename "$stale")"
  rm -f "$stale"
done

set -a
DATAMANAGER_VAULT_KEY=$VAULT_PASS
DATAMANAGER_VAULT_ENTRY_PASSWORD=$VAULT_PASS
DATAMANAGER_VAULT_PATH="$WORK/keystore.p12"
# Embedded broker: the app ships artemis-jms-server, so no external one is needed.
ARTEMIS_MODE=embedded
ARTEMIS_BROKER_URL=tcp://127.0.0.1:61616
ARTEMIS_USER=mockup
ARTEMIS_PASSWORD=mockup
# Mail is never delivered in the mock-up; the addresses only have to parse.
MAIL_HOST=127.0.0.1
MAIL_USERNAME=mockup
MAIL_PASSWORD=mockup
ROR_CLIENT_ID=mockup
OPENBIS_USER_NAME=mockup
OPENBIS_USER_PASSWORD=mockup
# The production defaults (Secure, Domain=de.uni-tuebingen.qbic) make a browser
# silently drop the session cookie on http://localhost. Both must be overridden.
SESSION_COOKIE_SECURE=false
SESSION_COOKIE_DOMAIN=''
set +a

# TempDirectory resolves "./tmp" against the working directory and fails the
# context if it is missing, so the app has to be started from a directory that
# already contains it.
mkdir -p "$WORK/tmp" "$WORK/logs"
cd "$WORK"

# Start from an empty log. Without this, a failure line from an earlier run is
# still in the file and the wait loop below reports a start that actually
# succeeded as failed.
: > "$WORK/mockup.log"

echo "==> starting the application"
# productionMode: without it Vaadin serves an empty shell, because the dev-mode
# bundle expects a Vite dev server on another port.
( java -Duser.home="$WORK/home" \
    -Dspring.profiles.active=development \
    -Dserver.port="$APP_PORT" \
    -Dvaadin.productionMode=true \
    -Dqbic.data-management.datasource.url="$(printf '%s' "jdbc:h2:file:$WORK/db-dm;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=VALUE,KEY,ACTION;SCHEMA=data_management;AUTO_SERVER=TRUE")" \
    -Dqbic.data-management.datasource.driver-class-name=org.h2.Driver \
    -Dqbic.data-management.datasource.username=sa \
    -Dqbic.data-management.datasource.password= \
    -Dqbic.data-management.datasource.ddl-auto=none \
    -Dqbic.finance.datasource.url="jdbc:h2:file:$WORK/db-fin;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=VALUE,KEY,ACTION;AUTO_SERVER=TRUE" \
    -Dqbic.finance.datasource.driver-class-name=org.h2.Driver \
    -Dqbic.finance.datasource.username=sa \
    -Dqbic.finance.datasource.password= \
    -Dqbic.finance.datasource.ddl-auto=update \
    -Djobrunr.database.datasource.url="jdbc:h2:file:$WORK/db-jobrunr;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=VALUE,KEY,ACTION" \
    -Djobrunr.database.datasource.driver-class-name=org.h2.Driver \
    -Djobrunr.database.datasource.username=sa \
    -Djobrunr.database.datasource.password= \
    -Djobrunr.background-job-server.enabled=false \
    -Djobrunr.dashboard.enabled=false \
    -jar "$JAR" > "$WORK/mockup.log" 2>&1 &
  echo $! > "$WORK/app.pid" )

# Wait for the server to come up, but stop as soon as it is clear it will not.
# Two independent signals, because either can come first:
#   * the log says so -- Spring writes "APPLICATION FAILED TO START", not the
#     "Application run failed" this loop used to look for, so a port clash
#     looked like a slow start and the script waited the full timeout;
#   * the JVM we started has exited.
APP_PID=$(cat "$WORK/app.pid" 2>/dev/null || true)
for _ in $(seq 1 180); do
  if grep -qE 'Tomcat started|Started DataManagerApplication|APPLICATION FAILED TO START|Application run failed' \
       "$WORK/mockup.log" 2>/dev/null; then
    break
  fi
  if [ -n "$APP_PID" ] && ! kill -0 "$APP_PID" 2>/dev/null; then
    break
  fi
  sleep 1
done

# Report the first line that names a cause. Spring prints a "Description:"
# block for some failures (port clash) and only a stack trace for others (a bean
# that could not be created), so look for both.
startup_reason() {
  # The innermost "Caused by" is the actual cause; the outer ones only restate
  # it. Written without relying on line-anchored matching: the log carries ANSI
  # colour codes, so a pattern like "^Caused by:" only matches when colour is
  # off.
  inner=$(grep -a 'Caused by:' "$WORK/mockup.log" 2>/dev/null | tail -1)
  if [ -n "$inner" ]; then
    echo "$inner" | sed 's/.*Caused by: *//' | cut -c1-300
    return
  fi
  # Spring's own summary, when it prints one (a port clash does).
  grep -a -A 3 'APPLICATION FAILED TO START' "$WORK/mockup.log" 2>/dev/null \
    | grep -av 'APPLICATION FAILED' | grep -av '^\*' | grep -av '^$' | head -1 | cut -c1-300
}

if grep -qaE 'APPLICATION FAILED TO START|Application run failed' "$WORK/mockup.log" 2>/dev/null \
   || { [ -n "$APP_PID" ] && ! kill -0 "$APP_PID" 2>/dev/null; }; then
  reason=$(startup_reason || true)
  echo "the application failed to start." >&2
  [ -n "$reason" ] && echo "  $reason" >&2
  echo >&2
  echo "full log: $WORK/mockup.log" >&2
  exit 3
fi

if [ "$NO_PROXY" = 1 ]; then
  echo
  echo "  application  http://localhost:$PORT   (login form, no proxy)"
  echo "  log          $WORK/mockup.log"
else
  echo "==> starting the session proxy"
  node "$REPO/scripts/mockup/session-proxy.js" \
    --listen "$PORT" --target "$APP_PORT" > "$WORK/proxy.log" 2>&1 &
  echo $! > "$WORK/proxy.pid"

  for i in $(seq 1 60); do
    if curl -sf -o /dev/null -m 2 "http://localhost:$PORT/login" 2>/dev/null; then break; fi
    sleep 1
  done

  echo
  echo "  application  http://localhost:$PORT   (already logged in)"
  echo "  direct       http://localhost:$APP_PORT   (login form)"
  echo "  log          $WORK/mockup.log"
  echo "  proxy log    $WORK/proxy.log"
fi

echo
echo "  accounts (login with the EMAIL address, not the username):"
echo "    dev@qbic.local     DevPassword123!"
echo "    admin@qbic.local   AdminPassword123!"
echo
echo "  screenshots  ./scripts/mockup/capture.sh"
