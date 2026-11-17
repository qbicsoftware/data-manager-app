package life.qbic.identity.application.mfa;

/**
 * <b>The presented one-time code was not accepted</b>
 *
 * <p>Covers every reason a code can be refused — wrong code, wrong seed, outside the tolerated clock
 * skew, malformed input — because telling the caller which one it was is exactly the oracle
 * AUTH-NFR-03 forbids. The message is for logs and says nothing about the code.</p>
 *
 * @since 1.0.0
 */
public class InvalidTotpCodeException extends RuntimeException {

  public InvalidTotpCodeException(String message) {
    super(message);
  }
}
