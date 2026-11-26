package life.qbic.identity.application.throttle;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

/**
 * <b>The request origin a throttle bucket may be keyed on — or the honest statement that there is
 * none.</b>
 *
 * <h3>Why "unknown" is a first-class value rather than a fallback address</h3>
 * <p>The obvious fallback is {@code request.getRemoteAddr()}. Behind a proxy that does not forward
 * the client address, that value is <em>the proxy</em>, identical for every visitor in the
 * deployment. Keying an origin bucket on it does not weaken the control, it inverts it: one anonymous
 * attacker fills the single bucket and the entire user population is locked out. That is a
 * self-inflicted denial of service, strictly worse than having no origin scope at all.</p>
 *
 * <p>So the absence of a trusted origin has to be representable, and every consumer has to handle it
 * explicitly. {@link #unknown()} is that value; {@link #network()} is empty exactly when the origin
 * could not be established.</p>
 *
 * <p>An origin is only ever <em>known</em> when the address was produced by a component that is
 * entitled to rewrite it — Tomcat's {@code RemoteIpValve} under
 * {@code server.forward-headers-strategy=native}, which trusts the header only from a peer in
 * {@code server.tomcat.remoteip.internal-proxies} and then walks the header right-to-left, discarding
 * everything client-supplied to its left. The application never parses the header itself; see
 * {@code docs/operations/installation-and-administration.md} §1.5 for why the leftmost-entry
 * strategies are unusable here.</p>
 *
 * @param network the grouped network the request came from, or empty when no trusted origin exists
 * @since 1.20.4
 */
public record ClientOrigin(Optional<String> network) {

  /**
   * A grouped network, e.g. {@code 203.0.113.0/24} or {@code 2001:db8:0:0::/64}.
   *
   * <p>Grouped, not a single address: see {@link IpNetworks} for why a single address is not an
   * identity an attacker cannot rotate.</p>
   */
  public ClientOrigin {
    requireNonNull(network, "network must not be null");
    network.ifPresent(value -> {
      if (value.isBlank()) {
        throw new IllegalArgumentException("a known origin must not be blank");
      }
    });
  }

  /**
   * No trusted origin could be established, so no origin bucket may be written or consulted.
   *
   * @return the unknown origin
   */
  public static ClientOrigin unknown() {
    return new ClientOrigin(Optional.empty());
  }

  /**
   * @param network a grouped network, as produced by {@link IpNetworks#groupedNetwork}
   * @return a known origin
   */
  public static ClientOrigin of(String network) {
    if (network == null || network.isBlank()) {
      return unknown();
    }
    return new ClientOrigin(Optional.of(network.strip()));
  }

  /**
   * Whether an origin bucket can be keyed on this value.
   *
   * <p>Callers must not treat "unknown" as "allow": the combined decision in
   * {@link AuthAttemptThrottle} falls back to account scope, which is the pre-existing behaviour.
   * Treating it as "allow" would silently disable throttling on any deployment that forgot the proxy
   * configuration.</p>
   *
   * @return whether a trusted origin is present
   */
  public boolean isKnown() {
    return network.isPresent();
  }
}
