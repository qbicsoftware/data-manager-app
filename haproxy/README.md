# HAProxy Custom Error Pages

Custom maintenance / error landing pages for the QBiC Data Manager HAProxy setup.

These pages follow the [HAProxy `errorfile` format](https://www.haproxy.com/documentation/haproxy-configuration-tutorials/alerts-and-monitoring/error-pages/):
each file has a `.http` extension, contains the HTTP status line and required headers at the top,
a blank line separator, and then the full HTML response body.

## Files

| File | HTTP Status | When to Use |
|------|-------------|-------------|
| `errors/503.http` | 503 Service Temporarily Unavailable | Scheduled maintenance or temporary service interruption |
| `errors/maintenance-illustration.svg` | — | Standalone SVG illustration (not used by HAProxy directly) |

## HAProxy Configuration

Add the error pages to your HAProxy configuration. Place the files on your HAProxy host
(e.g. under `/etc/haproxy/errors/`) and reference them in the `defaults` or `frontend` section:

```haproxy
defaults
    # Custom error pages
    errorfile 503 /etc/haproxy/errors/503.http
```

To serve the maintenance page for **all** traffic during a maintenance window (regardless of
backend status), combine it with a maintenance backend:

```haproxy
# --- Maintenance mode ---
# Enable by uncommenting the maintenance block and commenting out the normal backend.

# backend maintenance
#     errorfile 503 /etc/haproxy/errors/503.http
#     server maintenance 127.0.0.1:1 disable

# frontend myapp
#     # Maintenance mode: route everything to the maintenance error page
#     # use_backend maintenance if TRUE
#
#     # Normal mode:
#     use_backend app_servers
```

### Multiple Error Codes

You can also serve the same maintenance page for other status codes during an outage:

```haproxy
defaults
    errorfile 502 /etc/haproxy/errors/503.http
    errorfile 503 /etc/haproxy/errors/503.http
    errorfile 504 /etc/haproxy/errors/503.http
```

## Page Contents

The error page includes:

- **Informative message** — explains the situation to visitors in a friendly way
- **SVG illustration** — fully inline, no external image dependencies
- **Contact information** — support email: `support@qbic.zendesk.com`
- **Provider information** — QBiC – Quantitative Biology Center, University of Tübingen
  with a link to <https://www.info.qbic.uni-tuebingen.de/>

The page is fully self-contained:
- All CSS is inline (no external stylesheets)
- The SVG illustration is embedded inline (no external image files)
- CSS animations bring the illustration to life

### Visual style

- Warm cream colour palette
- Scene: two colleagues with a tilted server rack, smoke and sparks
- Subtle animations: floating papers, flickering LEDs, rising smoke, blinking sparks

## Customisation

### Changing the contact address

Edit the `mailto:` and display text in the `.contact-card` section of `503.http`.

### Changing the page appearance

All styles are in the `<style>` block inside `503.http`. The colour palette is:

| Token | Value | Usage |
|-------|-------|-------|
| Background | `#fefdf7` | Page background |
| Heading | `#c0392b` | Error title |
| Primary | `#2980b9` | Links |
| Card BG | `#f0f4f8` | Contact info card |
| Server rack | `#2d2d2d` / `#4a4a4a` | Rack gradient |
| LEDs | `#e74c3c` | Red error indicators |
| Sparks | `#f1c40f` / `#e74c3c` | Animated sparkle effects |

### Replacing the illustration

Swap the `<svg>` block inside the `.illustration` div with a different SVG or
replace the inline SVG with an `<img>` tag pointing to a standalone image file.

## Format Requirements (HAProxy)

Key rules from HAProxy's `errorfile` specification:

1. File extension **must** be `.http` (not `.html`)
2. File must start with the HTTP status line: `HTTP/1.1 <code> <reason>`
3. Followed by HTTP headers. For `HTTP/1.1` the following are **mandatory** (the parser rejects files missing them):
   - `Content-Length: <bytes>` — must exactly match the body size in bytes; without it HAProxy 2.7+ raises _"unable to parse headers"_ at startup
   - `Content-Type: text/html` (or `text/html; charset=utf-8`)
4. All line endings must be **CRLF** (`\r\n`) — HTTP/1.1 requires it per RFC 7230
5. A **blank line** (`\r\n\r\n`) separates headers from the HTML body
6. The HTML body follows after the blank line
7. The entire file is served verbatim as the HTTP response

## License

Same as the Data Manager project: AGPL-3.0-or-later.
