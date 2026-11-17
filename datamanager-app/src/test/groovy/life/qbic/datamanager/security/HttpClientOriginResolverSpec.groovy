package life.qbic.datamanager.security

import life.qbic.identity.application.throttle.ClientOrigin
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.tomcat.autoconfigure.TomcatServerProperties
import spock.lang.Specification
import spock.lang.Unroll

/**
 * <b>A forged or unresolvable client address must never become a throttle bucket key (AUTH-NFR-03,
 * #1691).</b>
 *
 * <p>The acceptance criterion is explicit: <i>given a client-supplied {@code X-Forwarded-For}, when the
 * proxy overwrites it, then the forged value never becomes a bucket key.</i> This spec covers the
 * application's half of that bargain — that the resolver only reports an origin it is entitled to
 * trust, and reports nothing at all otherwise.</p>
 *
 * <p>The property that matters is the <b>negative</b> one. An origin bucket keyed on the load balancer
 * would not merely be imprecise: every user of the deployment would share it, so one anonymous
 * attacker filling it would lock out the entire population. That is strictly worse than having no
 * origin scope, which is why "unknown" is a value and not a fallback.</p>
 */
class HttpClientOriginResolverSpec extends Specification {

  /** Boot's default, which the production resolver reads from the same property. */
  static final String INTERNAL_PROXIES =
      '10.0.0.0/8, 192.168.0.0/16, 169.254.0.0/16, 100.64.0.0/10, ' +
          'fc00::/7, 172.16.0.0/12, ::1/128, 127.0.0.0/8, fe80::/10'

  HttpClientOriginResolver resolver

  def setup() {
    resolver = resolverWith(INTERNAL_PROXIES)
  }

  /**
   * A resolver whose container properties report the given trusted-proxy list.
   *
   * <p>Built from the real {@link TomcatServerProperties} rather than a stub, so the spec exercises the
   * same accessor path the valve is configured from — a stub here would let the two drift apart, which
   * is the defect this design exists to prevent.</p>
   */
  private static HttpClientOriginResolver resolverWith(String internalProxies) {
    def properties = new TomcatServerProperties()
    if (internalProxies != null) {
      properties.remoteip.internalProxies = internalProxies
    }
    return new HttpClientOriginResolver(providerOf(properties))
  }

  /** No servlet container properties at all — the state outside a servlet context. */
  private static HttpClientOriginResolver resolverWithoutContainerProperties() {
    return new HttpClientOriginResolver(providerOf(null))
  }

  private static ObjectProvider<TomcatServerProperties> providerOf(
      TomcatServerProperties value) {
    return new ObjectProvider<TomcatServerProperties>() {
      @Override
      TomcatServerProperties getObject(Object... args) { return value }

      @Override
      TomcatServerProperties getIfAvailable() { return value }

      @Override
      TomcatServerProperties getIfUnique() { return value }

      @Override
      TomcatServerProperties getObject() { return value }

      @Override
      Iterator<TomcatServerProperties> iterator() {
        return value == null ? Collections.emptyIterator() : [value].iterator()
      }
    }
  }

  // ---- the positive case: a real client address becomes a grouped origin ---------------------------

  @Unroll
  def "a public address resolves to its grouped network: #address -> #network"() {
    expect:
    resolver.originOf(address).network().orElse(null) == network

    where:
    address          | network
    '203.0.113.7'    | '203.0.113.0/24'
    '203.0.113.200'  | '203.0.113.0/24'
    '198.51.100.4'   | '198.51.100.0/24'
    '2001:db8:0:1::5'| '2001:0db8:0000:0001:0000:0000:0000:0000/64'
    '::ffff:198.51.100.4' | '198.51.100.0/24'
  }

  def "a resolved origin is marked known, so the throttle will use it"() {
    expect:
    resolver.originOf('203.0.113.7').isKnown()
  }

  // ---- the negative case: never key on the proxy, never key on garbage -----------------------------

