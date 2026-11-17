package life.qbic.identity.application.mfa;

/**
 * <b>The secret of a second factor could not be recovered</b>
 *
 * <p>Thrown when the stored ciphertext cannot be decrypted — the vault key is missing or different,
 * or the row is corrupted. Callers must treat this as <em>the factor being unavailable</em>: deny the
 * login, do not fall through to "no factor enrolled" and definitely do not accept the attempt. That
 * is the fail-closed rule of AUTH-NFR-02.</p>
 *
 * <p>The message never contains the ciphertext or the seed.</p>
 *
 * @since 1.0.0
 */
public class SecondFactorSecretUnavailableException extends RuntimeException {

  public SecondFactorSecretUnavailableException(String message) {
    super(message);
  }

  public SecondFactorSecretUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
