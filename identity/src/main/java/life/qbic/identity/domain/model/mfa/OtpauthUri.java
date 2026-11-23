package life.qbic.identity.domain.model.mfa;

import static java.util.Objects.requireNonNull;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * <b>Provisioning URI for authenticator applications</b>
 *
 * <p>The {@code otpauth://} URI is the interchange format behind the enrolment QR code: the
 * authenticator scans it (or the user types the seed) and from then on derives codes locally. It is
 * built here, in the domain, so the exact parameters are testable and identical to what
 * {@link Totp#verify} expects — a mismatch between "what the app was told" and "what we verify" is
 * the classic reason a newly enrolled factor never works.</p>
 *
 * <p><b>The URI contains the plaintext seed.</b> It is therefore as sensitive as the seed itself:
 * it must be rendered once at enrolment, never logged, never stored, and never echo back after the
 * enrolment is confirmed. Only the encrypted seed is persisted (AUTH-NFR-01).</p>
 *
 * @since 1.0.0
 */
public final class OtpauthUri {

  /** Shown as the issuer in the authenticator list; also prefixes the account label. */
  public static final String ISSUER = "QBiC Data Manager";

  private OtpauthUri() {
  }

  /**
   * Builds the provisioning URI for a TOTP factor.
   *
   * @param accountLabel what the user will see under the issuer, e.g. their email address
   * @param base32Seed   the plaintext seed
   * @return the {@code otpauth://totp/...} URI, with parameters matching {@link Totp}
   */
  public static String forTotp(String accountLabel, String base32Seed) {
    requireNonNull(accountLabel, "accountLabel must not be null");
    requireNonNull(base32Seed, "base32Seed must not be null");
    if (accountLabel.isBlank()) {
      throw new IllegalArgumentException("accountLabel must not be blank");
    }
    if (base32Seed.isBlank()) {
      throw new IllegalArgumentException("base32Seed must not be blank");
    }
    // The label is "issuer:account". The colon between the two parts is a LITERAL separator, not an
    // encoded character: the Key Uri Format defines the label as an optional issuer prefix followed
    // by a colon, and only the parts are percent-encoded. Encoding the separator as %3A looks
    // equivalent — every app that decodes the label before splitting it agrees — but an app that
    // splits first finds no colon, treats the whole label as the account name, and then falls back to
    // the account's e-mail domain for the issuer. That is how an entry ends up named after the mail
    // provider instead of this application.
    String label = encode(ISSUER) + ":" + encode(accountLabel.strip());
    return "otpauth://totp/" + label
        + "?secret=" + encode(base32Seed.strip())
        + "&issuer=" + encode(ISSUER)
        + "&algorithm=SHA1"
        + "&digits=" + Totp.DIGITS
        + "&period=" + Totp.PERIOD_SECONDS;
  }

  private static String encode(String value) {
    // URLEncoder is form encoding: a space becomes '+', which some authenticator apps render
    // literally. The URI syntax requires %20.
    return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
  }
}
