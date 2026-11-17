package life.qbic.datamanager.configuration;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;

/**
 * <b>Refuse to start when origin-scope throttling is switched on but the client address cannot be
 * trusted.</b>
 *
 * <p>This is the code form of the issue's <em>blocking check</em>: "if {@code getRemoteAddr()} still
 * shows a proxy address after Phase C, stop". A runbook step that says "stop" is only as reliable as
 * the person reading it at 2 a.m. on the node that is already unhealthy, so the check is enforced where
 * it cannot be skipped.</p>
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

  /**
   * Used only to make the failure message actionable.
   *
   * <p>Nullable: a diagnostic must never be the reason a security gate cannot run.</p>
   */
  private final ConfigurableEnvironment environment;

  public OriginTrustConfiguration(
      @Value("${qbic.auth-throttle.login.origin.enabled:false}") boolean originEnabled,
      @Value("${server.forward-headers-strategy:none}") String forwardHeadersStrategy,
      ConfigurableEnvironment environment) {
    this.environment = environment;
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
        server.forward-headers-strategy=native, but it is currently '%s'%s.

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
        .formatted(strategy, configuredBy(strategy)));
  }

  /**
   * Maps a property name to the canonical kebab-case form Spring's relaxed binding compares on.
   *
   * @param name a property name in any supported spelling
   * @return the lower-cased name with {@code _} and {@code .} folded to {@code -}
   */
  private static String relax(String name) {
    return name.toLowerCase(Locale.ROOT).replace('_', '-').replace('.', '-');
  }

  /**
   * Where the ineffective strategy came from, so the message points at a file instead of a constant.
   *
   * <p>The first version of this message could not be acted on in the common case. When <em>nothing</em>
   * sets the property, it read <i>"it is currently 'none'"</i> — which looks exactly like a value
   * someone configured. An operator then greps their configuration for {@code none}, finds nothing, and
   * has no next step, because the value is Spring Boot's own default. Naming the origin of the value is
   * the difference between a message that explains a failure and one that merely reports it.</p>
   *
   * @param strategy the effective value
   * @return a parenthetical naming where the value came from, or an empty string if it cannot be told
   */
  private String configuredBy(String strategy) {
    if (!"none".equalsIgnoreCase(strategy)) {
      return " (set explicitly)";
    }
    try {
      if (environment == null) {
        return " (the default; the property is not set)";
      }
      // Report the key whose VALUE is the one that won, walking sources in precedence order.
      //
      // Not "the first key with a matching name": that picked up the kebab-case name from a low
      // precedence source while the actual value came from an environment variable, and the message
      // then named a key the operator had never written. Comparing values is what makes the answer
      // true even when several sources carry the property.
      String supplyingKey = StreamSupport
          .stream(environment.getPropertySources().spliterator(), false)
          .flatMap(source -> source instanceof EnumerablePropertySource<?> enumerable
              ? Arrays.stream(enumerable.getPropertyNames())
              : Stream.<String>empty())
          // Relaxed binding means the key may be `server.forward-headers-strategy` in a properties
          // file or `SERVER_FORWARD_HEADERS_STRATEGY` in the environment. Normalising the separators
          // is what lets one predicate match both — comparing on the raw name silently missed every
          // environment-variable spelling, which is the form that matters in production.
          .filter(name -> relax(name).endsWith("forward-headers-strategy"))
          .sorted()
          .filter(name -> {
            Object value = environment.getProperty(name);
            return value != null && strategy.equalsIgnoreCase(value.toString().strip());
          })
          .findFirst()
          .orElse(null);
      if (supplyingKey != null) {
        return " (from '%s')".formatted(supplyingKey);
      }
      return " (the default; the property is not set anywhere)";
    } catch (RuntimeException e) {
      // Diagnostics must never mask the security failure they accompany.
      log.debug("Could not determine where the forward-headers strategy came from", e);
      return "";
    }
  }
}
