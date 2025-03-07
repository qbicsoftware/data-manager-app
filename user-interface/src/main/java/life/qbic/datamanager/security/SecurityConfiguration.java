package life.qbic.datamanager.security;

import com.vaadin.flow.spring.security.VaadinDefaultRequestCache;
import com.vaadin.flow.spring.security.VaadinWebSecurity;
import static java.util.Objects.requireNonNull;
import life.qbic.datamanager.views.login.LoginLayout;
import life.qbic.identity.application.security.QBiCPasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@EnableWebSecurity
@Configuration
//@Import({AclSecurityConfiguration.class}) // enable in case you need beans from the Acl config
public class SecurityConfiguration extends VaadinWebSecurity {

  final VaadinDefaultRequestCache defaultRequestCache;

  @Value("${routing.registration.oidc.orcid.endpoint}")
  String registrationOrcidEndpoint;

  @Value("${routing.registration.error.pending-email-verification}")
  String emailConfirmationEndpoint;

  public SecurityConfiguration(
      @Autowired VaadinDefaultRequestCache defaultRequestCache) {
    this.defaultRequestCache = requireNonNull(defaultRequestCache,
        "defaultRequestCache must not be null");
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new QBiCPasswordEncoder();
  }

  private AuthenticationSuccessHandler authenticationSuccessHandler() {
    requireNonNull(registrationOrcidEndpoint, "openIdRegistrationEndpoint must not be null");
    StoredRequestAwareOidcAuthenticationSuccessHandler storedRequestAwareOidcAuthenticationSuccessHandler = new StoredRequestAwareOidcAuthenticationSuccessHandler(
        registrationOrcidEndpoint, emailConfirmationEndpoint);
    storedRequestAwareOidcAuthenticationSuccessHandler.setRequestCache(defaultRequestCache);
    return storedRequestAwareOidcAuthenticationSuccessHandler;
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(v -> v.requestMatchers(
            new AntPathRequestMatcher("/oauth2/authorization/orcid"),
            new AntPathRequestMatcher("/oauth2/code/**"), new AntPathRequestMatcher("images/*.png"))
        .permitAll());
    http.oauth2Login(oAuth2Login -> {
      oAuth2Login.loginPage("/login").permitAll();
      oAuth2Login.defaultSuccessUrl("/");
      oAuth2Login.successHandler(
          authenticationSuccessHandler());
      oAuth2Login.failureUrl("/login?errorOauth2=true&error");
    });
    super.configure(http);
    setLoginView(http, LoginLayout.class);
  }
}
