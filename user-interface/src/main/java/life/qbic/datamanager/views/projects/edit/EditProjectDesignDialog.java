package life.qbic.datamanager.views.projects.edit;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.spring.annotation.UIScope;
import life.qbic.datamanager.views.general.DialogWindow;
import life.qbic.datamanager.views.projects.edit.EditProjectInformationDialog.ProjectInformation;


/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@UIScope
public class EditProjectDesignDialog extends DialogWindow {

  private ProjectInformation initialInformation = new ProjectInformation();

  public EditProjectDesignDialog(ProjectInformation project) {
    super();
    var content = new Div();
    content.addClassName("vertical-list");
    setConfirmButtonLabel("Save");
    setCancelButtonLabel("Cancel");
    setHeaderTitle("Project Design");
    content.add(new Span(project.getProjectTitle()));
    var editForm = new EditProjectDesignForm();
    editForm.setContent(project);
    content.add(editForm);
    add(content);
  }

  @Override
  protected void onConfirmClicked(ClickEvent<Button> clickEvent) {

  }

  @Override
  protected void onCancelClicked(ClickEvent<Button> clickEvent) {

  }
}
