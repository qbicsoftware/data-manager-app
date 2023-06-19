package life.qbic.datamanager.views.projects.project.samples;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.util.Objects;
import life.qbic.datamanager.views.projects.project.ProjectNavigationBarComponent;
import life.qbic.datamanager.views.projects.project.ProjectViewPage;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.domain.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Sample Information page
 * <p>
 * This page hosts the components necessary to show and update the actual sample information
 * associated with a provided {@link life.qbic.projectmanagement.domain.project.ProjectId} in the
 * URL
 */

@Route(value = "projects/:projectId?/samples", layout = ProjectViewPage.class)
@SpringComponent
@UIScope
@PermitAll
public class SampleInformationPage extends Div {

  @Serial
  private static final long serialVersionUID = 3778218989387044758L;
  private static final Logger log = LoggerFactory.logger(SampleInformationPage.class);
  private final transient SampleInformationPageHandler sampleInformationPageHandler;

  public SampleInformationPage(
      @Autowired ProjectNavigationBarComponent projectNavigationBarComponent,
      @Autowired SampleInformationContent sampleInformationContent) {
    Objects.requireNonNull(projectNavigationBarComponent);
    Objects.requireNonNull(sampleInformationContent);
    setupPage(projectNavigationBarComponent, sampleInformationContent);
    stylePage();
    sampleInformationPageHandler = new SampleInformationPageHandler(projectNavigationBarComponent,
        sampleInformationContent);
    log.debug(String.format(
        "\"New instance for Sample Information page (#%s) created with Project Navigation Bar Component (#%s) and Sample Overview Component (#%s)",
        System.identityHashCode(this), System.identityHashCode(projectNavigationBarComponent),
        System.identityHashCode(sampleInformationContent)));
  }

  private void setupPage(ProjectNavigationBarComponent projectNavigationBarComponent,
      SampleInformationContent sampleInformationContent) {
    this.add(projectNavigationBarComponent);
    this.add(sampleInformationContent);
  }

  public void projectId(ProjectId projectId) {
    sampleInformationPageHandler.setProjectId(projectId);
  }

  private void stylePage() {
    this.setWidthFull();
    this.setHeightFull();
  }

  private final class SampleInformationPageHandler {

    ProjectNavigationBarComponent projectNavigationBarComponent;
    SampleInformationContent sampleInformationContent;

    public SampleInformationPageHandler(ProjectNavigationBarComponent projectNavigationBarComponent,
        SampleInformationContent sampleInformationContent) {
      this.sampleInformationContent = sampleInformationContent;
      this.projectNavigationBarComponent = projectNavigationBarComponent;
    }

    public void setProjectId(ProjectId projectId) {
      projectNavigationBarComponent.projectId(projectId);
      sampleInformationContent.projectId(projectId);
    }
  }

}
