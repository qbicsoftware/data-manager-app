package life.qbic.datamanager.views.projects.project.experiments;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;

/**
 * Experiment support component
 * <p>
 * The support component is a {@link Div} container, which is responsible for hosting the components
 * handling the sidebar content within the {@link ExperimentInformationMain}.
 */
@SpringComponent
@UIScope
public class ExperimentSupportComponent extends Div {
  @Serial
  private static final long serialVersionUID = -6996282848714468102L;

  public ExperimentSupportComponent() {
    //Needed for ExperimentInformationMain
  }
}
