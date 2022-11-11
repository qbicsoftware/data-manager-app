package life.qbic.datamanager.security;

import com.vaadin.flow.spring.security.VaadinWebSecurityConfigurerAdapter;
import life.qbic.authorization.security.QBiCPasswordEncoder;
import life.qbic.datamanager.views.login.LoginLayout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;

@EnableWebSecurity
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfiguration extends VaadinWebSecurityConfigurerAdapter {

  public static final String LOGOUT_URL = "/";

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new QBiCPasswordEncoder();
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    super.configure(http);
    setLoginView(http, LoginLayout.class, LOGOUT_URL);
  }

  @Override
  public void configure(WebSecurity web) throws Exception {
    super.configure(web);
    web.ignoring().antMatchers("/images/*.png");
  }
}
