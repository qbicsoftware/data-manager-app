package life.qbic.datamanager.security;

import com.vaadin.flow.spring.security.VaadinWebSecurity;
import life.qbic.datamanager.views.login.LoginLayout;
import life.qbic.identity.application.security.QBiCPasswordEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@EnableWebSecurity(debug = true)
@Configuration
//@Import({AclSecurityConfiguration.class}) // enable in case you need beans from the Acl config
public class SecurityConfiguration extends VaadinWebSecurity {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new QBiCPasswordEncoder();
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(v -> v.requestMatchers(
            new AntPathRequestMatcher("/oauth2/authorization/orcid"),
            new AntPathRequestMatcher("/oauth2/code/**"), new AntPathRequestMatcher("images/*.png"))
        .permitAll());

    http.oauth2Login(oAuth2Login -> {
      oAuth2Login.loginPage("/login").permitAll();
      oAuth2Login.defaultSuccessUrl("/auth");
      oAuth2Login.failureUrl("/login?errorOauth2=true&error");
    });
    super.configure(http);
    setLoginView(http, LoginLayout.class);
  }


}
