package life.qbic.datamanager.views.projects.project.experiments.experiment.components;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import life.qbic.datamanager.views.notifications.NotificationDialog;

/**
 * Notifies the user that experimental groups exist in the experiment.
 * <p>
 * This dialog is to be shown when editing variables is impossible as experimental groups are
 * present.
 */
public final class ExistingGroupsPreventVariableEdit extends NotificationDialog {

  public ExistingGroupsPreventVariableEdit(int numberOfExperimentalGroups) {
    super(Type.INFO);
    addClassName("existing-groups-prevent-variable-edit");
    customizeHeader();
    customizeContent(numberOfExperimentalGroups);
    customizeRejection();
    setConfirmText("Go to Experimental Groups");
  }

  private void customizeRejection() {
    setRejectable(true);
    Button cancelButton = new Button("Cancel");
    cancelButton.setThemeName("tertiary");
    setRejectButton(cancelButton);
  }

  private void customizeContent(int numberOfExperimentalGroups) {
    Span experimentalGroupCount = new Span(String.valueOf(numberOfExperimentalGroups));
    experimentalGroupCount.addClassName("experimental-group-count");
    setContent(new Div(
        new Div(new Text(
            "Editing experimental variables requires all experimental groups to be deleted.")),
        new Div(new Text("You have "), experimentalGroupCount,
            new Text(
                " experimental group%s.".formatted(numberOfExperimentalGroups > 1 ? "s" : ""))),
        new Div(new Text("Please delete the group%s to edit the variables.".formatted(
            numberOfExperimentalGroups > 1 ? "s" : "")))));
  }

  private void customizeHeader() {
    Icon errorIcon = new Icon(VaadinIcon.CLOSE_CIRCLE);
    errorIcon.setClassName("error-icon");
    setTitle("Cannot edit variables");
    setHeaderIcon(errorIcon);
  }
}
