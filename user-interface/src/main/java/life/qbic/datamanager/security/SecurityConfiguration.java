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

@EnableWebSecurity
@Configuration
//@Import({AclSecurityConfiguration.class}) // enable in case you need beans from the Acl config
public class SecurityConfiguration extends VaadinWebSecurity {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new QBiCPasswordEncoder();
  }

//  @Autowired
//  AclPermissionEvaluator permissionEvaluator;
//  protected AuthorizationManager<RequestAuthorizationContext> projectAuthorizationManager() {
//    return (authorization, object) -> {
//      String projectId = object.getVariables().get("projectId");
//      if (projectId == null) {
//        return new AuthorizationDecision(true);
//      }
//      return new AuthorizationDecision(
//          permissionEvaluator.hasPermission(authorization.get(), ProjectId.parse(projectId),
//              Project.class.getName(), BasePermission.READ));
//    };
//  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests()
        .requestMatchers(new AntPathRequestMatcher("/images/*.png"))
        .permitAll();
//        //vaadin ignores these configurations when navigating inside the app
//        .and()
//        .authorizeHttpRequests()
//        .requestMatchers(new AntPathRequestMatcher("/projects/list"))
//        .permitAll()
//        .and()
//        .authorizeHttpRequests()
//        .requestMatchers(new AntPathRequestMatcher("/projects/{projectId}/**"))
//        .access(projectAuthorizationManager());
    super.configure(http);
    setLoginView(http, LoginLayout.class);
  }


}
