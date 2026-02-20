# Requirements

## Feature: Authentication (F-01)

### Requirements
- R-01.1 The system shall support OAuth2 login.
- R-01.2 The system shall support MFA (TOTP).

### Acceptance criteria
- AC-01.1 Given a valid OAuth2 provider, when the user logs in, then…
- AC-01.2 Given MFA enabled, when the user enters a valid TOTP, then…

### Non-functional requirements
- NFR-01.1 Login p95 < 500ms under normal load.
- NFR-01.2 Audit log entries must be created for login + MFA events.

### Notes / Links
- Related ADRs: (none yet)