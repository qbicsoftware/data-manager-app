package life.qbic.datamanager.security;

import com.vaadin.flow.spring.security.VaadinWebSecurity;
import life.qbic.datamanager.views.login.LoginHandler;
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

  private final LoginHandler loginHandler;

  public SecurityConfiguration(LoginHandler loginHandler) {
    this.loginHandler = loginHandler;
  }

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
    http.authorizeHttpRequests(v -> v.requestMatchers(
        new AntPathRequestMatcher("/dev/oauth2/authorization/orcid"), new AntPathRequestMatcher("/dev/oauth2/code/**"), new AntPathRequestMatcher("images/*.png")).permitAll());

    http.oauth2Login(oAuth2Login -> {
      oAuth2Login.loginPage("/login").permitAll();
      oAuth2Login.defaultSuccessUrl("/projects/list");
      oAuth2Login.failureUrl("/login?errorOauth2=true");
      oAuth2Login.failureHandler((request, response, exception) -> {
        request.getSession().setAttribute("error.message", exception.getMessage());
        loginHandler.showInformation("Something went wrong", "dfdss");
      });
    });
    //http.oauth2Login();

    //http.authorizeHttpRequests()
    // .requestMatchers(new AntPathRequestMatcher("/images/*.png"))
    //.permitAll();
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
