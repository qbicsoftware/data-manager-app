package life.qbic.datamanager.views.project.view.components;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.textfield.TextField;
import java.io.Serial;
import life.qbic.datamanager.views.components.CardLayout;
import life.qbic.projectmanagement.application.ProjectInformationService;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class ProjectDetailsComponent extends Composite<CardLayout> {

  @Serial
  private static final long serialVersionUID = -5781313306040217724L;

  private final ProjectInformationService projectInformationService;

  TextField textField;

  private final ProjectDetailsHandler handler;

  public ProjectDetailsComponent(ProjectInformationService projectInformationService) {
    this.projectInformationService = projectInformationService;
    this.handler = new ProjectDetailsHandler(this);
    this.textField = new TextField();
    getContent().addFields(textField);
  }

  public void projectId(String projectId) {
    //this.projectInformationService.
    handler.projectId(projectId);

  }

}
