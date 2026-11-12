package life.qbic.datamanager.security;

import static com.vaadin.flow.spring.security.VaadinSecurityConfigurer.vaadin;
import static java.util.Objects.requireNonNull;

import com.vaadin.flow.spring.security.VaadinDefaultRequestCache;
import life.qbic.datamanager.views.login.LoginLayout;
import life.qbic.identity.application.security.QBiCPasswordEncoder;
import life.qbic.identity.application.throttle.AuthAttemptThrottle;
import life.qbic.identity.application.user.IdentityService;
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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestClient;

@EnableWebSecurity
@Configuration
public class SecurityConfiguration {

  private static final Logger log = LoggerFactory.logger(SecurityConfiguration.class);

  final VaadinDefaultRequestCache defaultRequestCache;
  private final IdentityService identityService;
  private final AuthAttemptThrottle authAttemptThrottle;

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

  @Value("${routing.registration.oidc.orcid.endpoint}")
  String registrationOrcidEndpoint;

  @Value("${routing.registration.error.pending-email-verification}")
  String emailConfirmationEndpoint;

  @Value("${server.servlet.context-path}")
  String contextPath;

  public SecurityConfiguration(
      @Autowired VaadinDefaultRequestCache defaultRequestCache,
      @Autowired IdentityService identityService,
      @Autowired AuthAttemptThrottle authAttemptThrottle) {
    this.defaultRequestCache = requireNonNull(defaultRequestCache,
        "defaultRequestCache must not be null");
    this.identityService = requireNonNull(identityService);
    this.authAttemptThrottle = requireNonNull(authAttemptThrottle,
        "authAttemptThrottle must not be null");
  }

  @Bean
  public SecurityFilterChain vaadinSecurityFilterChain(HttpSecurity http) {
    http.requestCache(c -> c.requestCache(defaultRequestCache));

    http.authorizeHttpRequests(v -> v
        .requestMatchers(
            "/oauth2/authorization/orcid",
            "/oauth2/code/**",
            "/link/**",
            "/images/*.png")
        .permitAll()
        // Profile pictures are served by a Spring MVC controller outside the Vaadin
        // stream mechanism, so the path must be explicitly authorized. Any authenticated
        // user may read any avatar (the controller is the authority for that policy).
        .requestMatchers("/profile-pictures/**")
        .authenticated()
    );

    http.oauth2Login(oauth2 ->
        oauth2.loginPage("/login")
            .permitAll()
            .defaultSuccessUrl("/")
            .successHandler(authenticationSuccessHandler())
            .failureHandler(oauth2AuthenticationFailureHandler())
            .tokenEndpoint(token -> token
                .accessTokenResponseClient(accessTokenResponseClient())));

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

  /**
   * Logs the actual OAuth2 authentication failure before redirecting to the login page.
   * <p>
   * Without this, Spring Security's default failure handler silently redirects and the
   * exception details are lost — making it impossible to diagnose OAuth2 issues.
   */
  private AuthenticationFailureHandler oauth2AuthenticationFailureHandler() {
    var handler = new SimpleUrlAuthenticationFailureHandler(
        "/login?errorOauth2=true&error");
    return (request, response, exception) -> {
      log.error("OAuth2 authentication failed: %s".formatted(exception.getMessage()), exception);
      handler.onAuthenticationFailure(request, response, exception);
    };
  }

  private AuthenticationSuccessHandler authenticationSuccessHandler() {
    requireNonNull(registrationOrcidEndpoint, "openIdRegistrationEndpoint must not be null");
    var storedRequestAwareOidcAuthenticationSuccessHandler = new StoredRequestAwareOidcAuthenticationSuccessHandler(
        registrationOrcidEndpoint, emailConfirmationEndpoint, identityService);
    storedRequestAwareOidcAuthenticationSuccessHandler.setRequestCache(defaultRequestCache);
    return storedRequestAwareOidcAuthenticationSuccessHandler;
  }
}
