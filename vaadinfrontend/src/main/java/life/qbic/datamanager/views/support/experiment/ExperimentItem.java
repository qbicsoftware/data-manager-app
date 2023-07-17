package life.qbic.datamanager.views.support.experiment;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.theme.lumo.LumoUtility.BorderRadius;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import life.qbic.datamanager.views.general.Card;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;

/**
 * <b>Experiment Item</b>
 * <p>
 * A light weight display of experiment information to enable the user to quickly scan and select an
 * experiment.
 *
 * @since 1.0.0
 */
public class ExperimentItem extends Card {

  @Serial
  private static final long serialVersionUID = 1787087141809506158L;

  private final Experiment experiment;

  private final Span activeTag = new Span("active");

  private final Span experimentTitle = new Span("no title set");

  private final Div progressSection = new Div();

  private final Div contentSection = new Div();

  private final Div activeSection = new Div();

  private final Icon flaskIcon = VaadinIcon.FLASK.create();

  private final List<ComponentEventListener<ExperimentItemClickedEvent>> selectionListeners = new ArrayList<>();

  private ExperimentItem(Experiment experiment) {
    this.experiment = experiment;
    layoutComponent();
    configureComponent();
  }

  private void layoutComponent() {
    addClassName("experiment-item");
    layoutExperimentLabel();
    layoutActiveSection();
  }

  private void configureComponent() {
    loadProgressStatus();
    loadExperimentTitle();
    setStatusToInactive();
    addListener(ExperimentItemClickedEvent.class,
        listener -> informListeners(new ExperimentItemClickedEvent(this, true)));
  }

  private void layoutExperimentLabel() {
    experimentTitle.addClassName("experiment-title");
    contentSection.addClassName("content-section");
    contentSection.add(experimentTitle);
    contentSection.add(flaskIcon);
    add(contentSection);
  }

  private void layoutActiveSection() {
    activeTag.getElement().getThemeList().add("badge success primary");
    activeTag.addClassName(BorderRadius.MEDIUM);
    activeSection.add(activeTag);
    activeSection.addClassName("active-section");
    add(activeSection);
  }

  private void loadProgressStatus() {
    progressSection.addClassName("incomplete");
    progressSection.setText("incomplete");
  }

  private void loadExperimentTitle() {
    experimentTitle.setText(experiment.getName());
  }

  private void setStatusToInactive() {
    activeTag.setVisible(false);
  }

  private void informListeners(ExperimentItemClickedEvent componentClickedEvent) {
    selectionListeners.forEach(listener -> listener.onComponentEvent(componentClickedEvent));
  }

  /**
   * Creates an {@link ExperimentItem} based on the information available in the provided
   * {@link Experiment}.
   *
   * @param experiment the experiment the item should render its information from
   * @return an {@link ExperimentItem} that displays concise information from the provided
   * {@link Experiment}
   * @since 1.0.0
   */
  public static ExperimentItem create(Experiment experiment) {
    Objects.requireNonNull(experiment);
    return new ExperimentItem(experiment);
  }


  /**
   * Queries the {@link ExperimentId} of referenced {@link Experiment} the item displays.
   *
   * @return the experiment id
   * @since 1.0.0
   */
  public ExperimentId experimentId() {
    return experiment.experimentId();
  }

  /**
   * Sets the item in its "active" state.
   *
   * @since 1.0.0
   */
  public void setAsActive() {
    activeTag.setVisible(true);
  }

  /**
   * Sets the item in its "inactive" state
   *
   * @since 1.0.0
   */
  public void setAsInactive() {
    activeTag.setVisible(false);
  }

  /**
   * Renders the item in its "selected" state.
   *
   * @since 1.0.0
   */
  public void setAsSelected() {
    addClassNames("selected");
  }

  /**
   * Add a listener that will be called, if the experiment item has been clicked.
   *
   * @param listener the listener to be called upon
   * @since 1.0.0
   */
  public void addSelectionListener(ComponentEventListener<ExperimentItemClickedEvent> listener) {
    selectionListeners.add(listener);
  }

}
