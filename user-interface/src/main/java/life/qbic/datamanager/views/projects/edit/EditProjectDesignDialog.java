package life.qbic.datamanager.views.projects.edit;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.spring.annotation.UIScope;
import life.qbic.datamanager.views.events.ProjectUpdateEvent;
import life.qbic.datamanager.views.general.DialogWindow;
import life.qbic.datamanager.views.general.Heading;
import life.qbic.datamanager.views.projects.edit.EditProjectInformationDialog.ProjectInformation;
import life.qbic.datamanager.views.projects.project.info.SimpleParagraph;


/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@UIScope
public class EditProjectDesignDialog extends DialogWindow {

  private final EditProjectDesignForm form;


  public EditProjectDesignDialog(ProjectInformation project) {
    super();
    addClassName("large-dialog");
    var content = new Div();
    content.addClassName("vertical-list");
    setConfirmButtonLabel("Save");
    setCancelButtonLabel("Cancel");
    setHeaderTitle("Project Design");
    content.add(Heading.withText("Project ID"));
    content.add(new SimpleParagraph(project.getProjectId()));
    form = new EditProjectDesignForm();
    form.setContent(project);
    content.add(form);
    add(content);
  }

  @Override
  protected void onConfirmClicked(ClickEvent<Button> clickEvent) {
    try {
      var projectInfo = form.fromUserInput();
      fireEvent(new ProjectUpdateEvent(this, true, projectInfo));
    } catch (ValidationException e) {
      // Do nothing, the user needs to correct the input
    }
  }

  @Override
  protected void onCancelClicked(ClickEvent<Button> clickEvent) {
    close();
  }

  public void addUpdateEventListener(ComponentEventListener<ProjectUpdateEvent> listener) {
    addListener(ProjectUpdateEvent.class, listener);
  }
}
