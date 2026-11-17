package life.qbic.datamanager.security;

import static java.util.Objects.requireNonNull;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import life.qbic.identity.application.throttle.ClientOrigin;
import life.qbic.identity.application.throttle.ClientOriginResolver;
import life.qbic.identity.application.throttle.IpNetworks;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * <b>Derives the throttle's request origin from the servlet request.</b>
 *
 * <h3>It reads the address, it does not parse the header</h3>
 * <p>{@link HttpServletRequest#getRemoteAddr()} is used deliberately, and the {@code X-Forwarded-*}
 * headers are never touched here. With {@code server.forward-headers-strategy=native}, Tomcat's
 * {@code RemoteIpValve} has already rewritten the address — and it is the only component in this
 * stack that does so safely:</p>
 * <ul>
 *   <li>it acts only when the connecting peer is inside {@code server.tomcat.remoteip.internal-proxies},
 *       so a request that reaches the application directly cannot claim an origin at all;</li>
 *   <li>it walks {@code X-Forwarded-For} <b>right-to-left</b>, skipping internal proxies, and takes the
 *       first entry that is not one of them, discarding everything to its left as client-supplied.</li>
 * </ul>
 * <p>By contrast {@code server.forward-headers-strategy=framework} uses
 * {@code ForwardedHeaderFilter}, which takes the <b>leftmost</b> entry with no trust check. HAProxy's
 * {@code option forwardfor} <em>appends</em>, so on that strategy the client writes the first entry
 * and therefore owns its own rate-limit identity. See
 * {@code docs/operations/installation-and-administration.md} §1.5 for the verified source lines.</p>
 *
 * <h3>Why an untrustworthy address becomes unknown rather than a bucket</h3>
 * <p>Two cases produce an address that is not an identity:</p>
 * <ol>
 *   <li>the address is unparseable — some proxy emitted a hostname or garbage;</li>
 *   <li>the address is <em>still inside {@code internal-proxies}</em>, which means the valve did not
 *       rewrite it. Either the peer was not a trusted proxy or the header was absent. Either way the
 *       value is the load balancer, shared by every visitor.</li>
 * </ol>
 * <p>In both cases the answer is {@link ClientOrigin#unknown()}. Keying a bucket on the proxy address
 * would put the whole deployment in one bucket, and a single anonymous attacker would then lock out
 * every user — the self-inflicted denial of service the whole design exists to avoid. Falling back to
 * account scope keeps the pre-existing behaviour and is logged by
 * {@code AuthAttemptThrottle}.</p>
 *
 * @since 1.20.4
 */
@Component
public class HttpClientOriginResolver implements ClientOriginResolver {

  private static final Logger log = LoggerFactory.logger(HttpClientOriginResolver.class);

  /**
   * The ranges the container is told to trust as proxies.
   *
   * <p>Read from the same property Boot reads, so the two cannot disagree. If the container trusts a
   * range that this class considers public, an address the valve left alone would be taken as a
   * client; if this class trusted more than the container, a real client would be discarded as a
   * proxy. Sharing the value makes both mistakes unrepresentable.</p>
   */
  private final String internalProxies;

  public HttpClientOriginResolver(
      @Value("${server.tomcat.remoteip.internal-proxies:"
          + "10.0.0.0/8, 192.168.0.0/16, 169.254.0.0/16, 100.64.0.0/10, "
          + "fc00::/7, 172.16.0.0/12, ::1/128, 127.0.0.0/8, fe80::/10}") String internalProxies) {
    this.internalProxies = requireNonNull(internalProxies, "internalProxies must not be null");
  }

  @Override
  public ClientOrigin resolve() {
    Optional<HttpServletRequest> request = currentRequest();
    if (request.isEmpty()) {
      // Nothing to attribute: no request bound to this thread. Happens for the throttle calls made
      // from a background job, and for the specs that exercise the throttle directly.
      return ClientOrigin.unknown();
    }
    return originOf(request.get().getRemoteAddr());
  }

  /**
   * Resolves an already-extracted address, so the rule can be asserted without an HTTP request.
   *
   * @param remoteAddress what {@link HttpServletRequest#getRemoteAddr()} returned; may be null
   * @return the grouped origin, or unknown when the value is not a trustworthy client address
   */
  ClientOrigin originOf(String remoteAddress) {
    Optional<IpNetworks.ParsedAddress> parsed = IpNetworks.parse(remoteAddress);
    if (parsed.isEmpty()) {
      log.debug("Request origin is not an IP literal and cannot be used as a throttle key");
      return ClientOrigin.unknown();
    }
    if (IpNetworks.isInAnyRange(parsed.get(), internalProxies)) {
      // The valve did not rewrite the address, so it is the proxy — the same value for everybody.
      log.debug("Request origin is an internal proxy address, so no trusted client origin exists");
      return ClientOrigin.unknown();
    }
    return ClientOrigin.of(IpNetworks.groupedNetwork(parsed.get()));
  }

  private static Optional<HttpServletRequest> currentRequest() {
    var attributes = RequestContextHolder.getRequestAttributes();
    if (attributes instanceof ServletRequestAttributes servletAttributes) {
      return Optional.of(servletAttributes.getRequest());
    }
    return Optional.empty();
  }
}