  @Unroll
  def "an address still inside internal-proxies yields no origin: #address"() {
    // This is the case the issue calls the blocking check: if getRemoteAddr() still shows a proxy, STOP.
    // "Stop" is implemented as "no origin", which degrades to account scope rather than producing a
    // global bucket that one attacker can fill.
    expect:
    resolver.originOf(address) == ClientOrigin.unknown()

    where:
    address << [
        '10.1.2.3',
        '172.20.214.50',    // the HAProxy nodes recorded in the issue
        '172.20.214.49',
        '172.20.214.48',
        '127.0.0.1',        // an unproxied local request
        '192.168.1.10',
        '169.254.1.1',
        '100.64.0.1',
        '::1',
        'fe80::1',
        'fd00::1',
    ]
  }

  def "the proxy address is never returned as an origin even though it parses"() {
    // The failure mode is silent: 172.20.214.50 is a perfectly good IPv4 literal, so a resolver that
    // only checked "did this parse?" would happily key every user in the deployment on it.
    when:
    def origin = resolver.originOf('172.20.214.50')

    then:
    !origin.isKnown()
    origin.network().isEmpty()
  }

  @Unroll
  def "an address that is not a literal yields no origin: #address"() {
    // A hostname here would mean a DNS lookup on a request-supplied value. Rejecting it also stops a
    // value like `localhost` becoming a shared bucket.
    expect:
    resolver.originOf(address) == ClientOrigin.unknown()

    where:
    address << [null, '', '   ', 'localhost', 'unknown', 'client.example.org', 'not-an-ip',
                '1.2.3', '999.1.1.1']
  }

  def "without a bound request the resolver answers unknown rather than failing"() {
    // The throttle is also consulted from background work and from specs, where there is no request
    // thread. Throwing here would turn a throttle check into a 500.
    given: 'no request bound to this thread'
    org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes()

    expect:
    resolver.resolve() == ClientOrigin.unknown()

    cleanup:
    org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes()
  }

  def "an origin is resolved from the request-bound address"() {
    given: 'a request whose resolved remote address is a public client'
    def request = Stub(jakarta.servlet.http.HttpServletRequest) {
      getRemoteAddr() >> '203.0.113.7'
    }
    org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(
        new org.springframework.web.context.request.ServletRequestAttributes(request))

    expect:
    resolver.resolve().network().orElse(null) == '203.0.113.0/24'

    cleanup:
    org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes()
  }

  def "two clients behind one NAT share an origin, which is the point of grouping"() {
    // Same /24 groups them; the origin threshold must therefore be set above one institution's normal
    // failure rate. Recorded here so the consequence of grouping is asserted, not just assumed.
    expect:
    resolver.originOf('203.0.113.10') == resolver.originOf('203.0.113.200')
  }

  def "two different institutions do not share an origin"() {
    expect:
    resolver.originOf('203.0.113.10') != resolver.originOf('198.51.100.10')
  }

  def "an empty internal-proxies list trusts nothing as a proxy, so every literal is an origin"() {
    // Operators can clear the list. That is a legitimate choice for a directly reachable deployment
    // (the valve then never rewrites, and every request is its own client), so the resolver must not
    // second-guess it — but it does mean a deployment that cleared the list AND sits behind a proxy
    // will key on the proxy address. Documented in ops §1.2.
    given:
    def trustingNothing = resolverWith('')

    expect:
    trustingNothing.originOf('10.1.2.3').isKnown()
  }

  def "without any container properties the resolver reports no origin, never the peer address"() {
    // Fail closed. The tempting alternative — treat "no boundary known" as "nothing is a proxy" — is
    // the global-bucket failure: every request would key on whatever address the peer has, and behind
    // a proxy that is one bucket for the entire deployment.
    given:
    def noContainer = resolverWithoutContainerProperties()

    expect:
    noContainer.originOf('203.0.113.7') == ClientOrigin.unknown()
    noContainer.originOf('172.20.214.50') == ClientOrigin.unknown()
    noContainer.resolve() == ClientOrigin.unknown()
  }

  def "the resolver reads the same list the valve is configured from"() {
    // The two must never disagree, and only one direction of disagreement is safe. Pinning that the
    // value comes from TomcatServerProperties means a change to Boot's default reaches both.
    given:
    def custom = resolverWith('203.0.113.0/24')

    expect: 'a range the container trusts is not a client'
    custom.originOf('203.0.113.7') == ClientOrigin.unknown()

    and: 'even loopback, once the operator has narrowed the list, is judged by the list'
    custom.originOf('127.0.0.1').isKnown()
  }
}
