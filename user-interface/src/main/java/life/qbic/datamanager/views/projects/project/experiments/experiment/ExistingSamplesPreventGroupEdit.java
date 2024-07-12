package life.qbic.datamanager.views.projects.project.experiments.experiment;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import life.qbic.datamanager.views.notifications.NotificationDialog;

/**
 * Notifies the user that samples exist in the experiment.
 * <p>
 * This dialog is to be shown when editing experimental groups is impossible as samples are
 * present.
 */
public class ExistingSamplesPreventGroupEdit extends NotificationDialog {

  public ExistingSamplesPreventGroupEdit(int sampleCount) {
    super(Type.INFO);
    addClassName("existing-samples-prevent-group-edit");
    customizeHeader();
    customizeContent(sampleCount);
    setConfirmText("Okay");
  }

  private void customizeHeader() {
    Icon errorIcon = new Icon(VaadinIcon.CLOSE_CIRCLE);
    errorIcon.setClassName("error-icon");
    setTitle("Cannot edit experimental groups");
    setHeaderIcon(errorIcon);
  }

  private void customizeContent(int sampleCount) {
    Span sampleCountSpan = new Span(String.valueOf(sampleCount));
    sampleCountSpan.addClassName("sample-count");
    setContent(new Div(
        new Div(new Text(
            "Editing experimental groups is only possible if samples are not registered.")),
        new Div(new Text("You have "), sampleCountSpan,
            new Text(" sample%s registered.".formatted(sampleCount > 1 ? "s" : "")))));
  }
}
