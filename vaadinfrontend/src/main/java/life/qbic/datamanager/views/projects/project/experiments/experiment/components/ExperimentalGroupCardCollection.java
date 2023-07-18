package life.qbic.datamanager.views.projects.project.experiments.experiment.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.Tag;
import java.io.Serial;
import java.util.Collection;
import life.qbic.datamanager.views.projects.project.experiments.experiment.ExperimentalGroupCard;

/**
 * <b>Experimental Group Collection</b>
 * <p>
 * Container of one or more {@link ExperimentalGroupCard}
 *
 * @since 1.0.0
 */
@Tag(Tag.DIV)
public class ExperimentalGroupCardCollection extends Component implements HasComponents, HasSize {

  @Serial
  private static final long serialVersionUID = -5835580091959912561L;

  public ExperimentalGroupCardCollection() {
    addClassName("experimental-group-card-collection");
  }

  public void setComponents(Collection<ExperimentalGroupCard> experimentalGroupComponents) {
    removeAll();
    addComponents(experimentalGroupComponents);
  }

  public void addComponents(Collection<ExperimentalGroupCard> experimentalGroupComponents) {
    experimentalGroupComponents.forEach(this::add);
  }

  /**
   * Add a component as the last child. If the component has a parent, it is removed from that
   * parent first.
   *
   * @param component the component to add
   */
  public void addComponentAsLast(Component component) {
    addComponentAtIndex(getElement().getChildCount(), component);
  }

}
