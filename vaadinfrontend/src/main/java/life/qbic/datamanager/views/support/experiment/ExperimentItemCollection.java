package life.qbic.datamanager.views.support.experiment;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Tag(Tag.DIV)
public class ExperimentItemCollection extends Div {

  @Serial
  private static final long serialVersionUID = -2196400941684042549L;

  private final List<ExperimentItem> items = new ArrayList<>();

  private final List<ComponentEventListener<ExperimentItemClickedEvent>> listeners = new ArrayList<>();

  private ExperimentItemCollection() {
    layoutComponent();
  }

  private void layoutComponent() {
    addClassName("experiment-item-collection");
  }

  public static ExperimentItemCollection create() {
    return new ExperimentItemCollection();
  }

  public void addExperimentItem(ExperimentItem item) {
    items.add(item);
    add(item);
    subscribeToClickEvent(item);
  }

  private void subscribeToClickEvent(ExperimentItem item) {
    item.addSelectionListener(
        (ComponentEventListener<ExperimentItemClickedEvent>) this::fireClickEvent);
  }

  private void fireClickEvent(ExperimentItemClickedEvent event) {
    listeners.forEach(listener -> listener.onComponentEvent(event));
  }

  public Optional<ExperimentItem> findBy(ExperimentId id) {
    return items.stream().filter(experimentItem -> experimentItem.experimentId().equals(id))
        .findAny();
  }

  public void addClickEventListener(ComponentEventListener<ExperimentItemClickedEvent> listener) {
    listeners.add(listener);
  }

}
