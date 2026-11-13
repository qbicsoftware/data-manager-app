#!/usr/bin/env bash
#
# Runs the verification-token specs (FEAT-AUTH-FLOWS-01 / #1678).
#
# Kept in the PR because the MariaDB path is the ONLY way to exercise InnoDB row locking,
# and the single-use guarantee depends on it.
#
#   ./scripts/run-verification-spike.sh              # H2 in MODE=MySQL (default, no setup)
#   ./scripts/run-verification-spike.sh mariadb      # real MariaDB, bootstrapped in a temp dir
#
# The MariaDB run proves the properties the spike cares about on the engine production
# actually uses: InnoDB row locking serialising concurrent consumers, and
# ON DUPLICATE KEY UPDATE keeping every throttle increment. H2's lock semantics are
# not a substitute for that (see docs/plans/spike-verification-token-flow.md).
#
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

MODE="${1:-h2}"
MYSQL_PORT="${MARIADB_PORT:-13310}"

# ---------------------------------------------------------------------------- Java
# sdkman's JDK is not on the default PATH on this machine.
if [[ -z "${JAVA_HOME:-}" ]]; then
  if [[ -d "$HOME/.sdkman/candidates/java/current" ]]; then
    export JAVA_HOME="$HOME/.sdkman/candidates/java/current"
  else
    echo "error: JAVA_HOME is not set and no sdkman JDK was found" >&2
    exit 1
  fi
fi
export PATH="$JAVA_HOME/bin:$PATH"
echo "java: $("$JAVA_HOME/bin/java" -version 2>&1 | head -1)"

# ----------------------------------------------------------------------- Maven args
MVN_ARGS=(
  -o
  -pl datamanager-app
  -am
  test
  -Dtest='VerificationToken*Spec,AuthAttempt*Spec'
  -Dsurefire.failIfNoSpecifiedTests=false
  -DskipITs
)

cleanup() {
  if [[ -n "${MARIADB_PID_FILE:-}" && -f "$MARIADB_PID_FILE" ]]; then
    mysqladmin -h 127.0.0.1 -P "$MYSQL_PORT" -u root -p"$MARIADB_PW" shutdown 2>/dev/null || true
  fi
  if [[ -n "${MARIADB_DIR:-}" && -d "$MARIADB_DIR" ]]; then
    rm -rf "$MARIADB_DIR"
  fi
}
trap cleanup EXIT

case "$MODE" in
  h2)
    echo "Running against H2 (MODE=MySQL)"
    ./mvnw "${MVN_ARGS[@]}" "$@"
    ;;

  mariadb)
    command -v mariadbd >/dev/null || { echo "error: mariadbd not found" >&2; exit 1; }
    command -v mysql_install_db >/dev/null || { echo "error: mysql_install_db not found" >&2; exit 1; }

    MARIADB_DIR="$(mktemp -d)"
    MARIADB_PID_FILE="$MARIADB_DIR/m.pid"
    MARIADB_PW="spikepw"

    echo "Bootstrapping MariaDB in $MARIADB_DIR"
    mkdir -p "$MARIADB_DIR/data"
    mysql_install_db --datadir="$MARIADB_DIR/data" --basedir=/usr/local \
      --auth-root-authentication-method=normal >/dev/null 2>&1

    # No unix socket: sandboxed environments often forbid binding one. TCP is enough here.
    mariadbd --datadir="$MARIADB_DIR/data" --basedir=/usr/local --socket= \
      --port="$MYSQL_PORT" --bind-address=127.0.0.1 \
      --pid-file="$MARIADB_PID_FILE" > "$MARIADB_DIR/err.log" 2>&1 &

    echo "Waiting for MariaDB on port $MYSQL_PORT ..."
    for _ in $(seq 1 40); do
      if mysql -h 127.0.0.1 -P "$MYSQL_PORT" -u root -e 'SELECT 1' >/dev/null 2>&1; then
        break
      fi
      sleep 1
    done
    mysql -h 127.0.0.1 -P "$MYSQL_PORT" -u root -e 'SELECT 1' >/dev/null 2>&1 \
      || { echo "error: MariaDB did not start; see $MARIADB_DIR/err.log" >&2; exit 1; }

    echo "MariaDB: $(mysql -h 127.0.0.1 -P "$MYSQL_PORT" -u root -N -e 'SELECT VERSION()' 2>/dev/null)"

    mysql -h 127.0.0.1 -P "$MYSQL_PORT" -u root -e "
      ALTER USER 'root'@'localhost' IDENTIFIED BY '$MARIADB_PW';
      CREATE DATABASE vertoken     CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
      CREATE DATABASE vertokenconc CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    " 2>/dev/null

    # `users` must exist before the migration DDL: FK_user_verification_token_user
    # fails with errno 150 otherwise. Same definition as sql/complete-schema.sql.
    USERS_DDL="
      CREATE TABLE IF NOT EXISTS \`users\` (
        \`id\` varchar(255) NOT NULL,
        \`active\` bit(1) NOT NULL,
        \`email\` varchar(255) DEFAULT NULL,
        \`encryptedPassword\` varchar(255) DEFAULT NULL,
        \`fullName\` varchar(255) DEFAULT NULL,
        \`registrationDate\` datetime(6) DEFAULT NULL,
        \`userName\` varchar(255) DEFAULT NULL,
        \`oidcId\` varchar(255) DEFAULT NULL,
        \`oidcIssuer\` varchar(255) DEFAULT NULL,
        PRIMARY KEY (\`id\`)
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;"

    echo "Applying the real migration DDL from sql/migrations/"
    for db in vertoken vertokenconc; do
      mysql -h 127.0.0.1 -P "$MYSQL_PORT" -u root -p"$MARIADB_PW" "$db" -e "$USERS_DDL" 2>/dev/null
      mysql -h 127.0.0.1 -P "$MYSQL_PORT" -u root -p"$MARIADB_PW" "$db" \
        < sql/migrations/auth-flow-hardening.sql 2>/dev/null
    done

    echo "Running against MariaDB $MYSQL_PORT"
    # -Dverification.dialect is required: Hibernate cannot infer a dialect from
    # MariaDB 12 through the MySQL driver. Production pins MariaDBDialect instead.
    ./mvnw "${MVN_ARGS[@]}" \
      -Dverification.jdbc.url="jdbc:mysql://127.0.0.1:$MYSQL_PORT/vertoken?useSSL=false&allowPublicKeyRetrieval=true" \
      -Dverification.jdbc.user=root \
      -Dverification.jdbc.password="$MARIADB_PW" \
      -Dverification.jdbc.driver=com.mysql.cj.jdbc.Driver \
      -Dverification.dialect=org.hibernate.dialect.MariaDBDialect
    ;;

  *)
    echo "usage: $0 [h2|mariadb]" >&2
    exit 2
    ;;
esac
