package life.qbic.identity.application.mfa;

/**
 * <b>The enrolment was started too long ago to confirm</b>
 *
 * <p>A started enrolment holds a seed that has never been shown to anyone but the user. If the second
 * step never happens — the tab was closed, the user walked away, the QR code was photographed and the
 * session left open — that seed must stop being confirmable rather than stay valid indefinitely.
 *
 * <p>Recovery is trivial for the user (start again, which issues a fresh seed) and the window is
 * short, so this is a normal path, not an error condition to alert on.</p>
 *
 * @since 1.0.0
 */
public class EnrolmentExpiredException extends RuntimeException {

  public EnrolmentExpiredException(String message) {
    super(message);
  }
}
