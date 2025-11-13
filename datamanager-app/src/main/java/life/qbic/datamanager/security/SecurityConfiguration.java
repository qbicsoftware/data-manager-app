package life.qbic.datamanager.security;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.spring.security.VaadinAwareSecurityContextHolderStrategyConfiguration;
import com.vaadin.flow.spring.security.VaadinDefaultRequestCache;
import com.vaadin.flow.spring.security.VaadinWebSecurity;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import life.qbic.datamanager.views.login.LoginLayout;
import life.qbic.identity.application.security.QBiCPasswordEncoder;
import life.qbic.identity.application.user.IdentityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.reactive.function.client.WebClient;

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

  private final OAuth2AuthorizedClientService authorizedClientService;

  public SecurityConfiguration(
      @Autowired VaadinDefaultRequestCache defaultRequestCache,
      @Autowired IdentityService identityService,
      @Autowired OAuth2AuthorizedClientService authorizedClientService) {
    this.defaultRequestCache = requireNonNull(defaultRequestCache,
        "defaultRequestCache must not be null");
    this.identityService = requireNonNull(identityService);
    this.authorizedClientService = requireNonNull(authorizedClientService);
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

    http.oauth2Client(Customizer.withDefaults());

    http.authorizeHttpRequests(v -> v
        .requestMatchers(VaadinWebSecurity.getDefaultWebSecurityIgnoreMatcher()).permitAll()
        .requestMatchers(
            new AntPathRequestMatcher("/oauth2/authorization/invenio"),
            new AntPathRequestMatcher("/oauth2/callback/invenio/**")
        ).permitAll()  // u
        .requestMatchers(
            new AntPathRequestMatcher("/oauth2/authorization/orcid"),
            new AntPathRequestMatcher("/login/oauth2/code/**"),
            new AntPathRequestMatcher("/images/*.png"))
        .permitAll()
    );

    http.oauth2Login(oAuth2Login -> {
      oAuth2Login.loginPage("/login").permitAll();
     // oAuth2Login.authorizationEndpoint(ae -> ae.baseUri("/oauth2/authorization"));
     // oAuth2Login.redirectionEndpoint(re -> re.baseUri("/oauth2/callback/*"));
      oAuth2Login.defaultSuccessUrl("/");
      // bypass principal replacement for the 'invenio' client
      oAuth2Login.successHandler((req, res, auth) -> {
        if (auth instanceof org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken token
            && "invenio".equals(token.getAuthorizedClientRegistrationId())) {

          var oauthClient = authorizedClientService.loadAuthorizedClient(
              token.getAuthorizedClientRegistrationId(),
              token.getName());

          // Restore the original application user (we saved it just before starting the flow)
          var orig = (org.springframework.security.core.Authentication)
              req.getSession().getAttribute("origAuth");
          if (orig != null) {
            org.springframework.security.core.context.SecurityContextHolder.getContext()
                .setAuthentication(orig);

            if (oauthClient != null) {
              var bridgedClient = new OAuth2AuthorizedClient(
                  oauthClient.getClientRegistration(),
                  orig.getName(),
                  oauthClient.getAccessToken(),
                  oauthClient.getRefreshToken()
              );
              authorizedClientService.saveAuthorizedClient(bridgedClient, orig);
            }
          }
          // Done: tokens are stored in OAuth2AuthorizedClientService. Redirect wherever you like.
          res.sendRedirect(contextPath + "/test-integration"); // or your “linked!” view
          return;
        }
        // For normal logins (e.g., ORCID), run your existing handler
        authenticationSuccessHandler().onAuthenticationSuccess(req, res, auth);
      });
      oAuth2Login.failureUrl("/login?errorOauth2=true&error");

      //FIXME remove after debugging
      oAuth2Login.failureHandler((req, res, ex) -> {
        ex.printStackTrace(); // or log.error("OAuth2 failure", ex)
        var msg = java.net.URLEncoder.encode(
            (ex.getClass().getSimpleName() + ": " + ex.getMessage()),
            java.nio.charset.StandardCharsets.UTF_8);
        res.sendRedirect("/login?errorOauth2=true&msg=" + msg);
      });
    });

    // Makes sure we stash the original auth before Spring Security triggers the OAuth2 redirect
    http.addFilterBefore(stashOrigAuthForInvenio(), OAuth2AuthorizationRequestRedirectFilter.class);

    // Let Vaadin register its filters/matchers
    super.configure(http);

    // Set the login view
    setLoginView(http, LoginLayout.class, contextPath + "/login?logout=true");
  }

  /**
   * Stash current auth before you kick off the Zenodo flow so we can restore it later.
   */
  @Bean
  public OncePerRequestFilter stashOrigAuthForInvenio() {
    final var matcher = new AntPathRequestMatcher("/oauth2/authorization/invenio");
    return new OncePerRequestFilter() {
      @Override
      protected void doFilterInternal(HttpServletRequest req,
          HttpServletResponse res,
          FilterChain chain)
          throws ServletException, java.io.IOException {

        if (matcher.matches(req)) {
          Authentication auth = SecurityContextHolder.getContext().getAuthentication();
          if (auth != null && auth.isAuthenticated()) {
            req.getSession(true).setAttribute("origAuth", auth);
          }
        }
        chain.doFilter(req, res);
      }
    };
  }

  @Bean
  @Qualifier("invenioWebClient")
  public WebClient invenioWebClient(OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager) {
    var oauth2 = new ServletOAuth2AuthorizedClientExchangeFilterFunction(oAuth2AuthorizedClientManager);
    oauth2.setDefaultClientRegistrationId("invenio");

    return WebClient.builder()
        .apply(oauth2.oauth2Configuration())
        .build();
  }

  /**
   * The manager will fetch registered clients for the user
   */
  @Bean
  public OAuth2AuthorizedClientManager authorizedClientManager(
      ClientRegistrationRepository registrations,
      OAuth2AuthorizedClientService clientService) {

    var provider = OAuth2AuthorizedClientProviderBuilder.builder()
        .authorizationCode()
        .refreshToken()
        .build();

    var manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(registrations,
        clientService);
    manager.setAuthorizedClientProvider(provider);
    return manager;
  }
}
