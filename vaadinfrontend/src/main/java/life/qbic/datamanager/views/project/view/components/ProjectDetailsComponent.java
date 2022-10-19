package life.qbic.datamanager.views.project.view.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.Objects;
import java.util.function.Consumer;
import life.qbic.datamanager.views.layouts.CardLayout;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Project Details Component
 * <p>
 * Shows project details to the user.
 *
 * @since 1.0.0
 */
@SpringComponent
@UIScope
public class ProjectDetailsComponent extends Composite<CardLayout> {

  @Serial
  private static final long serialVersionUID = -5781313306040217724L;

  private static final String TITLE = "Project Information";

  final TextField titleField = new TextField();
  final FormLayout formLayout = new FormLayout();

  final TextArea experimentalDesignField = new TextArea();
  final TextArea projectObjective = new TextArea();

  private final transient Handler handler;

  public ProjectDetailsComponent(@Autowired ProjectInformationService projectInformationService) {
    Objects.requireNonNull(projectInformationService);
    this.handler = new Handler(this, projectInformationService);
    handler.setFieldsEditableOnlyOnFocus();
    handler.attachSubmissionActionOnBlur();
    initLayout();
    setComponentStyles();
  }

  private void initLayout() {
    formLayout.addFormItem(titleField, "Project Title");
    formLayout.addFormItem(projectObjective, "Project Objective");
    formLayout.addFormItem(experimentalDesignField, "Experimental Design");
    // set form layout to only have one column (for any width)
    formLayout.setResponsiveSteps(new ResponsiveStep("0", 1));
    getContent().addFields(formLayout);
    getContent().addTitle(TITLE);
  }

  private void setComponentStyles() {
    titleField.setSizeFull();
    projectObjective.setWidthFull();
    experimentalDesignField.setWidthFull();
    formLayout.setClassName("create-project-form");
  }

  public void projectId(String projectId) {
    handler.projectId(projectId);
  }

  public void setStyles(String... componentStyles) {
    getContent().addClassNames(componentStyles);
  }

  /**
   * Component logic for the {@link ProjectDetailsComponent}
   *
   * @since 1.0.0
   */
  private final class Handler {

    private final ProjectDetailsComponent component;
    private final ProjectInformationService projectInformationService;

    private ProjectId selectedProject;

    public Handler(ProjectDetailsComponent component,
        ProjectInformationService projectInformationService) {
      this.component = component;
      this.projectInformationService = projectInformationService;
    }

    public void projectId(String projectId) {
      projectInformationService.find(ProjectId.parse(projectId)).ifPresentOrElse(
          this::loadProjectData,
          () -> component.titleField.setValue("Not found"));
    }

    private void loadProjectData(Project project) {
      this.selectedProject = project.getId();
      component.titleField.setValue(project.getProjectIntent().projectTitle().title());
      component.projectObjective.setValue(project.getProjectIntent().objective().value());
      project.getProjectIntent().experimentalDesign().ifPresentOrElse(
          experimentalDesignDescription -> component.experimentalDesignField.setValue(
              experimentalDesignDescription.value()),
          () -> component.experimentalDesignField.setPlaceholder("No description yet."));
    }


    public void setFieldsEditableOnlyOnFocus() {
      editableOnFocus(titleField);
      editableOnFocus(projectObjective);
      editableOnFocus(experimentalDesignField);
    }

    public void attachSubmissionActionOnBlur() {
      ProjectDetailsComponent.Handler.submitOnBlur(titleField, value ->
          //TODO replace with call to application service
          System.out.println("project " + selectedProject + ": submitting title = " + value));
      ProjectDetailsComponent.Handler.submitOnBlur(projectObjective, value ->
          //TODO replace with call to application service
          System.out.println("project " + selectedProject + ": submitting objective = " + value));
      ProjectDetailsComponent.Handler.submitOnBlur(experimentalDesignField, value ->
          //TODO replace with call to application service
          System.out.println(
              "project " + selectedProject + ": submitting experimental design description = "
                  + value));
    }

    private static <T extends Component & HasValue<?, ?> & Focusable<?>> void editableOnFocus(
        T element) {
      element.setReadOnly(true);
      element.addFocusListener(it -> element.setReadOnly(false));
      element.addBlurListener(it -> element.setReadOnly(true));
    }

    private static <V, T extends HasValue<?, V> & Focusable<?>> void submitOnBlur(T element,
        Consumer<V> submitAction) {
      element.addBlurListener(it -> submitAction.accept(element.getValue()));
    }

  }
}
