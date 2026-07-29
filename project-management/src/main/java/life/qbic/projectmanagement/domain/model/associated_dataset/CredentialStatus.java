package life.qbic.projectmanagement.domain.model.associated_dataset;

/**
 * The status of a stored external provider credential.
 *
 * <p>Per ADR-0002 §9, the status is updated <em>only</em> on explicit
 * user action (e.g. the user adds or re-validates a token in the UI).
 * A failed sync operation does <strong>not</strong> silently update the
 * credential status to {@code INVALIDATED} — that would be misleading,
 * since the failure may be transient or caused by insufficient access
 * rather than an expired token.</p>
 *
 * @since 1.12.0
 */
public enum CredentialStatus {

  /**
   * The token was validated and stored successfully. The credential
   * is currently usable for authenticated requests.
   */
  VALID,

  /**
   * The token was explicitly marked as invalidated (e.g. the user
   * removed it or re-validation failed). The stored encrypted blob
   * is retained for audit but must not be used for authentication.
   */
  INVALIDATED;

  public static CredentialStatus parse(String value) {
    try {
      return valueOf(value);
    } catch (IllegalArgumentException | NullPointerException e) {
      throw new IllegalArgumentException("Unknown credential status: " + value, e);
    }
  }

}
