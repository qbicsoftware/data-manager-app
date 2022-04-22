package life.qbic;

import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * The entry point of the Spring Boot application.
 * <p>
 * Use the @PWA annotation make the application installable on phones, tablets and some desktop
 * browsers.
 */
@SpringBootApplication
@Theme(value = "datamanager")
@PWA(name = "Data Manager", shortName = "Data Manager", offlineResources = {"images/logo.png"})
@NpmPackage(value = "line-awesome", version = "1.3.0")
public class Application extends SpringBootServletInitializer implements AppShellConfigurator {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);

    /*var userJpa = context.getBean(UserJpaRepository.class);

    Optional<User> result = userJpa.findUserById("c0b329ca-eaec-4365-9617-80154a23afaf");
    if (result.isPresent()) {
      System.out.println("Found user");
      User user = result.get();
      user.setEmail("new.address@gmail.com");
      userJpa.save(user);
    } else {
      System.out.println("No user found");
      User user = User.create("myawesomepassword", "Sven Fillinger", "example@mail.org");
      userJpa.save(user);
    }*/
  }
}
