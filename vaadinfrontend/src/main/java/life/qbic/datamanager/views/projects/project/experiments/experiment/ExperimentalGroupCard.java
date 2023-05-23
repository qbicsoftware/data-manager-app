package life.qbic.datamanager.views.projects.project.experiments.experiment;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import life.qbic.datamanager.views.general.Card;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentalGroup;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class ExperimentalGroupCard extends Card {


  private Button deleteButton;

  private ExperimentalGroup experimentalGroup;
  @Serial
  private static final long serialVersionUID = -8400631799486647200L;

  private final Handler handler;

  public ExperimentalGroupCard(ExperimentalGroup experimentalGroup) {
    super();
    addClassName("experimental-group");
    this.experimentalGroup = experimentalGroup;
    handler = new Handler();
    deleteButton = buttonCreation();
    layoutComponent();
    configureEvents();
  }

  private static Button buttonCreation() {
    return new Button("delete");
  }

  private void configureEvents() {
    this.deleteButton.addClickListener(listener -> handler.fireDeletionEvent());
  }


  private void layoutComponent() {
    addClassNames(CARD_BASE_LAYOUT, "experimental-group");
    getContent().add(EXAMPLE_SECTION);
    getContent().add(deleteButton);
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
    handler.addDeletionEventListener(subscriber);
  }


  private class Handler implements Serializable {

    @Serial
    private static final long serialVersionUID = 8415291591599490668L;
    List<ComponentEventListener<ExperimentalGroupDeletionEvent>> listenersDeletionEvent;

    void addDeletionEventListener(ComponentEventListener<ExperimentalGroupDeletionEvent> listener) {
      this.listenersDeletionEvent.add(listener);
    }

    void fireDeletionEvent() {
      var deletionEvent = new ExperimentalGroupDeletionEvent(ExperimentalGroupCard.this, true);
      listenersDeletionEvent.forEach(listener -> listener.onComponentEvent(deletionEvent));
    }

  }

}
