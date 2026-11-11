package life.qbic.identity.domain.model.verification;

/**
 * <b>Token purpose</b>
 *
 * <p>The flow a verification token was issued for. Stored as a string in
 * {@code user_verification_token.purpose}.</p>
 *
 * <p>Keeping this a closed set in the domain (while the column stays an open {@code varchar}) means
 * a token issued for one flow can never be consumed by another — a confirmation token must not be
 * able to reset a password.</p>
 *
 * @since 1.0.0
 */
public enum TokenPurpose {

  /** Completes a registration by confirming the email address. */
  EMAIL_CONFIRMATION,

  /** Grants one password change for an existing account. */
  PASSWORD_RESET,

  /**
   * One-time invitation for an account that has no password (the ORCID-only population).
   *
   * <p>Sets the password <em>and</em> confirms the address in one step: those accounts are
   * {@code active = false} and cannot log in to confirm separately.</p>
   */
  SET_PASSWORD
}
