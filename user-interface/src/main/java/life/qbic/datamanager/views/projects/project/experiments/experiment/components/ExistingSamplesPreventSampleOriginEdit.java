package life.qbic.datamanager.views.projects.project.experiments.experiment.components;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import life.qbic.datamanager.views.notifications.NotificationDialog;

/**
 * Notifies the user that samples exist in the experiment and reference the Ontology term the user
 * wanted to delete
 * <p>
 * This dialog is to be shown when editing variables is impossible as samples are
 * present.
 */
public class ExistingSamplesPreventSampleOriginEdit extends NotificationDialog {

  public ExistingSamplesPreventSampleOriginEdit(String ontologyLabel) {
    addClassName("existing-samples-prevent-variable-edit");
    customizeHeader();
    customizeContent(ontologyLabel);
    setConfirmText("Okay");
  }

  private void customizeHeader() {
    Icon errorIcon = new Icon(VaadinIcon.CLOSE_CIRCLE);
    errorIcon.setClassName("error-icon");
    setTitle("Cannot remove sample origin");
    setHeaderIcon(errorIcon);
  }

  private void customizeContent(String ontologyLabel) {
    content.add(
        new Div(new Text("'%s' cannot be deleted, as it is referenced in samples of this experiment.".formatted(ontologyLabel))));
  }
}
