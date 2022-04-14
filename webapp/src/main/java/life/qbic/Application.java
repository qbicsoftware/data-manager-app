package life.qbic;

import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;

import life.qbic.datamanagement.Example;
import life.qbic.usermanagement.persistence.UserJpaRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * The entry point of the Spring Boot application.
 *
 * Use the @PWA annotation make the application installable on phones, tablets
 * and some desktop browsers.
 *
 */
@SpringBootApplication
@Theme(value = "datamanager")
@PWA(name = "Data Manager", shortName = "Data Manager", offlineResources = {"images/logo.png"})
@NpmPackage(value = "line-awesome", version = "1.3.0")
public class Application extends SpringBootServletInitializer implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        //run();
    }

    public static void run() {
        var context = new AnnotationConfigApplicationContext(UserJpaRepository.class);
        var userJpa = context.getBean(UserJpaRepository.class);

        System.out.println(userJpa.findUsersByEmail("sven.fillinger@qbic.uni-tuebingen.de"));

    }

}
