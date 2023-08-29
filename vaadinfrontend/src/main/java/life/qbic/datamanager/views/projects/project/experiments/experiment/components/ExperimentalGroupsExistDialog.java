package life.qbic.datamanager.views.projects.project.experiments.experiment.components;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public class ExperimentalGroupsExistDialog extends ConfirmDialog {

  protected final Button cancelButton = new Button("Cancel");

  public ExperimentalGroupsExistDialog(int numberOfExperimentalGroups) {
    cancelButton.setThemeName("tertiary");
    addClassName("experimental-groups-exist-dialog");

    Div content = new Div();
    Span experimentalGroupCount = new Span(String.valueOf(numberOfExperimentalGroups));
    experimentalGroupCount.addClassName("experimental-group-count");
    content.add(
        new Div(new Text(
            "Editing experimental variables requires all experimental groups to be deleted.")),
        new Div(new Text("You have "), experimentalGroupCount,
            new Text(
                " experimental group%s.".formatted(numberOfExperimentalGroups > 1 ? "s" : ""))),
        new Div(new Text("Please delete the group%s to edit the variables.".formatted(
            numberOfExperimentalGroups > 1 ? "s" : ""))));
    content.addClassName("content");

    H2 title = new H2("Cannot edit variables");
    title.addClassName("title");
    Icon errorIcon = new Icon(VaadinIcon.CLOSE_CIRCLE);
    errorIcon.setClassName("error-icon");
    setHeader(new Span(errorIcon, title));

    setConfirmText("Go to Experimental Groups");
    setRejectable(true);
    setRejectButton(cancelButton);

    add(content);

  }
}
