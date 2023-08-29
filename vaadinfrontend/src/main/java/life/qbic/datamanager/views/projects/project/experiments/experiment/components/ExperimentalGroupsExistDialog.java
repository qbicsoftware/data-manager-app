package life.qbic.datamanager.views.projects.project.experiments.experiment.components;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.shared.Registration;
import life.qbic.datamanager.views.general.DialogWindow;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public class ExperimentalGroupsExistDialog extends DialogWindow {

  public ExperimentalGroupsExistDialog(int numberOfExperimentalGroups) {

    addClassName("experimental-groups-exist-dialog");
    removeClassName("dialog-window"); //FIXME

    Div content = new Div();
    content.add(
        new Div(new Text(
            "Editing experimental variables requires all experimental groups to be deleted.")),
        new Div(new Text("You have "), new Span(String.valueOf(numberOfExperimentalGroups)),
            new Text(
                " experimental group%s.".formatted(numberOfExperimentalGroups > 1 ? "s" : ""))),
        new Div(new Text("Please delete the group%s to edit the variables.".formatted(
            numberOfExperimentalGroups > 1 ? "s" : ""))));
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
    confirmButton.addClickListener(
        buttonClickEvent -> fireEvent(new ConfirmEvent(this, buttonClickEvent.isFromClient())));
    cancelButton.addClickListener(
        buttonClickEvent -> fireEvent(new CancelEvent(this, buttonClickEvent.isFromClient())));

  }

  public Registration addConfirmListener(ComponentEventListener<ConfirmEvent> confirmListener) {
    return addListener(ConfirmEvent.class, confirmListener);
  }

  public Registration addCancelListener(ComponentEventListener<CancelEvent> canelListener) {
    return addListener(CancelEvent.class, canelListener);
  }

  @DomEvent("confirm")
  public static class ConfirmEvent extends ComponentEvent<ExperimentalGroupsExistDialog> {

    public ConfirmEvent(ExperimentalGroupsExistDialog source, boolean fromClient) {
      super(source, fromClient);
    }
  }

  @DomEvent("cancel")
  public static class CancelEvent extends ComponentEvent<ExperimentalGroupsExistDialog> {

    public CancelEvent(ExperimentalGroupsExistDialog source, boolean fromClient) {
      super(source, fromClient);
    }
  }
}
