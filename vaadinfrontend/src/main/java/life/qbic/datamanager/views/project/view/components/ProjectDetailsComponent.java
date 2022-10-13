package life.qbic.datamanager.views.project.view.components;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import java.io.Serial;
import java.util.Objects;
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

  final TextField titleField = new TextField();
  final FormLayout formLayout = new FormLayout();

  final TextArea experimentalDesignField = new TextArea();
  final TextArea projectObjective = new TextArea();

  private transient final ProjectDetailsHandler handler;

  public ProjectDetailsComponent(ProjectInformationService projectInformationService) {
    Objects.requireNonNull(projectInformationService);
    initLayout();
    setComponentStyles();
    this.handler = new ProjectDetailsHandler(this, projectInformationService);
  }

  private void initLayout() {
    formLayout.addFormItem(titleField, "Project Title");
    formLayout.addFormItem(projectObjective, "Project Objective");
    formLayout.addFormItem(experimentalDesignField, "Experimental Design");
    // set form layout to only have one column (for any width)
    formLayout.setResponsiveSteps(new ResponsiveStep("0", 1));
    getContent().addFields(formLayout);
  }

  private void setComponentStyles() {
    titleField.setSizeFull();
    projectObjective.setWidthFull();
    experimentalDesignField.setWidthFull();
    formLayout.setClassName("create-project-form");
  }

  public void projectId(String projectId) {
    //this.projectInformationService.
    handler.projectId(projectId);

  }

}
