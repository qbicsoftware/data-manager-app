package life.qbic.datamanager.configuration;

import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * <b>Refuse to start when origin-scope throttling is switched on but the client address cannot be
 * trusted.</b>
 *
 * <p>This is the code form of the issue's <em>blocking check</em>: "if
 * {@code getRemoteAddr()} still shows a proxy address after Phase C, stop". A runbook step that says
 * "stop" is only as reliable as the person reading it at 2 a.m. on the node that is already
 * unhealthy, so the check is enforced where it cannot be skipped.</p>
 *
 * <h3>The failure this prevents</h3>
 * <p>Origin scope is only correct when the request origin comes from a component entitled to rewrite
 * it — Tomcat's {@code RemoteIpValve}, which runs under
 * {@code server.forward-headers-strategy=native}. Under {@code framework}
 * ({@code ForwardedHeaderFilter}) the <b>leftmost</b> {@code X-Forwarded-For} entry wins with no trust
 * check, and HAProxy's {@code option forwardfor} appends — so the client writes the entry that becomes
 * the bucket key, mints a fresh bucket per request, and the throttle appears to work while being
 * bypassable. Under {@code none} the address stays the proxy, one bucket covers the whole deployment,
 * and one attacker locks out everybody.</p>
 *
 * <p>Both are strictly worse than the account-scope limitation this work is closing, and both are
 * silent. So they are startup failures.</p>
 *
 * <h3>Why it only fires when origin scope is enabled</h3>
 * <p>The default deployment ships {@code origin.enabled=false}, so this class does nothing there. That
 * is deliberate: enabling the headers also changes how absolute links and the OAuth2 redirect URI are
 * built, and the issue stages that as a separate, independently revertable operator step (Phase C).
 * This gate constrains the operator's step, it does not take it.</p>
 *
 * @since 1.20.4
 */
@Configuration(proxyBeanMethods = false)
public class OriginTrustConfiguration {

  private static final Logger log = LoggerFactory.logger(OriginTrustConfiguration.class);

  /**
   * The only {@code server.forward-headers-strategy} value that is safe on this stack.
   *
   * <p>{@code framework} is named in the message rather than silently rejected, because it is the
   * value the operations documentation used to recommend and an operator following an older copy of
   * it is the realistic way this goes wrong.</p>
   */
  static final String REQUIRED_STRATEGY = "native";

  public OriginTrustConfiguration(
      @Value("${qbic.auth-throttle.login.origin.enabled:false}") boolean originEnabled,
      @Value("${server.forward-headers-strategy:none}") String forwardHeadersStrategy) {
    if (!originEnabled) {
      return;
    }
    String strategy = forwardHeadersStrategy == null ? "none" : forwardHeadersStrategy.strip();
    // Boot binds the enum case-insensitively; `native` and `NATIVE` both mean the valve runs.
    if (REQUIRED_STRATEGY.equalsIgnoreCase(strategy)) {
      log.info("Origin-scope throttling is enabled with forward-headers-strategy=native");
      return;
    }
    throw new IllegalStateException(("""
        qbic.auth-throttle.login.origin.enabled=true requires \
        server.forward-headers-strategy=native, but it is currently '%s'.

        Origin-scope throttling keys a bucket on the client network. It is only correct when that \
        network comes from a component that may rewrite it:
          * native    - Tomcat RemoteIpValve; trusts X-Forwarded-For only from a peer inside \
        server.tomcat.remoteip.internal-proxies, then walks it right-to-left and discards everything \
        client-supplied. This is the only safe option on this stack.
          * framework - ForwardedHeaderFilter; takes the LEFTMOST X-Forwarded-For entry with no trust \
        check. HAProxy's `option forwardfor` appends, so the client owns its own rate-limit identity \
        and can mint a fresh bucket per request.
          * none      - the address stays the proxy; one bucket covers the whole deployment and one \
        attacker locks out every user.

        Either set server.forward-headers-strategy=native (and make sure HAProxy REPLACES \
        X-Forwarded-For, e.g. `http-request set-header X-Forwarded-For %%[src]`), or leave \
        qbic.auth-throttle.login.origin.enabled=false to keep account-scope throttling. See \
        docs/operations/installation-and-administration.md 1.2, 1.5 and 1.7.""")
        .formatted(strategy));
  }
}
