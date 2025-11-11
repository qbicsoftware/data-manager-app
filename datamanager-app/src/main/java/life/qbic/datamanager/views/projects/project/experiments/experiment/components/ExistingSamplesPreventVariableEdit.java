package life.qbic.datamanager.views.projects.project.experiments.experiment.components;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import life.qbic.datamanager.views.notifications.NotificationDialog;
import life.qbic.datamanager.views.notifications.NotificationLevel;

/**
 * Notifies the user that samples exist in the experiment.
 * <p>
 * This dialog is to be shown when editing variables is impossible as samples are
 * present.
 */
public class ExistingSamplesPreventVariableEdit extends NotificationDialog {

  public ExistingSamplesPreventVariableEdit(int sampleCount) {
    super(NotificationLevel.ERROR);
    addClassName("existing-samples-prevent-variable-edit");
    withTitle("Cannot edit variables");
    customizeContent(sampleCount);
    setConfirmText("Okay");
  }


  private void customizeContent(int sampleCount) {
    Span sampleCountSpan = new Span(String.valueOf(sampleCount));
    sampleCountSpan.addClassName("sample-count");
    withContent(
        new Div(new Text(
            "Editing experimental variables is only possible if samples are not registered.")),
        new Div(new Text("You have "), sampleCountSpan,
            new Text(" sample%s registered.".formatted(sampleCount > 1 ? "s" : ""))));
  }
}
