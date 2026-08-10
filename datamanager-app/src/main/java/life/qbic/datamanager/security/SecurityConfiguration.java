package life.qbic.datamanager.security;

import static com.vaadin.flow.spring.security.VaadinSecurityConfigurer.vaadin;
import static java.util.Objects.requireNonNull;

import com.vaadin.flow.spring.security.VaadinDefaultRequestCache;
import life.qbic.datamanager.views.login.LoginLayout;
import life.qbic.identity.application.security.QBiCPasswordEncoder;
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
import org.springframework.web.client.RestClient;

@EnableWebSecurity
@Configuration
public class SecurityConfiguration {

  private static final Logger log = LoggerFactory.logger(SecurityConfiguration.class);

  final VaadinDefaultRequestCache defaultRequestCache;
  private final IdentityService identityService;

  @Value("${routing.registration.oidc.orcid.endpoint}")
  String registrationOrcidEndpoint;

  @Value("${routing.registration.error.pending-email-verification}")
  String emailConfirmationEndpoint;

  @Value("${server.servlet.context-path}")
  String contextPath;

  public SecurityConfiguration(
      @Autowired VaadinDefaultRequestCache defaultRequestCache,
      @Autowired IdentityService identityService) {
    this.defaultRequestCache = requireNonNull(defaultRequestCache,
        "defaultRequestCache must not be null");
    this.identityService = requireNonNull(identityService);
  }

  @Bean
  public SecurityFilterChain vaadinSecurityFilterChain(HttpSecurity http) throws Exception {
    http.requestCache(c -> c.requestCache(defaultRequestCache));

    http.authorizeHttpRequests(v -> v
        .requestMatchers(
            "/oauth2/authorization/orcid",
            "/oauth2/code/**",
            "/link/**",
            "/images/*.png")
        .permitAll()
    );

    http.oauth2Login(oauth2 ->
        oauth2.loginPage("/login")
            .permitAll()
            .defaultSuccessUrl("/")
            .successHandler(authenticationSuccessHandler())
            .failureHandler(oauth2AuthenticationFailureHandler())
            .tokenEndpoint(token -> token
                .accessTokenResponseClient(accessTokenResponseClient())));

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
    return new QBiCPasswordEncoder();
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
