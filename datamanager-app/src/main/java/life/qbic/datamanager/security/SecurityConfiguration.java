package life.qbic.datamanager.security;

import static com.vaadin.flow.spring.security.VaadinSecurityConfigurer.vaadin;
import static java.util.Objects.requireNonNull;

import com.vaadin.flow.spring.security.VaadinDefaultRequestCache;
import life.qbic.datamanager.views.login.LoginLayout;
import life.qbic.identity.application.security.QBiCPasswordEncoder;
import life.qbic.identity.application.throttle.AuthAttemptThrottle;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import life.qbic.datamanager.security.mfa.MfaChallengeEnforcementFilter;
import life.qbic.datamanager.security.mfa.MfaLoginSuccessHandler;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationCodeGrantFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestClient;

@EnableWebSecurity
@Configuration
public class SecurityConfiguration {

  private static final Logger log = LoggerFactory.logger(SecurityConfiguration.class);

  final VaadinDefaultRequestCache defaultRequestCache;
  private final AuthAttemptThrottle authAttemptThrottle;
  private final life.qbic.identity.api.OidcLinkService oidcLinkService;
  private final org.springframework.security.oauth2.client.OAuth2AuthorizedClientService authorizedClientService;
  private final life.qbic.datamanager.security.mfa.MfaLoginSuccessHandler mfaLoginSuccessHandler;
  private final life.qbic.identity.application.mfa.MfaLoginGuard mfaLoginGuard;

  /**
   * Held directly, not injected.
   *
   * <p>{@link #passwordEncoder()} is a {@code @Bean} on <em>this</em> class, so injecting a
   * {@code PasswordEncoder} back into the constructor makes the configuration bean depend on a bean
   * it creates. Spring Boot 4 prohibits circular references by default, and the application fails to
   * start with "the dependencies of some of the beans in the application context form a cycle:
   * securityConfiguration".</p>
   *
   * <p>Constructing it here is honest rather than a workaround: it is a stateless value with no
   * collaborators, and {@link QBiCPasswordEncoder} is exactly what the bean method returns.</p>
   */
  private final PasswordEncoder passwordEncoder = new QBiCPasswordEncoder();

  @Value("${server.servlet.context-path}")
  String contextPath;

  /**
   * The issuer the ORCID client registration points at.
   *
   * <p><b>No default, deliberately.</b> This value is written to {@code users.oidcIssuer} on every link
   * and is half of the uniqueness key: an iD on {@code https://orcid.org} and the same iD on
   * {@code https://sandbox.orcid.org} are two different links. A silent default therefore does not
   * "fall back" — it stamps a registry the deployment may not be talking to, and the same researcher
   * then ends up holding two links that look exchangeable and are not.</p>
   *
   * <p>There is no {@code :...} fallback here on purpose: a missing value must fail at startup, not
   * pick sandbox. {@link #verifyIssuerMatchesProvider()} checks that it agrees with the configured
   * OAuth2 provider, because a mismatch is silently wrong rather than loudly broken.</p>
   */
  @Value("${qbic.oidc.orcid.issuer}")
  String orcidIssuer;

  /**
   * The provider's authorization endpoint, from the same key the OAuth2 client registration uses.
   *
   * <p>Used only to derive which registry the client actually talks to, so {@link #orcidIssuer} can be
   * cross-checked against it. There is no {@code issuer} property on the provider block — the endpoint
   * URIs are what a deployment actually changes.</p>
   */
  @Value("${spring.security.oauth2.client.provider.orcid.authorization-uri:}")
  String providerAuthorizationUri;

  public SecurityConfiguration(
      @Autowired VaadinDefaultRequestCache defaultRequestCache,
      @Autowired AuthAttemptThrottle authAttemptThrottle,
      @Autowired life.qbic.identity.api.OidcLinkService oidcLinkService,
      @Autowired org.springframework.security.oauth2.client.OAuth2AuthorizedClientService authorizedClientService,
      @Autowired life.qbic.datamanager.security.mfa.MfaLoginSuccessHandler mfaLoginSuccessHandler,
      @Autowired life.qbic.identity.application.mfa.MfaLoginGuard mfaLoginGuard) {
    this.defaultRequestCache = requireNonNull(defaultRequestCache,
        "defaultRequestCache must not be null");
    this.authAttemptThrottle = requireNonNull(authAttemptThrottle,
        "authAttemptThrottle must not be null");
    this.oidcLinkService = requireNonNull(oidcLinkService, "oidcLinkService must not be null");
    this.authorizedClientService = requireNonNull(authorizedClientService,
        "authorizedClientService must not be null");
    this.mfaLoginSuccessHandler = requireNonNull(mfaLoginSuccessHandler,
        "mfaLoginSuccessHandler must not be null");
    this.mfaLoginGuard = requireNonNull(mfaLoginGuard, "mfaLoginGuard must not be null");
  }

