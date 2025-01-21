package life.qbic.datamanager.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

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
    http
        .securityMatcher("/oauth2/code**") // Only handle routes related to Zenodo OAuth2 flow
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(new AntPathRequestMatcher("/oauth2/code*"),
                new AntPathRequestMatcher("/oauth2/callback")).permitAll() // Allow the callback
            .anyRequest().authenticated()
        )
        .oauth2Login(oauth2 -> oauth2
                .loginPage("/whoami").permitAll()
                .defaultSuccessUrl("/whoami/zenodo/success", true).failureUrl("/zenodo/failure")
            // Redirect after successful login
        );

    return http.build();
  }

}
