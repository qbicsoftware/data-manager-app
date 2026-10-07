#!/usr/bin/env bash
#
# Renders a Grype JSON report into a human-readable Markdown report, a GitHub
# Actions job summary, and badge data for schneegans/dynamic-badges-action.
#
# Usage: render-grype-report.sh [grype.json]
#
# Outputs:
#   - dependency-vulnerability-report.md   (artifact / report body)
#   - $GITHUB_STEP_SUMMARY                 (job summary, if set)
#   - $GITHUB_OUTPUT                       (badge_message, badge_color, counts)
#
set -euo pipefail

REPORT="${1:-grype.json}"
OUT="dependency-vulnerability-report.md"

if [[ ! -f "$REPORT" ]]; then
  echo "::error file=${REPORT}::Grype report not found. Did the SBOM scan run?"
  exit 1
fi

sev_count() {
  jq --arg s "$1" \
    '[.matches[]? | select((.vulnerability.severity // "Unknown") == $s)] | length' \
    "$REPORT"
}

critical=$(sev_count Critical)
high=$(sev_count High)
medium=$(sev_count Medium)
low=$(sev_count Low)
negligible=$(sev_count Negligible)
unknown=$(sev_count Unknown)
total=$(jq '[.matches[]?] | length' "$REPORT")

# Worst severity present drives the badge colour.
# shellcheck disable=SC2015
if   (( critical > 0 )); then badge_color="#b60205"
elif (( high     > 0 )); then badge_color="#d93f0b"
elif (( medium   > 0 )); then badge_color="#fbca04"
elif (( low      > 0 )); then badge_color="#97ca00"
else                          badge_color="#0e8a16"
fi

if (( total == 0 )); then
  badge_message="0 known"
else
  badge_message="${total} total (${critical} crit, ${high} high)"
fi

# ---- Markdown report -------------------------------------------------------
{
  echo "# Dependency vulnerability report"
  echo
  echo "_Generated $(date -u '+%Y-%m-%d %H:%M UTC') from the CycloneDX SBOM of this"
  echo "revision, scanned with [Grype](https://github.com/anchore/grype)._"
  echo
  echo "## Summary"
  echo
  echo "| Severity | Count |"
  echo "|----------|------:|"
  printf '| Critical | %s |\n'   "$critical"
  printf '| High | %s |\n'       "$high"
  printf '| Medium | %s |\n'     "$medium"
  printf '| Low | %s |\n'        "$low"
  printf '| Negligible | %s |\n' "$negligible"
  printf '| Unknown | %s |\n'    "$unknown"
  printf '| **Total** | **%s** |\n' "$total"
  echo

  if (( total == 0 )); then
    echo "No known vulnerabilities were reported for this revision."
    echo
  fi

  if (( critical + high > 0 )); then
    echo "## Findings requiring action (Critical / High)"
    echo
    echo "| CVE | Severity | Package | Installed | Fixed version | Suggested mitigation |"
    echo "|-----|----------|---------|-----------|---------------|----------------------|"
    jq -r '
      def rank: if . == "Critical" then 0 elif . == "High" then 1 else 2 end;
      [ .matches[]?
        | select((.vulnerability.severity // "Unknown") == "Critical"
              or (.vulnerability.severity // "Unknown") == "High") ]
      | sort_by(.vulnerability.severity | rank)
      | .[]
      | [ (.vulnerability.id // "?"),
          (.vulnerability.severity // "Unknown"),
          (.artifact.name // "?"),
          (.artifact.version // "?"),
          ((.vulnerability.fix.versions // []) | join(", ")),
          (.vulnerability.fix.state // "unknown") ]
      | join("\u001f")
    ' "$REPORT" | while IFS=$'\x1f' read -r id severity pkg installed fixed state; do
        if [[ -n "$fixed" ]]; then
          mitigation="Upgrade \`${pkg}\` to \`${fixed}\` (pin in \`datamanager-bom/pom.xml\`)"
        else
          mitigation="No upstream fix (${state}); check reachability, else accept in \`.github/grype.yaml\` with reason + expiry"
        fi
        printf '| `%s` | %s | `%s` | `%s` | %s | %s |\n' \
          "$id" "$severity" "$pkg" "$installed" "${fixed:-—}" "$mitigation"
      done
    echo
  fi

  echo "## How to mitigate"
  echo
  echo "1. **Upgrade** the affected coordinate. Third-party versions are centralised in"
  echo "   \`datamanager-bom/pom.xml\` — bump the property or the managed dependency there,"
  echo "   and override transitive dependencies via \`<dependencyManagement>\`."
  echo "2. **Verify reachability** when no fix exists and record the assessment on the"
  echo "   tracking issue."
  echo "3. **Accept the risk** only with a documented reason and expiry in"
  echo "   \`.github/grype.yaml\`. Expired acceptances are re-reviewed — see"
  echo "   \`docs/security/vulnerability-management.md\`."
  echo "4. The full machine-readable list is attached as \`grype.json\`; findings also surface"
  echo "   under **Security → Code scanning** as SARIF."
  echo
  echo "> This report only covers dependencies resolved into the CycloneDX SBOM. Secret"
  echo "> scanning, SAST (CodeQL) and container scanning are handled by separate workflows."
  echo
} > "$OUT"

# ---- Job summary + console output ------------------------------------------
cat "$OUT"
if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
  cat "$OUT" >> "$GITHUB_STEP_SUMMARY"
fi

# ---- Badge data ------------------------------------------------------------
if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
  {
    echo "badge_message=${badge_message}"
    echo "badge_color=${badge_color}"
    echo "total=${total}"
    echo "critical=${critical}"
    echo "high=${high}"
    echo "medium=${medium}"
  } >> "$GITHUB_OUTPUT"
fi