  /**
   * Fails startup when the stored-issuer setting and the OAuth2 provider disagree.
   *
   * <p>They are two settings for one fact. Nothing at runtime compares them, so a deployment that
   * changes one and not the other silently records the wrong registry on every link — which is how a
   * single installation came to hold {@code users.oidcIssuer} values from both registries.</p>
   */
  @jakarta.annotation.PostConstruct
  void verifyIssuerMatchesProvider() {
    String configured = orcidIssuer == null ? "" : orcidIssuer.strip();
    if (configured.isEmpty()) {
      throw new IllegalStateException(
          "qbic.oidc.orcid.issuer must be set to the ORCID registry this deployment talks to "
              + "(https://orcid.org or https://sandbox.orcid.org). It has no default because it is "
              + "stored on every linked account and is part of the link's uniqueness.");
    }
    var providerRegistry = registryOf(providerAuthorizationUri);
    if (providerRegistry.isEmpty()) {
      throw new IllegalStateException(
          "spring.security.oauth2.client.provider.orcid.authorization-uri must be set, so the ORCID "
              + "registry in use can be determined.");
    }
    if (!providerRegistry.get().equals(configured)) {
      throw new IllegalStateException(
          ("qbic.oidc.orcid.issuer (%s) does not match the registry the OAuth2 client talks to (%s, from "
              + "the authorization URI). Both must name the same registry, or every link records the "
              + "wrong one.").formatted(configured, providerRegistry.get()));
    }
    log.info("ORCID issuer for linked accounts: %s".formatted(configured));
  }

  /**
   * The registry origin behind an authorization endpoint, as {@code scheme://host}.
   *
   * @param authorizationUri the configured authorization endpoint
   * @return the origin, or empty when the value is absent or unparseable
   */
  static java.util.Optional<String> registryOf(String authorizationUri) {
    if (authorizationUri == null || authorizationUri.isBlank()) {
      return java.util.Optional.empty();
    }
    try {
      var uri = java.net.URI.create(authorizationUri.strip());
      if (uri.getScheme() == null || uri.getHost() == null) {
        return java.util.Optional.empty();
      }
      var port = uri.getPort() == -1 ? "" : ":" + uri.getPort();
      return java.util.Optional.of(uri.getScheme() + "://" + uri.getHost() + port);
    } catch (IllegalArgumentException notAUri) {
      return java.util.Optional.empty();
    }
  }

  @Bean
  public SecurityFilterChain vaadinSecurityFilterChain(HttpSecurity http) {
    http.requestCache(c -> c.requestCache(defaultRequestCache));

    http.authorizeHttpRequests(v -> v
        .requestMatchers(
            // /link/orcid is permitAll on purpose, because it must serve anonymous callers: the
            // controller checks the authentication itself and redirects to /login when there is
            // none. Leaving it to the authorization filter would show a bare 403 instead of the
            // login form, which reads as a broken link rather than as "sign in first".
            //
            // The OAuth2 callback is deliberately absent from this list. It is reached by the
            // provider redirect and is matched by no other rule, so it falls through to the last
            // matcher's default. Do not "fix" this by adding /login/oauth2/** here without also
            // confirming that OAuth2AuthorizationCodeGrantFilter still consumes the response first.
            "/link/**",
            "/images/*.png")
        .permitAll()
        // Profile pictures are served by a Spring MVC controller outside the Vaadin
        // stream mechanism, so the path must be explicitly authorized. Any authenticated
        // user may read any avatar (the controller is the authority for that policy).
        .requestMatchers("/profile-pictures/**")
        .authenticated()
    );

    // ORCID is a client grant, not a login (AUTH-R-07).
    //
    // This used to be oauth2Login(...), which mints an authenticated principal from a provider
    // assertion. That made ORCID a second way of establishing a session — the one thing a
    // second-factor requirement cannot tolerate, because the factor is applied to the password path
    // only. oauth2Client() performs the same authorization-code exchange but stores the result as an
    // OAuth2AuthorizedClient for the principal that is already logged in; it never produces an
    // Authentication, so "linking cannot create a session" is a property of the filter chain rather
    // than of a success handler's discipline.
    http.oauth2Client(client -> client
        .authorizationCodeGrant(grant -> grant
            .accessTokenResponseClient(accessTokenResponseClient())));

    // The link is completed here, AFTER OAuth2AuthorizationCodeGrantFilter has exchanged the code and
    // stored the OAuth2AuthorizedClient for the current principal. Registering it "after" in the chain
    // is what makes the token available to read, and reading it is all this filter does: it takes the
    // ORCID iD and immediately discards the token.
    //
    // It never creates or restores an Authentication. That is the difference between this arrangement
    // and the oauth2Login() one it replaces, and it is the property the whole story exists for.
    http.addFilterAfter(oidcLinkCompletionFilter(), OAuth2AuthorizationCodeGrantFilter.class);

    // Server-side throttling for the password login (AUTH-NFR-03, #1682).
    //
    // Registered BEFORE UsernamePasswordAuthenticationFilter, because a throttled attempt must be
    // rejected before any password work happens — a check after verification is too late to save the
    // work and cannot make the response cheap. Counting failed attempts is done separately, by
    // AuthAttemptEventListener, on Spring's authentication events.
    //
    // The failure handler is deliberately the same one the form login would use, so a throttled
    // attempt produces the identical "Incorrect username or password" screen. A distinct "too many
    // attempts" message would tell an attacker their target is real (decision B4).
    http.addFilterBefore(loginThrottleFilter(), UsernamePasswordAuthenticationFilter.class);

    // Belt and braces for the second factor (MILESTONE-MFA M4-1): if an authenticated session ever
    // exists while a login is still pending a factor, it is withdrawn and the challenge is required.
    // Registered BEFORE the password filter on purpose — the freshly minted authentication on the
    // login POST must be left to the success handler, which knows whether to withhold it.
    http.addFilterBefore(new MfaChallengeEnforcementFilter(mfaLoginGuard),
        UsernamePasswordAuthenticationFilter.class);

    http.with(vaadin(), vaadin -> vaadin
        .loginView(LoginLayout.class, contextPath + "/login?logout=true"));

    // The second factor: after the password is accepted, this handler decides whether a session may
    // exist at all. Configured AFTER the Vaadin configurer so it is the last word on the success
    // handler; a password-only session must never be established (AUTH-R-01).
    http.formLogin(form -> form.successHandler(mfaLoginSuccessHandler));
    return http.build();
  }

