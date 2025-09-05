package life.qbic.datamanager;

import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;
import com.vaadin.flow.theme.Theme;
import java.io.Serial;
import life.qbic.identity.domain.registry.DomainRegistry;
import life.qbic.identity.domain.repository.UserRepository;
import life.qbic.identity.domain.service.UserDomainService;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.DataRepoConnectionTester;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;

/**
 * The entry point of the Spring Boot application.
 *
 * <p>Use the @PWA annotation make the application installable on phones, tablets and some desktop
 * browsers.
 */
@SpringBootApplication(exclude = {ErrorMvcAutoConfiguration.class})
@Theme(value = "datamanager")
@PWA(
    name = "Data Manager",
    shortName = "Data Manager",
    offlineResources = {"images/logo.png"})
@NpmPackage(value = "line-awesome", version = "1.3.0")
@ComponentScan(value = {"life.qbic"})
@Push(value = PushMode.AUTOMATIC, transport = Transport.LONG_POLLING)
public class Application extends SpringBootServletInitializer implements AppShellConfigurator {

  private static final Logger log = LoggerFactory.logger(Application.class.getName());

  @Serial
  private static final long serialVersionUID = -8182104817961102407L;

  public static void main(String[] args) {
    log.info("Starting data manager app...");

    var appContext = SpringApplication.run(Application.class, args);

    try {
      var connectionTester = appContext.getBean(DataRepoConnectionTester.class);
      //connectionTester.testApplicationServer();
      //connectionTester.testDatastoreServer();
    } catch (Exception e) {
      log.error(
          "Unexpected error occurred while starting data manager app during openBis connection.",
          e);
      SpringApplication.exit(appContext, () -> 1);
    }

    // We need to set up the domain registry and register important services:
    var userRepository = appContext.getBean(UserRepository.class);
    DomainRegistry.instance().registerService(new UserDomainService(userRepository));
  }
}
