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
   * The issuer of the ORCID registration, from configuration.
   *
   * <p>Not read from the assertion: a provider that could name its own issuer could rename itself into
   * an accepted value. This is the same value the client registration was built from, so the stored
   * issuer always matches the registry the iD actually came from.</p>
   */
  @Value("${qbic.oidc.orcid.issuer:https://sandbox.orcid.org}")
  String orcidIssuer;

  public SecurityConfiguration(
      @Autowired VaadinDefaultRequestCache defaultRequestCache,
      @Autowired AuthAttemptThrottle authAttemptThrottle,
      @Autowired life.qbic.identity.api.OidcLinkService oidcLinkService,
      @Autowired org.springframework.security.oauth2.client.OAuth2AuthorizedClientService authorizedClientService) {
    this.defaultRequestCache = requireNonNull(defaultRequestCache,
        "defaultRequestCache must not be null");
    this.authAttemptThrottle = requireNonNull(authAttemptThrottle,
        "authAttemptThrottle must not be null");
    this.oidcLinkService = requireNonNull(oidcLinkService, "oidcLinkService must not be null");
    this.authorizedClientService = requireNonNull(authorizedClientService,
        "authorizedClientService must not be null");
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

    http.with(vaadin(), vaadin -> vaadin
        .loginView(LoginLayout.class, contextPath + "/login?logout=true"));
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
