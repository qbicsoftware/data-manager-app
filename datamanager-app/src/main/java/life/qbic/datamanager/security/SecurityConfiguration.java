package life.qbic.datamanager.security;

import static com.vaadin.flow.spring.security.VaadinSecurityConfigurer.vaadin;
import static java.util.Objects.requireNonNull;

import com.vaadin.flow.spring.security.VaadinAwareSecurityContextHolderStrategyConfiguration;
import com.vaadin.flow.spring.security.VaadinDefaultRequestCache;
import life.qbic.datamanager.views.login.LoginLayout;
import life.qbic.identity.application.security.QBiCPasswordEncoder;
import life.qbic.identity.application.user.IdentityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@EnableWebSecurity
@Configuration
@Import(VaadinAwareSecurityContextHolderStrategyConfiguration.class)
public class SecurityConfiguration {

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

    http.oauth2Login(oauth2 -> oauth2.
        loginPage("/login").permitAll()
        .defaultSuccessUrl("/")
        .successHandler(authenticationSuccessHandler())
        .failureUrl("/login?errorOauth2=true&error"));

    http.with(vaadin(), vaadin -> vaadin
        .loginView(LoginLayout.class, contextPath + "/login?logout=true"));
    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new QBiCPasswordEncoder();
  }

  private AuthenticationSuccessHandler authenticationSuccessHandler() {
    requireNonNull(registrationOrcidEndpoint, "openIdRegistrationEndpoint must not be null");
    var storedRequestAwareOidcAuthenticationSuccessHandler = new StoredRequestAwareOidcAuthenticationSuccessHandler(
        registrationOrcidEndpoint, emailConfirmationEndpoint, identityService);
    storedRequestAwareOidcAuthenticationSuccessHandler.setRequestCache(defaultRequestCache);
    return storedRequestAwareOidcAuthenticationSuccessHandler;
  }
}
