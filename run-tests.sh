#!/bin/sh
# ---------------------------------------------------------------------------
# run-tests.sh - Make Mockito-based tests runnable in any (agent) environment.
#
# WHY THIS EXISTS
#   Mockito's inline mock maker (default since Mockito 5) self-attaches a
#   Byte Buddy agent at runtime via the JVM attach mechanism (jdk.attach).
#   In sandboxed/spawned contexts (AI coding agents, CI runners) the attach
#   mechanism is often unavailable: the target JVM never creates its
#   .java_pid socket, so Byte Buddy fails with:
#
#     "Could not self-attach to current VM using external process"
#     -> "... MockitoMockMaker could not be instantiated" (Spock)
#     -> "... Could not initialize plugin: interface org.mockito.plugins.MockMaker"
#
#   The reliable fix is to load the agent at JVM startup via -javaagent, which
#   completely bypasses the attach mechanism. This script sets that up.
#
# USAGE
#   ./run-tests.sh                        # exports env (source-friendly) + prints summary
#   source ./run-tests.sh                 # make env available without running Maven
#   ./run-tests.sh test                   # export env, then run: ./mvnw test
#   ./run-tests.sh clean verify           # export env, then run: ./mvnw clean verify
#   ./run-tests.sh --print                # print the export line for shell/env config
#   ./run-tests.sh --help
#
# Implementation notes
#   * Uses JDK_JAVA_OPTIONS: it is inherited by the Maven JVM AND every forked
#     surefire JVM, so no pom.xml / argLine changes are required.
#   * The preferred agent is byte-buddy-agent-X.jar (self-contained). A
#     mockito-core jar is used only as a fallback.
#   * Picks the newest matching jar from the local Maven repository; the agent
#     API is stable, a slightly newer agent is compatible with older Mockito.
# ---------------------------------------------------------------------------

set -u

AGENT_JARS_CANDIDATES_BB="net/bytebuddy/byte-buddy-agent"
AGENT_JARS_CANDIDATES_MK="org/mockito/mockito-core"

# ---------------------------------------------------------------------------
# locate an agent jar in the local Maven repository (newest version wins)
# ---------------------------------------------------------------------------
resolve_agent_jar() {
  _m2=""
  for _cand in \
    "${M2_REPO:-}" \
    "${MAVEN_REPO_LOCAL:-}" \
    "$HOME/.m2/repository" \
    "$HOME/.sdkman/.m2/repository"; do
    if [ -n "$_cand" ] && [ -d "$_cand" ]; then
      _m2="$_cand"
      break
    fi
  done
  [ -n "$_m2" ] || {
    echo "run-tests.sh: could not locate a local Maven repository (~/.m2 expected)." >&2
    echo "Run './mvnw -q dependency:go-offline' once to populate it, then retry." >&2
    return 1
  }
  for _group in "$AGENT_JARS_CANDIDATES_BB" "$AGENT_JARS_CANDIDATES_MK"; do
    _base="$_m2/$_group"
    [ -d "$_base" ] || continue
    _versions="$(ls -1 "$_base" 2>/dev/null | sort -V)"
    _latest=""
    for _v in $_versions; do
      # shellcheck disable=SC2086
      for _j in "$_base/$_v"/"${_group##*/}-$_v.jar"; do
        [ -f "$_j" ] && _latest="$_j"
      done
    done
    if [ -n "$_latest" ]; then
      echo "$_latest"
      return 0
    fi
  done
  echo "run-tests.sh: no byte-buddy-agent or mockito-core jar found in $_m2." >&2
  echo "Run './mvnw -q dependency:go-offline' once to populate the repository, then retry." >&2
  return 1
}

# ---------------------------------------------------------------------------
# main
# ---------------------------------------------------------------------------
MODE="run"
case "${1:-}" in
  --help|-h)
    sed -n '2,32p' "$0"
    exit 0
    ;;
  --print)
    MODE="print"
    ;;
  *)
    ;;
esac

if ! AGENT_JAR="$(resolve_agent_jar)"; then
  exit 1
fi

EXTRA="-Djdk.attach.allowAttachSelf=true"
# preserve any pre-existing JDK_JAVA_OPTIONS
if [ -n "${JDK_JAVA_OPTIONS:-}" ]; then
  case " $JDK_JAVA_OPTIONS " in
    *" -javaagent:"*) ;; # an agent is already configured - keep as is
    *) JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS -javaagent:$AGENT_JAR $EXTRA" ;;
  esac
else
  JDK_JAVA_OPTIONS="-javaagent:$AGENT_JAR $EXTRA"
fi
export JDK_JAVA_OPTIONS

case "$MODE" in
  print)
    echo "export JDK_JAVA_OPTIONS=\"$JDK_JAVA_OPTIONS\""
    exit 0
    ;;
esac

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
JAVA_BIN="${JAVA_HOME:+$JAVA_HOME/bin/java}"
if [ -n "$JAVA_BIN" ]; then
  _jver="$("$JAVA_BIN" -version 2>&1 | head -1)"
else
  _jver="$(java -version 2>&1 | head -1)"
fi

echo "Mockito agent enabled for this environment:"
echo "  agent : $AGENT_JAR"
echo "  java  : ${_jver}"
echo "  env   : JDK_JAVA_OPTIONS=$JDK_JAVA_OPTIONS"
echo "Agents: 'source $0' first, then run './mvnw test' (or any Maven goal)."
echo

# no arguments -> environment is ready, return to the caller
if [ $# -eq 0 ]; then
  return 0 2>/dev/null || exit 0
fi

# arguments given -> run Maven accordingly
if [ -x "$SCRIPT_DIR/mvnw" ]; then
  exec "$SCRIPT_DIR/mvnw" "$@"
fi
exec mvn "$@"