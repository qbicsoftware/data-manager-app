package life.qbic.datamanager.views.projects.project.experiments.experiment;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Span;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import life.qbic.datamanager.views.general.Card;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentalGroup;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *Ø
 * @since <version tag>
 */
public class ExperimentalGroupCard extends Card {

  private Button deleteButton;
  private ExperimentalGroup experimentalGroup;
  private List<ComponentEventListener<ExperimentalGroupDeletionEvent>> listenersDeletionEvent;
  @Serial
  private static final long serialVersionUID = -8400631799486647200L;

  public ExperimentalGroupCard(ExperimentalGroup experimentalGroup) {
    super();
    this.experimentalGroup = experimentalGroup;
    this.deleteButton = buttonCreation();
    this.listenersDeletionEvent = new ArrayList<>();
    setTitle();
    setSampleSize(experimentalGroup);
    add(deleteButton);
    layoutComponent();
    configureEvents();
  }

  private void setSampleSize(ExperimentalGroup experimentalGroup) {
    Span span = new Span();
    span.add("Sample size:");
    span.add(String.valueOf(experimentalGroup.sampleSize()));
    this.add(span);
  }

  private void setTitle() {
    H5 cardTitle = new H5();
    cardTitle.setText("Experimental Group");
    this.add(cardTitle);
  }

  private static Button buttonCreation() {
    return new Button("delete");
  }

  private void layoutComponent() {
    addClassName("experimental-group");
  }

  private void configureEvents() {
    this.deleteButton.addClickListener(listener -> fireDeletionEvent());
  }

  public void addDeletionEventListener(
      ComponentEventListener<ExperimentalGroupDeletionEvent> listener) {
    this.listenersDeletionEvent.add(listener);
  }

  public void fireDeletionEvent() {
    var deletionEvent = new ExperimentalGroupDeletionEvent(ExperimentalGroupCard.this, true);
    listenersDeletionEvent.forEach(listener -> listener.onComponentEvent(deletionEvent));
  }

  public void setExperimentalGroup(ExperimentalGroup experimentalGroup) {
    this.experimentalGroup = experimentalGroup;
  }

  public long groupId() {
    return this.experimentalGroup.id();
  }

  public ExperimentalGroup experimentalGroup() {
    return this.experimentalGroup;
  }

  public void subscribeToDeletionEvent(
      ComponentEventListener<ExperimentalGroupDeletionEvent> subscriber) {
    this.listenersDeletionEvent.add(subscriber);
  }

}
