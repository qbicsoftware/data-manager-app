package life.qbic.datamanager.views.projects.project.info;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.Objects;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.domain.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Project Content component
 * <p>
 * The content component is a {@link Div} container, which is responsible for hosting the components
 * handling the content within the {@link ProjectInformationMain}. It is intended to propagate
 * project information provided in the {@link ProjectDetailsComponent} to the
 * {@link ProjectInformationMain} and vice versa and can be easily extended with additional
 * components if necessary
 */

@SpringComponent
@UIScope
public class ProjectContentComponent extends Div {

  private static final Logger log = LoggerFactory.logger(ProjectContentComponent.class);
  @Serial
  private static final long serialVersionUID = -1061134126086910532L;
  private final ProjectDetailsComponent projectDetailsComponent;

  public ProjectContentComponent(
      @Autowired ProjectDetailsComponent projectDetailsComponent) {
    Objects.requireNonNull(projectDetailsComponent);
    this.projectDetailsComponent = projectDetailsComponent;
    layoutComponent();
  }

  private void layoutComponent() {
    this.add(projectDetailsComponent);
  }

  /**
   * Triggers the propagation of the provided {@link ProjectId} to internal components
   *
   * @param projectId The projectId to be propagated
   */
  public void projectId(ProjectId projectId) {
    projectDetailsComponent.projectId(projectId);
  }

}
