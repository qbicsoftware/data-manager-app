package life.qbic.datamanager.views.projects.project.samples;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.controlling.domain.model.project.ProjectId;

/**
 * Sample support component
 * <p>
 * The support component is a {@link Div} container, which is responsible for hosting the components
 * handling the sidebar content within the {@link SampleInformationMain}. It propagates the sample
 * information provided within its components to the {@link SampleInformationMain} and vice versa
 * and can be easily extended with additional components if necessary
 */
@SpringComponent
@UIScope
public class SampleSupportComponent extends Div {

  @Serial
  private static final long serialVersionUID = 6214605184545498061L;
  private static final Logger log = LoggerFactory.logger(SampleSupportComponent.class);

  public SampleSupportComponent() {
    //Will contain the BatchOverviewComponent

  }

  /**
   * Provides the {@link ProjectId} to the components within this container
   * <p>
   * This method serves as an entry point providing the necessary {@link ProjectId} to components
   * within this component, so they can retrieve the information associated with the
   * {@link ProjectId}
   */
  public void projectId(ProjectId projectId) {

    //Will propagate the ProjectId to the future BatchOverviewComponent
  }

}
