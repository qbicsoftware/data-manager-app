#!/usr/bin/env python3
"""
A throwaway SMTP server for verifying the error-notification appender by hand.

WHY THIS EXISTS
  The appender's behaviour depends on what the *server* offers: whether it advertises
  STARTTLS, and whether it accepts AUTH. A fake is the only way to observe the branch
  we care about ("server cannot upgrade to TLS") without breaking a real mail server.

  It prints every command it receives, so the thing to look for is whether an
  "AUTH LOGIN" line ever appears. If the appender is working, it does not: the client
  must refuse before authenticating. That refusal is the fix.

USAGE
  # 1. Server that does NOT support STARTTLS — reproduces the incident.
  #    Expect: EHLO, then no AUTH, and the application logs a send failure.
  python3 scripts/dev/smtp-probe.py --port 2525

  # 2. Same, but also advertises AUTH (so a client that tries anyway is visible).
  python3 scripts/dev/smtp-probe.py --port 2525 --advertise-auth

  # 3. Sanity check the probe itself with swaks/nc:
  printf 'EHLO x\\r\\nQUIT\\r\\n' | nc 127.0.0.1 2525

  Point the application at it:
    MAIL_HOST=127.0.0.1
    MAIL_PORT=2525
    MAIL_USERNAME=someone@example.org
    MAIL_PASSWORD=whatever
    QBIC_MAIL_VERIFY_CONNECTION_ON_STARTUP=false   # the startup gate would refuse to boot
    qbic.logging.error.recipient=you@example.org

  The startup gate and this appender therefore cannot both be exercised at once against
  a non-TLS server — the gate refuses to start, which is the point of the gate. To test
  the appender on its own, disable the gate; to test the gate, leave it on and expect a
  startup failure.

NOTE  This is a development aid. It accepts mail from anyone, stores nothing, and must
      never be exposed beyond localhost.
"""

import argparse
import socket
import sys
import threading


def handle(conn, addr, advertise_auth, advertise_tls):
    def send(line):
        conn.sendall((line + "\r\n").encode())

    try:
        send("220 smtp-probe ready")
        with conn:
            reader = conn.makefile("rb")
            while True:
                raw = reader.readline()
                if not raw:
                    return
                line = raw.decode("utf-8", "replace").rstrip("\r\n")
                print(f"[{addr[1]}] C: {line}", flush=True)
                cmd = line.upper()

                if cmd.startswith("EHLO") or cmd.startswith("HELO"):
                    send("250-smtp-probe greets you")
                    if advertise_auth:
                        send("250-AUTH LOGIN PLAIN")
                    if advertise_tls:
                        send("250-STARTTLS")
                    send("250 8BITMIME")
                elif cmd.startswith("STARTTLS"):
                    # Advertised (if at all) but not implemented: refuse, which is what a
                    # half-configured relay does. A client with starttls.required must fail.
                    send("454 4.7.0 TLS not available")
                elif cmd.startswith("AUTH"):
                    print("        ^^^ AUTH ATTEMPTED — the client sent credentials over an "
                          "unencrypted hop. This is the defect.", flush=True)
                    send("535 5.7.8 Authentication credentials invalid")
                elif cmd.startswith("QUIT"):
                    send("221 bye")
                    return
                else:
                    send("250 ok")
    except (ConnectionResetError, BrokenPipeError):
        pass
    finally:
        print(f"[{addr[1]}] connection closed", flush=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--port", type=int, default=2525)
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--advertise-auth", action="store_true",
                        help="offer AUTH LOGIN/PLAIN in the EHLO reply")
    parser.add_argument("--advertise-tls", action="store_true",
                        help="offer STARTTLS in the EHLO reply (it will still refuse the upgrade)")
    args = parser.parse_args()

    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind((args.host, args.port))
    server.listen(8)
    print(f"smtp-probe listening on {args.host}:{args.port}", flush=True)
    print(f"  advertises AUTH: {args.advertise_auth}, advertises TLS: {args.advertise_tls}",
          flush=True)
    if not args.advertise_tls:
        print("  -> a client with starttls.required must refuse to send; "
              "no AUTH line should ever appear", flush=True)

    try:
        while True:
            conn, addr = server.accept()
            threading.Thread(target=handle, args=(conn, addr, args.advertise_auth,
                                                  args.advertise_tls), daemon=True).start()
    except KeyboardInterrupt:
        print("\nstopped", flush=True)
    finally:
        server.close()


if __name__ == "__main__":
    sys.exit(main())
