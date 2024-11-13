package life.qbic.datamanager.views.projects.edit;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.spring.annotation.UIScope;
import life.qbic.datamanager.views.events.ProjectDesignUpdateEvent;
import life.qbic.datamanager.views.general.DialogWindow;
import life.qbic.datamanager.views.general.Heading;
import life.qbic.datamanager.views.projects.ProjectInformation;
import life.qbic.datamanager.views.projects.project.info.SimpleParagraph;
import life.qbic.datamanager.views.strategy.dialog.DialogClosingStrategy;


/**
 * <b>Edit Project Design Dialog</b>
 *
 * <p>Dialog that is displayed to the user, when they want to enter project design information.</p>
 *
 * @since 1.6.0
 */
@UIScope
public class EditProjectDesignDialog extends DialogWindow {

  private final ProjectDesignForm form;

  private DialogClosingStrategy noChangesClosingStrategy;

  private DialogClosingStrategy warningClosingStrategy;

  public EditProjectDesignDialog(ProjectInformation project) {
    super();
    addClassName("large-dialog");
    var content = new Div();
    content.addClassName("vertical-list");
    setConfirmButtonLabel("Save");
    setCancelButtonLabel("Cancel");
    setHeaderTitle("Edit Project Design");
    content.add(Heading.withText("Project ID"));
    content.add(new SimpleParagraph(project.getProjectId()));
    form = new ProjectDesignForm();
    form.setContent(project);
    content.add(form);
    add(content);
  }

  public void setDefaultStrategy(DialogClosingStrategy strategy) {
    this.noChangesClosingStrategy = strategy;
  }

  public void setWarningStrategy(DialogClosingStrategy strategy) {
    this.warningClosingStrategy = strategy;
  }

  @Override
  protected void onConfirmClicked(ClickEvent<Button> clickEvent) {
    try {
      var projectInfo = form.fromUserInput();
      fireEvent(new ProjectDesignUpdateEvent(this, true, projectInfo));
    } catch (ValidationException e) {
      // Do nothing, the user needs to correct the input
    }
  }

  @Override
  protected void onCancelClicked(ClickEvent<Button> clickEvent) {
    if (form.hasChanges() && warningClosingStrategy != null) {
      warningClosingStrategy.execute();
    } else if (noChangesClosingStrategy != null) {
      noChangesClosingStrategy.execute();
    }
  }

  public void addUpdateEventListener(ComponentEventListener<ProjectDesignUpdateEvent> listener) {
    addListener(ProjectDesignUpdateEvent.class, listener);
  }
}
