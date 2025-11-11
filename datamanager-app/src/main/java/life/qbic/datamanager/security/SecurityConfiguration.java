package life.qbic.datamanager.security;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.spring.security.VaadinAwareSecurityContextHolderStrategyConfiguration;
import com.vaadin.flow.spring.security.VaadinDefaultRequestCache;
import com.vaadin.flow.spring.security.VaadinWebSecurity;
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
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@EnableWebSecurity
@Configuration
@Import(VaadinAwareSecurityContextHolderStrategyConfiguration.class)
public class SecurityConfiguration extends VaadinWebSecurity {

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

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    // Use Vaadin’s request cache (so redirects back to the original Flow route work)
    http.requestCache(c -> c.requestCache(defaultRequestCache));

    http.authorizeHttpRequests(v -> v
        .requestMatchers(VaadinWebSecurity.getDefaultWebSecurityIgnoreMatcher()).permitAll()
        .requestMatchers(
            new AntPathRequestMatcher("/oauth2/authorization/orcid"),
            new AntPathRequestMatcher("/oauth2/code/**"),
            new AntPathRequestMatcher("/link/**"),
            new AntPathRequestMatcher("/images/*.png"))
        .permitAll()
    );

    http.oauth2Login(oAuth2Login -> {
      oAuth2Login.loginPage("/login").permitAll();
      oAuth2Login.defaultSuccessUrl("/");
      oAuth2Login.successHandler(
          authenticationSuccessHandler());
      oAuth2Login.failureUrl("/login?errorOauth2=true&error");
    });

    // Let Vaadin register its filters/matchers
    super.configure(http);

    // Set the login view
    setLoginView(http, LoginLayout.class, contextPath + "/login?logout=true");
  }
}