  /**
   * Provides an OAuth2 access token response client that explicitly uses the JDK HTTP client.
   * <p>
   * This is necessary because the openbis-api dependency bundles Jetty 9 classes in a fat JAR,
   * which causes Spring Framework's auto-detection to select JettyClientHttpRequestFactory.
   * Spring Framework 7 expects Jetty 12 API, so the bundled Jetty 9 leads to a
   * NoSuchMethodError at runtime. By explicitly configuring the JDK HTTP client, we bypass
   * the faulty auto-detection.
   * <p>
   * The RestClient must also be configured with the same message converters and error handler
   * that Spring Security's default RestClient uses, otherwise the OAuth2 token response
   * deserialization will be incomplete (e.g. additionalParameters will be null).
   */
  @Bean
  public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient() {
    var restClient = RestClient.builder()
        .requestFactory(new JdkClientHttpRequestFactory())
        .configureMessageConverters(converters -> {
          converters.addCustomConverter(new FormHttpMessageConverter());
          converters.addCustomConverter(new OAuth2AccessTokenResponseHttpMessageConverter());
        })
        .defaultStatusHandler(new OAuth2ErrorResponseErrorHandler())
        .build();
    var client = new RestClientAuthorizationCodeTokenResponseClient();
    client.setRestClient(restClient);
    return client;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    // A separate instance from the field above, deliberately. This bean is what Spring Boot injects
    // into the DaoAuthenticationProvider it derives from the UserDetailsService; the field exists
    // only so this class never has to inject a bean it defines itself.
    return new QBiCPasswordEncoder();
  }

  /**
   * Rejects a throttled login attempt before any password work is done.
   *
   * <p>The constant-work hook performs one derivation on the rejection path, so a throttled attempt
   * is not observably faster than a wrong password — otherwise the throttle itself signals that the
   * address exists.</p>
   */
  private OidcLinkCompletionFilter oidcLinkCompletionFilter() {
    return new OidcLinkCompletionFilter(oidcLinkService, authorizedClientService, orcidIssuer);
  }

  private LoginThrottleFilter loginThrottleFilter() {
    SimpleUrlAuthenticationFailureHandler failureHandler =
        new SimpleUrlAuthenticationFailureHandler("/login?error");
    return new LoginThrottleFilter(authAttemptThrottle, failureHandler,
        // One real derivation against the same well-formed stand-in the login path uses for an
        // account with no password. It must be well-formed PBKDF2 (iterations:salt:hash) — a bcrypt
        // string would throw here instead of deriving, making the rejection measurably faster.
        address -> passwordEncoder.matches(UserDetailsServiceImpl.NO_PASSWORD,
            UserDetailsServiceImpl.NO_PASSWORD));
  }

}
