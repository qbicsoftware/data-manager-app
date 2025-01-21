package life.qbic.datamanager.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Configuration
public class ZenodoSecurityConfiguration {

  @Bean
  public SecurityFilterChain secondarySecurityChain(HttpSecurity http) throws Exception {
//    http
//        .authorizeHttpRequests(auth -> auth
//            .requestMatchers("/", "/login", "/oauth2/authorization/zenodo2").permitAll() // Public paths
//            .requestMatchers("/oauth2/code/**").permitAll()
//            .anyRequest().authenticated() // Protect all other paths
//        )
//        .oauth2Login(oauth2 -> oauth2
//            .defaultSuccessUrl("/login2", true) // Redirect after login
//        );

    return http.build();
  }

}
