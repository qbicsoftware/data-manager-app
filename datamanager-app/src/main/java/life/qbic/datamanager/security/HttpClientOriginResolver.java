package life.qbic.datamanager.security;

import static java.util.Objects.requireNonNull;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import life.qbic.identity.application.throttle.ClientOrigin;
import life.qbic.identity.application.throttle.ClientOriginResolver;
import life.qbic.identity.application.throttle.IpNetworks;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.tomcat.autoconfigure.TomcatServerProperties;
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
   * Supplies the ranges the container is told to trust as proxies.
   *
   * <p><b>Deliberately the same object the valve is configured from</b>, not a copy of the property and
   * certainly not a list of literals here. The two lists must agree exactly, and only one direction of
   * disagreement is dangerous:</p>
   * <ul>
   *   <li>this class trusting <em>more</em> than the container only wastes a bucket on something that
   *       really was a client;</li>
   *   <li>this class trusting <em>less</em> means an address the valve refused to rewrite is treated as
   *       a client — so the bucket is keyed on the <b>proxy</b>, one row covers the whole deployment,
   *       and a single attacker locks out every user.</li>
   * </ul>
   * <p>A hardcoded copy of Boot's default would drift towards that second case as soon as Boot changed
   * its default, and nothing would report it. Reading {@code TomcatServerProperties} makes the
   * disagreement unrepresentable.</p>
   *
   * <p>An {@link ObjectProvider} because this class is also used outside a servlet context (the specs),
   * where Boot's {@code TomcatServletWebServerAutoConfiguration} never ran. Absent, this resolver
   * reports no origin at all — failing closed rather than guessing a trust boundary.</p>
   */
  private final ObjectProvider<TomcatServerProperties> tomcatProperties;

  public HttpClientOriginResolver(ObjectProvider<TomcatServerProperties> tomcatProperties) {
    this.tomcatProperties = requireNonNull(tomcatProperties,
        "tomcatProperties must not be null");
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
    String internalProxies = internalProxies();
    if (internalProxies == null) {
      // No trust boundary is known, so nothing can be classified. Returning the address here would be
      // the global-bucket failure: every request would key on whatever the peer happens to be.
      log.debug("No trusted-proxy ranges available, so no trusted client origin exists");
      return ClientOrigin.unknown();
    }
    if (IpNetworks.isInAnyRange(parsed.get(), internalProxies)) {
      // The valve did not rewrite the address, so it is the proxy — the same value for everybody.
      log.debug("Request origin is an internal proxy address, so no trusted client origin exists");
      return ClientOrigin.unknown();
    }
    return ClientOrigin.of(IpNetworks.groupedNetwork(parsed.get()));
  }

  /**
   * The trusted-proxy ranges, as the container itself is configured.
   *
   * @return the CIDR list, or {@code null} when no servlet container properties exist — see
   *     {@link #internalProxies} for why "no boundary" must not fall back to "no proxies"
   */
  private String internalProxies() {
    TomcatServerProperties properties = tomcatProperties.getIfAvailable();
    return properties == null ? null : properties.getRemoteip().getInternalProxies();
  }

  private static Optional<HttpServletRequest> currentRequest() {
    var attributes = RequestContextHolder.getRequestAttributes();
    if (attributes instanceof ServletRequestAttributes servletAttributes) {
      return Optional.of(servletAttributes.getRequest());
    }
    return Optional.empty();
  }
}
