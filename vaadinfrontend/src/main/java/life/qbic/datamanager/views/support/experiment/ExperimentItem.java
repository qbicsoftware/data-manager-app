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

  private final Span activeTag = new Span("active");

  private final Span experimentTitle = new Span("no title set");

  private final Div progressSection = new Div();

  private final Div contentSection = new Div();

  private final Div activeSection = new Div();

  private final Icon flaskIcon = VaadinIcon.FLASK.create();

  private final Span progressTag = new Span("no progress set");

  private final List<ComponentEventListener<ExperimentItemClickedEvent>> selectionListeners = new ArrayList<>();

  private ExperimentItem(Experiment experiment) {
    this.experiment = experiment;
    layoutComponent();
    configureComponent();
  }

  private void configureComponent() {
    loadProgressStatus();
    loadExperimentTitle();
    setStatusToInactive();
    addListener(ExperimentItemClickedEvent.class, listener -> informListeners(new ExperimentItemClickedEvent(this, true)));
  }

  private void setStatusToInactive() {
    activeTag.setVisible(false);
  }

  private void loadProgressStatus() {
    progressSection.addClassName("incomplete-experiment");
    progressSection.setText("incomplete");
  }

  private void loadExperimentTitle() {
    experimentTitle.setText(experiment.getName());
  }

  private void layoutComponent() {
    addClassName("experiment-item");
    layoutProgressStatus();
    layoutExperimentLabel();
    layoutActiveSection();
  }

  private void layoutActiveSection() {
    activeTag.getElement().getThemeList().add("badge success primary");
    activeTag.addClassName(BorderRadius.MEDIUM);
    activeSection.add(activeTag);
    activeSection.addClassName("active-section");
    add(activeSection);
  }

  private void layoutExperimentLabel() {
    experimentTitle.addClassName("experiment-title");
    contentSection.addClassName("content-section");
    contentSection.add(experimentTitle);
    contentSection.add(flaskIcon);
    add(contentSection);
  }

  private void layoutProgressStatus() {
    progressSection.addClassName("progress-section");
    progressTag.addClassName("progress-tag");
    progressSection.add(progressTag);
    add(progressSection);
  }

  public static ExperimentItem create(Experiment experiment) {
    Objects.requireNonNull(experiment);
    return new ExperimentItem(experiment);
  }

  public ExperimentId experimentId() {
    return experiment.experimentId();
  }

  public void setAsActive() {
    activeTag.setVisible(true);
  }

  public void setAsInactive() {
    activeTag.setVisible(false);
  }

  public void setAsSelected() {

  }

  public void addSelectionListener(ComponentEventListener<ExperimentItemClickedEvent> listener) {
    selectionListeners.add(listener);
  }

  private void informListeners(ExperimentItemClickedEvent componentClickedEvent) {
    selectionListeners.forEach(listener -> listener.onComponentEvent(componentClickedEvent));
  }

}
