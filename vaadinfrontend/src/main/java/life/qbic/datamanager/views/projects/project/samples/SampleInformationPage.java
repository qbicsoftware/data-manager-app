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
      @Autowired SampleMainComponent sampleMainComponent) {
    Objects.requireNonNull(projectNavigationBarComponent);
    Objects.requireNonNull(sampleMainComponent);
    this.addClassName("sample-page");
    setupPage(projectNavigationBarComponent, sampleMainComponent);
    sampleInformationPageHandler = new SampleInformationPageHandler(projectNavigationBarComponent,
        sampleMainComponent);
    log.debug(String.format(
        "\"New instance for Sample Information page (#%s) created with Project Navigation Bar Component (#%s) and Sample Main Component (#%s)",
        System.identityHashCode(this), System.identityHashCode(projectNavigationBarComponent),
        System.identityHashCode(sampleMainComponent)));
  }

  private void setupPage(ProjectNavigationBarComponent projectNavigationBarComponent,
      SampleMainComponent sampleMainComponent) {
    this.add(projectNavigationBarComponent);
    this.add(sampleMainComponent);
  }

  /**
   * Provides the {@link ProjectId} to the components within this page
   * <p>
   * This method serves as an entry point providing the necessary {@link ProjectId} to the
   * components within this cage
   *
   * @param projectId projectId of the selected project
   */
  public void projectId(ProjectId projectId) {
    sampleInformationPageHandler.setProjectId(projectId);
  }

  private static final class SampleInformationPageHandler {

    ProjectNavigationBarComponent projectNavigationBarComponent;
    SampleMainComponent sampleMainComponent;

    public SampleInformationPageHandler(ProjectNavigationBarComponent projectNavigationBarComponent,
        SampleMainComponent sampleMainComponent) {
      this.sampleMainComponent = sampleMainComponent;
      this.projectNavigationBarComponent = projectNavigationBarComponent;
    }

    public void setProjectId(ProjectId projectId) {
      projectNavigationBarComponent.projectId(projectId);
      sampleMainComponent.projectId(projectId);
    }
  }

}
