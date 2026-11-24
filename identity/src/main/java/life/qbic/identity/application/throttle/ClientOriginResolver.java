package life.qbic.identity.application.throttle;

/**
 * <b>Supplies the trusted origin of the request being throttled.</b>
 *
 * <p>A port rather than a direct servlet dependency, because {@link AuthAttemptThrottle} lives in
 * the {@code identity} module and must stay free of the web layer — and because it is the seam that
 * lets the specs assert the origin behaviour without an HTTP container.</p>
 *
 * <p>Implementations are expected to read the <em>resolved</em> client address (i.e. after the
 * container's {@code RemoteIpValve} has applied the forwarded headers) and to answer
 * {@link ClientOrigin#unknown()} when that address is not trustworthy. See {@link ClientOrigin} for
 * why an untrustworthy address must not become a bucket key.</p>
 *
 * @since 1.20.4
 */
@FunctionalInterface
public interface ClientOriginResolver {

  /**
   * @return the origin of the request currently being handled, or {@link ClientOrigin#unknown()}
   */
  ClientOrigin resolve();

  /**
   * A resolver that never has an origin to offer.
   *
   * <p>The default in tests and in any deployment that has not configured a trusted proxy. It is
   * deliberately not "everything resolves to the same value": that would be the global-bucket failure
   * {@link ClientOrigin} exists to prevent.</p>
   *
   * @return a resolver that always answers unknown
   */
  static ClientOriginResolver none() {
    return ClientOrigin::unknown;
  }
}
