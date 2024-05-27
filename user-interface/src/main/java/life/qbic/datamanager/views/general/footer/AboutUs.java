package life.qbic.datamanager.views.general.footer;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import life.qbic.datamanager.views.DataManagerLayout;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;

/**
 * About Us
 * <p>
 * About Us Main Component showing background information about the QBiC data managing environment
 */
@SpringComponent
@UIScope
@Route(value = "about-us", layout = DataManagerLayout.class)
@AnonymousAllowed
public class AboutUs extends Main {

  @Serial
  private static final long serialVersionUID = -7133361843956861809L;
  private static final Logger log = LoggerFactory.logger(AboutUs.class);
  private final PageArea aboutUsComponent = new PageArea() {
  };

  public AboutUs() {
    Span title = new Span("About Us");
    title.addClassName("title");
    aboutUsComponent.addComponentAsFirst(title);
    setAboutUsInformation();
    add(aboutUsComponent);
    addClassName("about-us");
    log.debug(String.format(
        "New instance for %s(#%s) created",
        this.getClass().getSimpleName(), System.identityHashCode(this)));
  }

  private void setAboutUsInformation() {
    aboutUsComponent.add(createIntroduction());
    aboutUsComponent.add(createServiceList());
    aboutUsComponent.add(createOutro());
  }

  private Paragraph createIntroduction() {
    Paragraph introduction = new Paragraph();
    introduction.setText(
        "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. "
            + "At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. "
            + "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. "
            + "At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. "
            + "At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet."
            + "Duis autem vel eum iriure dolor in hendrerit in vulputate velit esse molestie consequat, vel illum dolore eu feugiat nulla facilisis at vero eros et accumsan et iusto odio dignissim qui blandit praesent luptatum zzril delenit augue duis dolore te feugait nulla facilisi. "
            + "Lorem ipsum dolor sit amet,");
    return introduction;
  }

  private Div createServiceList() {
    Div services = new Div();
    Span introduction = new Span("Services we offer");
    introduction.addClassName("bold");
    ListItem dataStorage = new ListItem("Data storage");
    ListItem dataManagement = new ListItem("Data management");
    ListItem dataAnalysis = new ListItem("Data analysis");
    UnorderedList serviceListing = new UnorderedList(dataStorage, dataManagement, dataAnalysis);
    services.add(introduction, serviceListing);
    return services;
  }

  private Span createOutro() {
    Anchor linkToWebsite = new Anchor(
        "https://uni-tuebingen.de/forschung/forschungsinfrastruktur/zentrum-fuer-quantitative-biologie-qbic/",
        "Visit our website", AnchorTarget.BLANK);
    Span outroText = new Span("to learn more about QBiC");
    Span outro = new Span(linkToWebsite, outroText);
    outro.addClassName("inline");
    return outro;
  }
}
