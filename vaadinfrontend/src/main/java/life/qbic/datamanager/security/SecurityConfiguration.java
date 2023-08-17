package life.qbic.datamanager.security;

import com.vaadin.flow.spring.security.VaadinWebSecurity;
import life.qbic.authorization.security.QBiCPasswordEncoder;
import life.qbic.datamanager.views.login.LoginLayout;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@EnableWebSecurity
@Configuration
@EnableCaching
@EnableMethodSecurity
public class SecurityConfiguration extends VaadinWebSecurity {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new QBiCPasswordEncoder();
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests().requestMatchers(new AntPathRequestMatcher("/images/*.png"))
        .permitAll();
    super.configure(http);
    setLoginView(http, LoginLayout.class);
  }
}
