package life.qbic.identity.domain.model.mfa;

/**
 * <b>Second-factor type</b>
 *
 * <p>The kind of credential a {@link SecondFactor} row holds. Stored as a string in
 * {@code user_second_factor.type}.</p>
 *
 * <p>The set is closed in the domain while the column stays an open {@code varchar}, for the same
 * reason {@code TokenPurpose} is closed: a value the code cannot verify must never be written, and
 * a new type must be added deliberately in both places. {@link #TOTP} is the baseline that every
 * user can satisfy without dedicated hardware (AUTH-R-02); {@link #WEBAUTHN} is the
 * phishing-resistant extension that ships only if its deployment prerequisites hold (ADR 0011 W1).</p>
 *
 * @since 1.0.0
 */
public enum SecondFactorType {

  /**
   * Time-based one-time password from an authenticator application (RFC 6238).
   *
   * <p>Carries a shared seed. That seed is the security-critical part: it must be recoverable to
   * compute the expected code, so it is stored as AES-GCM ciphertext, never as a hash
   * (AUTH-NFR-01, ADR 0011 F1/F2).</p>
   */
  TOTP,

  /**
   * Passkey or security key (WebAuthn).
   *
   * <p>Carries no shared secret at all: the server stores the public key and a signature counter, so
   * a database copy yields nothing that can be replayed (AUTH-NFR-01).</p>
   */
  WEBAUTHN
}
