package life.qbic.datamanager.views.projects.project.experiments.experiment.components;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.Html;
import life.qbic.datamanager.views.notifications.NotificationDialog;
import org.springframework.context.MessageSource;

/**
 * Notifies the user that experimental groups exist in the experiment.
 * <p>
 * This dialog is to be shown when editing variables is impossible as experimental groups are
 * present.
 */
public final class ExistingGroupsPreventVariableEdit extends NotificationDialog {

  private final MessageSource messageSource;

  public ExistingGroupsPreventVariableEdit(int numberOfExperimentalGroups,
      MessageSource messageSource) {
    super(Type.ERROR);
    this.messageSource = requireNonNull(messageSource, "messageSource must not be null");
    addClassName("existing-groups-prevent-variable-edit");
    withTitle("Cannot edit variables");

    String defaultMessage =
        "You have " + numberOfExperimentalGroups + " experimental groups. Please delete them.";
    String message = this.messageSource.getMessage(
        "experiment.design.variable.edit.groups-exist.message",
        new Object[]{numberOfExperimentalGroups}, defaultMessage, getLocale());
    withContent(new Html("<div>%s</div>".formatted(message)));

    String confirmText = messageSource.getMessage(
        "experiment.design.variable.edit.groups-exist.confirm-text", null, "Ok", getLocale());
    setConfirmText(confirmText);

    setCancelable(true);
  }

}
