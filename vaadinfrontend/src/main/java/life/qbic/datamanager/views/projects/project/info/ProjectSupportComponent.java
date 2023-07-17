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
 * Project support component
 * <p>
 * The support component is a {@link Div} container, which is responsible for hosting the components
 * handling the sidebar content within the {@link ProjectInformationMain}. It propagates the project
 * information provided in the {@link ProjectLinksComponent} to the {@link ProjectInformationMain}
 * and vice versa and can be easily extended with additional components if necessary
 */
@SpringComponent
@UIScope
public class ProjectSupportComponent extends Div {

  @Serial
  private static final long serialVersionUID = -6996282848714468102L;
  private final ProjectLinksComponent projectLinksComponent;
  private static final Logger log = LoggerFactory.logger(ProjectSupportComponent.class);

  public ProjectSupportComponent(@Autowired ProjectLinksComponent projectLinksComponent) {
    Objects.requireNonNull(projectLinksComponent);
    this.projectLinksComponent = projectLinksComponent;
    layoutComponent();
  }

  private void layoutComponent() {
    this.add(projectLinksComponent);
  }

  /**
   * Provides the {@link ProjectId} to the components within this container
   * <p>
   * This method serves as an entry point providing the necessary {@link ProjectId} to components
   * within this component, so they can retrieve the information associated with the
   * {@link ProjectId}
   */
  public void projectId(ProjectId projectId) {
    projectLinksComponent.projectId(projectId);
  }

}
