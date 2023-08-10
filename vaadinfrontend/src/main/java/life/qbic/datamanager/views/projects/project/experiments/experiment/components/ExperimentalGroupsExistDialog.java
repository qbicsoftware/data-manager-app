package life.qbic.datamanager.views.projects.project.experiments.experiment.components;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import life.qbic.datamanager.views.general.DialogWindow;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
//@Tag("my-awesome-dialog")
public class ExperimentalGroupsExistDialog extends DialogWindow {

  public ExperimentalGroupsExistDialog() {
    addClassName("experimental-groups-exist-dialog");
    Div content = new Div(new Text("Hello World!"));
    content.addClassName("content");
    add(content);
  }
}
