package life.qbic.datamanager.views.projects.project.experiments;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.projects.project.experiments.experiment.ExperimentDetailsComponent;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Experiment Content component
 * <p>
 * The content component is a {@link Div} container, which is responsible for hosting the components
 * handling the content within the {@link ExperimentInformationMain}. It is intended to propagate
 * experiment information provided in the {@link ExperimentDetailsComponent} to the
 * {@link ExperimentInformationMain} and vice versa and can be easily extended with additional
 * components if necessary
 */

@SpringComponent
@UIScope
public class ExperimentContentComponent extends Div {
  @Serial
  private static final long serialVersionUID = 464171225772721108L;
  private final ExperimentDetailsComponent experimentDetailsComponent;

  public ExperimentContentComponent(
      @Autowired ExperimentDetailsComponent experimentDetailsComponent) {
    this.experimentDetailsComponent = experimentDetailsComponent;
  }

  /**
   * Propagates the context to internal components.
   *
   * @param context the context in which the user is.
   */
  public void setContext(Context context) {
    context.experimentId().ifPresentOrElse(
        experimentId -> {
          experimentDetailsComponent.setContext(context);
          this.add(experimentDetailsComponent);
        }, () -> this.remove(experimentDetailsComponent)
    );
  }
}