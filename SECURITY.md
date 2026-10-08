# Security Policy

The Data Manager team takes the security of this software and the research data
it manages seriously. Thank you for helping us keep the project and its users
safe.

## Supported Versions

Security fixes are provided for the latest release only. The project maintains
a single release line, so older releases are not supported and we may ask you
to upgrade before we can address an issue.

| Version | Supported |
|---|---|
| Latest release | :white_check_mark: |
| Older releases | :x: |

## Reporting a Vulnerability

**Please do not report security vulnerabilities through public GitHub issues,
discussions, or pull requests.**

Use GitHub's private vulnerability reporting:

1. Go to the repository's **Security** tab.
2. Click **Report a vulnerability**.
3. Fill in the advisory form with as much detail as possible.

This opens a private security advisory that is only visible to the maintainers.

If you cannot use GitHub private reporting, contact the QBiC maintainers via the
contact details on <https://qbic.uni-tuebingen.de/> and ask for a secure channel
before sharing any details.

### What to include

- A description of the vulnerability and its impact.
- The affected version(s), module, or component.
- Steps to reproduce or a proof of concept.
- Any known mitigations or conditions required to exploit it.
- Whether you would like to be credited in the advisory.

## What to Expect

- **Acknowledgement** within 5 working days.
- **Initial assessment** (severity, affected versions, next steps) within 10
  working days.
- **Coordinated disclosure**: we will agree on a disclosure timeline with you.
  Our default is to publish a GitHub security advisory after a fix is available.
- **Credit**: we are happy to credit reporters in the published advisory unless
  you prefer to remain anonymous.

## Scope

This policy covers the source code in this repository, its build and release
pipelines, and the project's official container/artifacts. It does **not** cover
third-party dependencies, which are tracked separately — see
[docs/security/vulnerability-management.md](docs/security/vulnerability-management.md)
for how dependency CVEs are detected, reported, and mitigated.

## Supply-Chain Security

This project uses OpenSSF Scorecard, Dependabot, CycloneDX SBOM generation, and
Grype CVE scanning. Workflows follow the principle of least privilege and all
GitHub Actions are pinned to immutable commit SHAs. Dependency findings are
published to the repository's Security tab and to the vulnerability report
artifacts. See
[docs/security/vulnerability-management.md](docs/security/vulnerability-management.md)
for details.
