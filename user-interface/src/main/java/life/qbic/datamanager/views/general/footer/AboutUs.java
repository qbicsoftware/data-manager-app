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
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.landing.LandingPageLayout;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;

/**
 * About Us
 * <p>
 * About Us Main Component showing background information about the QBiC data managing environment
 */
@SpringComponent
@UIScope
@Route(value = "about-us", layout = LandingPageLayout.class)
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
        "qPortal is a web-based science gateway providing users with an intuitive way to create (qWizard), manage, and analyze quantitative biological data (qNavigator). "
            + "The portal comprises a backend consisting of databases, data stores, data models and means of data transfer, as well as front-end solutions to give users access to data management and easy-to-use analysis options. "
            + "We provide a one-stop-shop solution for biomedical projects, providing up to date analysis pipelines, quality control workflows, and visualization tools. "
            + "Through intensive user interactions, appropriate data models have been developed. "
            + "These models build the foundation of our biological data management system and provide possibilities to annotate data, query existing metadata for statistics and future re-analysis on high-performance computing systems via coupling of workflow management systems.");
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
