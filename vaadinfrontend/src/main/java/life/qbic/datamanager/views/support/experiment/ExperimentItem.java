package life.qbic.datamanager.views.support.experiment;

import com.vaadin.flow.component.html.Span;
import java.io.Serial;
import java.util.Objects;
import life.qbic.datamanager.views.general.Card;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class ExperimentItem extends Card {

  @Serial
  private static final long serialVersionUID = 1787087141809506158L;

  private final Experiment experiment;

  private final Span activeTag = new Span("not set");

  private final Span progressTag = new Span("progress");

  private ExperimentItem(Experiment experiment) {
    this.experiment = experiment;
    layoutComponent();
  }

  private void layoutComponent() {
    addClassName("experiment-item");
    layoutProgressStatus();
  }

  private void layoutProgressStatus() {
    progressTag.addClassName("progress-tag");
    add(progressTag);
  }

  public static ExperimentItem create(Experiment experiment) {
    Objects.requireNonNull(experiment);
    return new ExperimentItem(experiment);
  }

}
