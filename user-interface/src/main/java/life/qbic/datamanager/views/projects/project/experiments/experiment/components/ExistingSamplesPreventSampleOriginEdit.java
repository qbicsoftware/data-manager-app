package life.qbic.datamanager.views.projects.project.experiments.experiment.components;

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
    super(Type.INFO);
    addClassName("existing-samples-prevent-variable-edit");
    customizeHeader();
    withContent(new Div(
        "'%s' cannot be deleted, as it is referenced in samples of this experiment.".formatted(
            ontologyLabel)));
    setConfirmText("Okay");
  }

  private void customizeHeader() {
    Icon errorIcon = new Icon(VaadinIcon.CLOSE_CIRCLE);
    errorIcon.setClassName("error-icon");
    withTitle("Cannot remove sample origin");
    withHeaderIcon(errorIcon);
  }

}
