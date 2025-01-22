package life.qbic.datamanager.security;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.spring.security.VaadinDefaultRequestCache;
import com.vaadin.flow.spring.security.VaadinWebSecurity;
import life.qbic.datamanager.CustomOAuth2AccessTokenResponseClient;
import life.qbic.datamanager.views.login.LoginLayout;
import life.qbic.identity.application.security.QBiCPasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@EnableWebSecurity
@Configuration
//@Import({AclSecurityConfiguration.class}) // enable in case you need beans from the Acl config
public class SecurityConfiguration extends VaadinWebSecurity {

  final VaadinDefaultRequestCache defaultRequestCache;
  private final OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient;

  @Value("${routing.registration.oidc.orcid.endpoint}")
  String registrationOrcidEndpoint;

  @Value("${routing.registration.error.pending-email-verification}")
  String emailConfirmationEndpoint;

  public SecurityConfiguration(
      @Autowired VaadinDefaultRequestCache defaultRequestCache,
      OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient) {
    this.defaultRequestCache = requireNonNull(defaultRequestCache,
        "defaultRequestCache must not be null");
    this.accessTokenResponseClient = accessTokenResponseClient;
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
    /*http.authorizeHttpRequests(v -> v.requestMatchers(
            new AntPathRequestMatcher("/oauth2/authorization/orcid"),
            new AntPathRequestMatcher("/oauth2/authorization/zenodo"),
            new AntPathRequestMatcher("/oauth2/callback/zenodo2"),
            new AntPathRequestMatcher("/oauth2/code/**"), new AntPathRequestMatcher("images/*.png"))
        .permitAll());

    http.oauth2Login(oAuth2Login -> {
      oAuth2Login.loginPage("/login").permitAll();
      oAuth2Login.defaultSuccessUrl("/");
      oAuth2Login.failureHandler((request, response, e) -> {
        System.out.println(e.getMessage());
      });
      oAuth2Login.successHandler(
          authenticationSuccessHandler());
      oAuth2Login.failureUrl("/login?errorOauth2=true&error");
    });
    super.configure(http);
    setLoginView(http, LoginLayout.class);*/
    http.authorizeHttpRequests(v ->
            v.requestMatchers("/", "/login", "/oauth2/authorization/zenodo2").permitAll() // Public paths
            .requestMatchers("/oauth2/code/**").permitAll()
        )
        .oauth2Login(oauth2 -> oauth2
            .defaultSuccessUrl("/login", true)
                .tokenEndpoint(v -> v.accessTokenResponseClient(accessTokenResponseClient))
        );

    super.configure(http);
  }

  @Bean
  public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> customAccessTokenResponseClient() {
    return new CustomOAuth2AccessTokenResponseClient();
  }
}
