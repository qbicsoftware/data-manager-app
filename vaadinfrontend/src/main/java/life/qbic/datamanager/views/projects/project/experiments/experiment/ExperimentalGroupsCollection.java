package life.qbic.datamanager.views.projects.project.experiments.experiment;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.theme.lumo.LumoUtility.Gap;
import java.io.Serial;
import java.util.Collection;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Tag(Tag.DIV)
public class ExperimentalGroupsCollection extends Component implements HasComponents, HasSize {

  @Serial
  private static final long serialVersionUID = -5835580091959912561L;

  public ExperimentalGroupsCollection() {
    addClassName(Gap.SMALL);
    addClassName("card-deck");
  }

  public void setComponents(Collection<ExperimentalGroupCard> experimentalGroupComponents) {
    removeAll();
    addComponents(experimentalGroupComponents);
  }

  public void addComponents(Collection<ExperimentalGroupCard> experimentalGroupComponents) {
    experimentalGroupComponents.forEach(this::add);
  }

}
