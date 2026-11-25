#!/usr/bin/env python3
"""Check the project's SBOM against Spring's own security advisory feed.

Why this exists
---------------
Grype, and every other CVE tool, builds its database from NVD, the CVE.org
record API, OSV and GitHub Security Advisories. Spring publishes its
advisories as a coordinated disclosure FIRST: for a window of days or weeks an
advisory exists only on spring.io, in none of those feeds. During that window a
Grype-only scan reports zero findings for the very framework the application is
built on.

This script closes that window. It reads Spring's advisory feed directly and
matches it against the CycloneDX SBOM, so a fresh Spring advisory shows up in CI
on the day it is published rather than whenever it propagates downstream.

It deliberately fails loudly
----------------------------
A feed that cannot be fetched, or one that parses to zero advisories, is
reported as an ERROR with a non-zero exit. A silent empty result is exactly the
failure mode this script exists to prevent, so "no data" must never be rendered
as "no vulnerabilities".

Usage
-----
  spring-advisories.py --sbom target/datamanager-sbom.json
                       [--feed FILE] [--out spring-findings.json]
                       [--merge-into grype.json] [--sarif spring.sarif]
                       [--gate critical,high] [--print-table]

Exit codes
----------
  0  check ran, nothing at or above the --gate severities
  1  check ran, something at or above the --gate severities was found
  2  the check could not be completed (feed unreachable/empty, SBOM missing)
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
import urllib.error
import urllib.request

FEED_URL = "https://spring.io/page-data/security/page-data.json"
FEED_ORIGIN = "https://spring.io/security"
DEFAULT_LEDGER = ".github/grype.yaml"
SEVERITY_ORDER = {"CRITICAL": 0, "HIGH": 1, "MEDIUM": 2, "LOW": 3, "UNKNOWN": 4}


class CheckError(RuntimeError):
    """The check could not be completed; never treat this as 'clean'."""


def log(msg: str) -> None:
    print(msg, file=sys.stderr)


# --------------------------------------------------------------------------
# version comparison
# --------------------------------------------------------------------------

def parse_version(value: str):
    """Turn a Spring version into a comparable tuple.

    Handles the shapes Spring actually publishes: 7.0.10, 4.1.2.1 (enterprise
    patch on top of an OSS release), 5.2.26.RELEASE. Anything unparseable
    returns None, and the caller then skips the range rather than guessing -
    a wrong comparison silently hides a finding.
    """
    if not value:
        return None
    text = value.strip()
    if text.upper().endswith(".RELEASE"):
        text = text[: -len(".RELEASE")]
    out = []
    for part in text.split("."):
        if part.isdigit():
            out.append(int(part))
            continue
        match = re.match(r"^(\d+)", part)
        if not match:
            return None
        out.append(int(match.group(1)))
    return tuple(out)


def compare_versions(left: str, right: str):
    """Return -1, 0 or 1, or None when either side is unparseable."""
    a, b = parse_version(left), parse_version(right)
    if a is None or b is None:
        return None
    width = max(len(a), len(b))
    a = a + (0,) * (width - len(a))
    b = b + (0,) * (width - len(b))
    return (a > b) - (a < b)


def version_in_range(version: str, lower, upper) -> bool:
    """Is the version inside the inclusive range [lower, upper]?"""
    if lower:
        cmp = compare_versions(version, lower)
        if cmp is None or cmp < 0:
            return False
    if upper:
        cmp = compare_versions(version, upper)
        if cmp is None or cmp > 0:
            return False
    return True


# --------------------------------------------------------------------------
# feed + SBOM
# --------------------------------------------------------------------------

def fetch_feed(feed_path):
    if feed_path:
        try:
            with open(feed_path, encoding="utf-8") as handle:
                return json.load(handle)
        except OSError as exc:
            raise CheckError("cannot read feed file {}: {}".format(feed_path, exc))
        except json.JSONDecodeError as exc:
            raise CheckError("feed file {} is not valid JSON: {}".format(feed_path, exc))

    request = urllib.request.Request(
        FEED_URL, headers={"User-Agent": "datamanager-spring-advisory-check"}
    )
    try:
        with urllib.request.urlopen(request, timeout=60) as response:
            body = response.read().decode("utf-8")
    except (urllib.error.URLError, urllib.error.HTTPError, TimeoutError) as exc:
        raise CheckError("cannot fetch Spring advisory feed {}: {}".format(FEED_URL, exc))
    try:
        return json.loads(body)
    except json.JSONDecodeError as exc:
        raise CheckError("Spring advisory feed is not valid JSON: {}".format(exc))


def advisories_from_feed(document):
    """Extract the advisory list, treating an empty feed as an error."""
    try:
        nodes = document["result"]["data"]["cves"]["nodes"]
    except (KeyError, TypeError):
        raise CheckError(
            "Spring advisory feed has an unexpected shape (expected "
            "result.data.cves.nodes) - the feed format changed, so the check "
            "must be fixed rather than skipped"
        )
    if not isinstance(nodes, list):
        raise CheckError("Spring advisory feed 'nodes' is not a list")

    advisories = []
    for node in nodes:
        fields = node.get("fields") or {}
        raw = fields.get("affected")
        if not raw:
            continue
        try:
            affected = json.loads(raw) if isinstance(raw, str) else raw
        except json.JSONDecodeError:
            # An advisory we cannot parse is a coverage hole, not a pass.
            log(
                "::warning::unparseable 'affected' block on Spring advisory {}; "
                "skipped".format(fields.get("slug", "?"))
            )
            continue
        advisories.append(
            {
                "cve": str(fields.get("slug", "")).upper(),
                "severity": str(fields.get("severity", "UNKNOWN")).upper(),
                "published": fields.get("publishedAt", ""),
                "project": fields.get("project", ""),
                "artifacts": affected.get("artifacts") or [],
                "ranges": affected.get("versions") or [],
            }
        )

    if not advisories:
        raise CheckError(
            "Spring advisory feed parsed to zero advisories with version ranges. "
            "Spring has never had an empty advisory set, so this is treated as a "
            "broken check rather than a clean result."
        )
    return advisories


def parse_accepted_risks(path):
    """Read the accepted-risk ledger from .github/grype.yaml.

    Grype applies its own ignore list inside the scanner, so a merged Spring
    finding would otherwise have no suppression path at all: the severity gate
    would fail on main forever, with no documented way to accept a risk that has
    no OSS fix. This reads the same ledger for the same reason.

    The file is a simple, project-controlled shape, so it is parsed directly
    rather than pulling a YAML dependency into CI:

        ignore:
          - vulnerability: CVE-2026-12345
            package:
              name: org.example:some-lib
              version: "1.2.3"
            reason: "... exp:2026-06-30; see issue #1234"

    An entry without a future exp: date is NOT honoured. The documented rule is
    that every acceptance carries an expiry, and an entry that expires silently
    is how an accepted risk becomes a permanent blind spot.
    """
    from datetime import date

    try:
        with open(path, encoding="utf-8") as handle:
            lines = handle.readlines()
    except FileNotFoundError:
        return []
    except OSError as exc:
        raise CheckError("cannot read accepted-risk ledger {}: {}".format(path, exc))

    entries = []
    current = None
    in_package = False
    for raw in lines:
        line = raw.split("#", 1)[0].rstrip()
        if not line.strip():
            continue
        stripped = line.strip()
        if stripped.startswith("- vulnerability:"):
            if current:
                entries.append(current)
            current = {"vulnerability": stripped.split(":", 1)[1].strip(), "reason": ""}
            in_package = False
        elif current is None:
            continue
        elif stripped.startswith("package:"):
            in_package = True
        elif in_package and stripped.startswith("name:"):
            current["package"] = stripped.split(":", 1)[1].strip().strip('"')
        elif stripped.startswith("reason:"):
            current["reason"] = stripped.split(":", 1)[1].strip().strip('"')
            in_package = False
    if current:
        entries.append(current)

    honoured = []
    for entry in entries:
        match = re.search(r"exp:(\d{4}-\d{2}-\d{2})", entry.get("reason") or "")
        if not match:
            log(
                "::warning::accepted-risk entry for {} has no exp:YYYY-MM-DD in "
                "its reason, so it is not honoured".format(entry["vulnerability"])
            )
            continue
        try:
            expiry = date.fromisoformat(match.group(1))
        except ValueError:
            log(
                "::warning::accepted-risk entry for {} has an unparseable expiry "
                "{}; not honoured".format(entry["vulnerability"], match.group(1))
            )
            continue
        if expiry < date.today():
            log(
                "::warning::accepted-risk entry for {} expired on {} and must be "
                "re-assessed; not honoured".format(entry["vulnerability"], expiry)
            )
            continue
        entry["expires"] = expiry.isoformat()
        honoured.append(entry)
    return honoured


def apply_accepted_risks(findings, accepted):
    """Split findings into kept and suppressed against the ledger."""
    if not accepted:
        return findings, []
    kept, suppressed = [], []
    for finding in findings:
        hit = None
        for entry in accepted:
            if entry["vulnerability"].upper() != finding["cve"].upper():
                continue
            package = entry.get("package")
            if package and package != finding["artifact"]:
                continue
            hit = entry
            break
        if hit:
            suppressed.append(dict(finding, accepted_reason=hit.get("reason", ""),
                                   accepted_until=hit.get("expires")))
        else:
            kept.append(finding)
    for finding in suppressed:
        log(
            "suppressed {} for {} {}: accepted risk until {} ({})".format(
                finding["cve"], finding["artifact"], finding["version"],
                finding["accepted_until"], finding["accepted_reason"],
            )
        )
    return kept, suppressed


def load_sbom(path):
    try:
        with open(path, encoding="utf-8") as handle:
            document = json.load(handle)
    except OSError as exc:
        raise CheckError("cannot read SBOM {}: {}".format(path, exc))
    except json.JSONDecodeError as exc:
        raise CheckError("SBOM {} is not valid JSON: {}".format(path, exc))

    components = {}
    for component in document.get("components") or []:
        group = component.get("group") or ""
        name = component.get("name") or ""
        version = component.get("version") or ""
        if not name or not version:
            continue
        coordinate = "{}:{}".format(group, name) if group else name
        components.setdefault(coordinate, set()).add(version)
    if not components:
        raise CheckError("SBOM {} contains no versioned components".format(path))
    return components


# --------------------------------------------------------------------------
# matching
# --------------------------------------------------------------------------

def match(advisories, components):
    findings = []
    for advisory in advisories:
        for version_range in advisory["ranges"]:
            # A range may narrow the artifact list. The 3.5.x branch of a Spring
            # Boot advisory, for example, only affects two of its artifacts.
            # Fall back to the advisory-level list when it does not.
            artifacts = version_range.get("artifacts") or advisory["artifacts"]
            if not artifacts:
                continue
            lower = version_range.get("introduced")
            upper = version_range.get("last-affected")
            raw_fixes = [f for f in version_range.get("fixes", []) if f.get("version")]
            # Spring lists both an OSS and an Enterprise-Support-only fix
            # (4.1.2.1 sits beside 4.1.3). Recommending the paid one to this
            # project would send the reader to a coordinate it cannot resolve,
            # so OSS fixes are preferred and enterprise ones are kept only as a
            # fallback when no OSS fix exists.
            oss_fixes = [
                f["version"] for f in raw_fixes if f.get("availability", "oss") == "oss"
            ]
            fixes = oss_fixes or [f["version"] for f in raw_fixes]
            enterprise_only = [f["version"] for f in raw_fixes if f.get("availability") == "enterprise"]
            for artifact in artifacts:
                for version in sorted(components.get(artifact, ())):
                    if not version_in_range(version, lower, upper):
                        continue
                    findings.append(
                        {
                            "cve": advisory["cve"],
                            "severity": advisory["severity"],
                            "published": advisory["published"],
                            "project": advisory["project"],
                            "artifact": artifact,
                            "version": version,
                            "range": {"introduced": lower, "last_affected": upper},
                            "fixes": fixes,
                            "enterpriseFixes": enterprise_only,
                            "url": "{}/{}".format(FEED_ORIGIN, advisory["cve"].lower()),
                        }
                    )

    # One advisory can match through several ranges; keep one row per artifact.
    deduped = {}
    for finding in findings:
        key = (finding["cve"], finding["artifact"], finding["version"])
        deduped.setdefault(key, finding)
    return sorted(
        deduped.values(),
        key=lambda f: (SEVERITY_ORDER.get(f["severity"], 9), f["cve"], f["artifact"]),
    )


# --------------------------------------------------------------------------
# outputs
# --------------------------------------------------------------------------

def as_grype_matches(findings):
    """Render findings in Grype's match shape.

    The existing report renderer, badge and summary consume that shape, so one
    report keeps a single set of totals across every surface.
    """
    matches = []
    for finding in findings:
        fixes = finding["fixes"]
        matches.append(
            {
                "vulnerability": {
                    "id": finding["cve"],
                    "severity": finding["severity"].capitalize(),
                    "dataSource": FEED_ORIGIN,
                    "fix": {
                        "versions": fixes,
                        "state": "fixed" if fixes else "unknown",
                    },
                },
                "artifact": {
                    "name": finding["artifact"],
                    "version": finding["version"],
                    "type": "java-archive",
                },
                "matchDetails": [
                    {
                        "type": "spring-advisory",
                        "matcher": "spring.io",
                        "searchedBy": {
                            "range": finding["range"],
                            "published": finding["published"],
                        },
                    }
                ],
            }
        )
    return matches


def merge_into_grype(report_path, findings):
    """Append the Spring findings to a Grype JSON report."""
    try:
        with open(report_path, encoding="utf-8") as handle:
            report = json.load(handle)
    except OSError as exc:
        raise CheckError("cannot read Grype report {}: {}".format(report_path, exc))
    except json.JSONDecodeError as exc:
        raise CheckError("Grype report {} is not valid JSON: {}".format(report_path, exc))

    existing = report.setdefault("matches", [])
    if existing is None:
        existing = report["matches"] = []
    already = set()
    for entry in existing:
        already.add(
            (
                (entry.get("vulnerability") or {}).get("id"),
                (entry.get("artifact") or {}).get("name"),
                (entry.get("artifact") or {}).get("version"),
            )
        )

    added = 0
    for entry in as_grype_matches(findings):
        key = (
            entry["vulnerability"]["id"],
            entry["artifact"]["name"],
            entry["artifact"]["version"],
        )
        if key in already:
            continue
        existing.append(entry)
        already.add(key)
        added += 1

    report.setdefault("source", {})
    report["source"]["springAdvisoryFeed"] = FEED_URL
    with open(report_path, "w", encoding="utf-8") as handle:
        json.dump(report, handle, indent=2)
    log("merged {} Spring advisory finding(s) into {}".format(added, report_path))


def write_sarif(path, findings):
    """Emit SARIF so Spring findings also reach Security / Code scanning."""
    rules = {}
    results = []
    for index, finding in enumerate(findings):
        level = (
            "error"
            if finding["severity"] in ("CRITICAL", "HIGH")
            else "warning"
            if finding["severity"] == "MEDIUM"
            else "note"
        )
        rules.setdefault(
            finding["cve"],
            {
                "id": finding["cve"],
                "name": finding["cve"],
                "shortDescription": {
                    "text": "{} advisory {}".format(finding["project"], finding["cve"])
                },
                "fullDescription": {
                    "text": "{} {} is affected by {}. Fix: {}".format(
                        finding["artifact"],
                        finding["version"],
                        finding["cve"],
                        ", ".join(finding["fixes"]) or "none",
                    )
                },
                "helpUri": finding["url"],
                "defaultConfiguration": {"level": level},
            },
        )
        results.append(
            {
                "ruleId": finding["cve"],
                "level": level,
                "message": {
                    "text": "{} {} is affected by {} ({}). Upgrade to {}.".format(
                        finding["artifact"],
                        finding["version"],
                        finding["cve"],
                        finding["severity"],
                        ", ".join(finding["fixes"]) or "a fixed version",
                    )
                },
                "locations": [
                    {
                        "physicalLocation": {
                            "artifactLocation": {"uri": "datamanager-bom/pom.xml"},
                            "region": {"startLine": index + 1, "endLine": index + 1},
                        }
                    }
                ],
            }
        )

    sarif = {
        "version": "2.1.0",
        "$schema": "https://json.schemastore.org/sarif-2.1.0.json",
        "runs": [
            {
                "tool": {
                    "driver": {
                        "name": "spring-advisories",
                        "informationUri": FEED_ORIGIN,
                        "rules": list(rules.values()),
                    }
                },
                "results": results,
            }
        ],
    }
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(sarif, handle, indent=2)
    log("wrote {} Spring advisory result(s) to {}".format(len(results), path))


def print_table(findings):
    if not findings:
        print("No Spring advisories affect the dependencies in this SBOM.")
        return
    width = max(len(f["artifact"]) for f in findings)
    header = "SEVERITY  ADVISORY         {} VERSION    FIXED IN".format(
        "ARTIFACT".ljust(width)
    )
    print(header)
    print("-" * len(header))
    for finding in findings:
        print(
            "{:9} {:15} {} {:10} {}".format(
                finding["severity"],
                finding["cve"],
                finding["artifact"].ljust(width),
                finding["version"],
                ", ".join(finding["fixes"]) or "-",
            )
        )
    counts = {}
    for finding in findings:
        counts[finding["severity"]] = counts.get(finding["severity"], 0) + 1
    summary = ", ".join(
        "{} {}".format(count, severity.lower())
        for severity, count in sorted(
            counts.items(), key=lambda kv: SEVERITY_ORDER.get(kv[0], 9)
        )
    )
    print(
        "\n{} Spring advisory finding(s) affect this SBOM: {}".format(
            len(findings), summary
        )
    )


# --------------------------------------------------------------------------

def self_check():
    """Assert the matching rules that a regression would silently break.

    These are the cases that decide whether a real finding is reported or hidden.
    Running them in CI means a change to the range logic cannot quietly turn a
    critical into a non-finding.
    """
    cases = [
        ("4.1.2", "4.1.0", "4.1.2", True, "last-affected is inclusive"),
        ("4.1.1", "4.1.0", "4.1.2", True, "inside the range"),
        ("4.0.9", "4.1.0", "4.1.2", False, "below the range"),
        ("4.1.3", "4.1.0", "4.1.2", False, "OSS fix above the range"),
        ("4.1.2.1", "4.1.0", "4.1.2", False, "enterprise fix above the range"),
        ("7.0.10", "7.0.0", "7.0.10", True, "4-part comparison"),
        ("7.0.11", "7.0.0", "7.0.10", False, "4-part comparison, fixed"),
        ("5.2.26.RELEASE", "5.3.0", "5.3.50", False, "RELEASE suffix below"),
        ("5.3.51", "5.3.0", "5.3.50", False, "RELEASE-style fix above"),
    ]
    failures = 0
    for version, lower, upper, expected, label in cases:
        got = version_in_range(version, lower, upper)
        if got != expected:
            failures += 1
            print(
                "SELF-CHECK FAIL: {} in [{}, {}] -> {} (expected {}) - {}".format(
                    version, lower, upper, got, expected, label
                ),
                file=sys.stderr,
            )
    if failures:
        print(
            "::error::{} Spring advisory version-comparison self-check(s) "
            "failed; the matcher is unreliable and its results must not be "
            "trusted".format(failures),
            file=sys.stderr,
        )
        return 2
    log("self-check: {} version-comparison cases pass".format(len(cases)))
    return 0


def main(argv):
    parser = argparse.ArgumentParser(
        description="Check an SBOM against Spring's security advisory feed."
    )
    parser.add_argument("--sbom", default="target/datamanager-sbom.json")
    parser.add_argument("--feed", default=None, help="read the feed from a file")
    parser.add_argument("--out", default="spring-findings.json")
    parser.add_argument("--merge-into", default=None, help="merge into a Grype JSON report")
    parser.add_argument("--sarif", default=None)
    parser.add_argument(
        "--gate", default=None, help="comma-separated severities that fail the job"
    )
    parser.add_argument(
        "--accept",
        default=None,
        help="accepted-risk ledger (defaults to .github/grype.yaml when present)",
    )
    parser.add_argument("--self-check", action="store_true")
    parser.add_argument("--print-table", action="store_true")
    args = parser.parse_args(argv)

    if args.self_check:
        return self_check()

    try:
        advisories = advisories_from_feed(fetch_feed(args.feed))
        components = load_sbom(args.sbom)
        log(
            "checked {} SBOM coordinate(s) against {} Spring advisories".format(
                len(components), len(advisories)
            )
        )

        findings = match(advisories, components)

        ledger = args.accept
        if ledger is None and os.path.exists(DEFAULT_LEDGER):
            ledger = DEFAULT_LEDGER
        accepted = parse_accepted_risks(ledger) if ledger else []
        findings, suppressed = apply_accepted_risks(findings, accepted)

        with open(args.out, "w", encoding="utf-8") as handle:
            json.dump(
                {
                    "source": FEED_URL,
                    "advisoryCount": len(advisories),
                    "ledger": ledger,
                    "findings": findings,
                    "suppressed": suppressed,
                },
                handle,
                indent=2,
            )
        if args.print_table:
            print_table(findings)
        if args.merge_into:
            merge_into_grype(args.merge_into, findings)
        if args.sarif:
            write_sarif(args.sarif, findings)
    except CheckError as exc:
        # Not a pass: the check did not run at all.
        print(
            "::error::Spring advisory check could not be completed: {}".format(exc),
            file=sys.stderr,
        )
        return 2

    if args.gate:
        gated = {s.strip().upper() for s in args.gate.split(",") if s.strip()}
        blocking = [f for f in findings if f["severity"] in gated]
        if blocking:
            for finding in blocking:
                print(
                    "::error::Spring advisory {} ({}) affects {} {}; fix: {} ({})".format(
                        finding["cve"],
                        finding["severity"],
                        finding["artifact"],
                        finding["version"],
                        ", ".join(finding["fixes"]) or "no OSS fix yet",
                        finding["url"],
                    )
                )
            return 1
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
