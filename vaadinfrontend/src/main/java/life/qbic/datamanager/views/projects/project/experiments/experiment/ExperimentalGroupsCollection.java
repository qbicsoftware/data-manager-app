package life.qbic.datamanager.views.projects.project.experiments.experiment;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import java.io.Serial;
import java.util.Collection;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class ExperimentalGroupsCollection extends Composite<Div> {

  @Serial
  private static final long serialVersionUID = -5835580091959912561L;

  public ExperimentalGroupsCollection() {

  }

  public void addComponents(Collection<ExperimentalGroupCard> experimentalGroupComponents) {
    experimentalGroupComponents.stream().forEach(experimentalGroupComponent ->
      getContent().add(experimentalGroupComponent)
    );
  }
}
