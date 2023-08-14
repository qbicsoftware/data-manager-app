package life.qbic.datamanager.views.projects.project.experiments.experiment.components;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
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
    removeClassName("dialog-window"); //FIXME

    Div content = new Div();
    content.add(
        new Div(new Text(
            "Editing experimental variables requires all experimental groups to be deleted.")),
        new Div(new Text("You have "), new Span("X"), new Text(" experimental groups.")),
        new Div(new Text("Please delete them to edit the variables.")));
    content.addClassName("content");

    H2 title = new H2("Cannot edit variables");
    title.addClassName("title");

    Icon errorIcon = new Icon(VaadinIcon.CLOSE_CIRCLE);
    errorIcon.setClassName("error-icon");

    setConfirmButtonLabel("Go to Experimental Groups");
    setCancelButtonLabel("Cancel");

    getHeader().add(errorIcon, title);
    getFooter().add(cancelButton, confirmButton);

    add(content);
  }
}
