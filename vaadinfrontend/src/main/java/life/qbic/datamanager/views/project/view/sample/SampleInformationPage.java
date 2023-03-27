package life.qbic.datamanager.views.project.view.sample;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.Objects;
import life.qbic.datamanager.views.project.view.ProjectViewPage;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
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
@CssImport("./styles/views/project/project-view.css")
public class SampleInformationPage extends Div {

  @Serial
  private static final long serialVersionUID = 3778218989387044758L;

  private static final Logger log = LoggerFactory.logger(SampleInformationPage.class);
  private final transient SampleInformationPageHandler sampleInformationPageHandler;

  public SampleInformationPage(@Autowired SampleOverviewComponent sampleOverviewComponent) {
    Objects.requireNonNull(sampleOverviewComponent);
    add(sampleOverviewComponent);
    setComponentStyles(sampleOverviewComponent);
    sampleInformationPageHandler = new SampleInformationPageHandler(sampleOverviewComponent);
    log.debug(String.format(
        "\"New instance for Sample Information page (#%s) created with Sample Overview Component (#%s)",
        System.identityHashCode(this), System.identityHashCode(sampleOverviewComponent)));
  }

  public void projectId(String projectId) {
    sampleInformationPageHandler.setProjectId(projectId);
  }

  public void setComponentStyles(SampleOverviewComponent sampleOverviewComponent) {
    sampleOverviewComponent.setStyles("sample-overview-component");
  }

  private final class SampleInformationPageHandler {

    SampleOverviewComponent sampleOverviewComponent;

    public SampleInformationPageHandler(SampleOverviewComponent sampleOverviewComponent) {
      this.sampleOverviewComponent = sampleOverviewComponent;
    }

    public void setProjectId(String projectId) {
      sampleOverviewComponent.projectId(projectId);
    }
  }

}
