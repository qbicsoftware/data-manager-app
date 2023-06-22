package life.qbic.datamanager.views.projects.project.experiments;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import life.qbic.datamanager.views.projects.project.experiments.experiment.ExperimentDetailsComponent;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <class short description - One Line!>
 * <p>
 * <More detailed description - When to use, what it solves, etc.>
 *
 * @since <version tag>
 */

@SpringComponent
@UIScope
public class ExperimentMainComponent extends Div {

  private static final Logger log = LoggerFactory.logger(ExperimentMainComponent.class);
  @Serial
  private static final long serialVersionUID = 464171225772721108L;
  private final ExperimentDetailsComponent experimentDetailsComponent;

  public ExperimentMainComponent(@Autowired ExperimentDetailsComponent experimentDetailsComponent) {
    this.experimentDetailsComponent = experimentDetailsComponent;
    this.addClassName("main");
    this.add(experimentDetailsComponent);
  }

  public void setExperiment(ExperimentId experimentId) {
    experimentDetailsComponent.setExperiment(experimentId);
  }

}
