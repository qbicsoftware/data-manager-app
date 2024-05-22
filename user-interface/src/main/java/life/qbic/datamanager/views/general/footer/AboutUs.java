package life.qbic.datamanager.views.general.footer;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import life.qbic.datamanager.views.UserMainLayout;
import life.qbic.datamanager.views.general.InformationComponent;
import life.qbic.datamanager.views.general.Main;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;

/**
 * <class short description - One Line!>
 * <p>
 * <More detailed description - When to use, what it solves, etc.>
 *
 * @since <version tag>
 */
@SpringComponent
@UIScope
@Route(value = "about-us", layout = UserMainLayout.class)
@PermitAll
public class AboutUs extends Main {

  @Serial
  private static final long serialVersionUID = -7133361843956861809L;
  private static final Logger log = LoggerFactory.logger(AboutUs.class);
  private final InformationComponent aboutUsInformation = new InformationComponent();

  public AboutUs() {
    Span title = new Span("About Us");
    title.addClassName("main-title");
    addComponentAsFirst(title);
    setAboutUsInformation();
    addClassName("about-us");
    log.debug(String.format(
        "New instance for %s(#%s) created with %s(#%s)",
        this.getClass().getSimpleName(), System.identityHashCode(this),
        aboutUsInformation.getClass().getSimpleName(),
        System.identityHashCode(aboutUsInformation)));
  }

  private void setAboutUsInformation() {
    //Todo implement me
  }
}
