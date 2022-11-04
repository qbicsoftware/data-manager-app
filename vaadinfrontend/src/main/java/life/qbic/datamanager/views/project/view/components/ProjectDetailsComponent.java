package life.qbic.datamanager.views.project.view.components;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.Objects;
import java.util.function.Consumer;
import life.qbic.datamanager.views.layouts.CardLayout;
import life.qbic.projectmanagement.application.PersonSearchService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.domain.project.*;
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

  private final TextField titleField;
  private final FormLayout formLayout;
  private final TextArea experimentalDesignField;
  private final TextArea projectObjective;
  private final ComboBox<PersonReference> projectManager;
  private final transient Handler handler;

  public ProjectDetailsComponent(@Autowired ProjectInformationService projectInformationService, @Autowired PersonSearchService personSearchService) {
    Objects.requireNonNull(projectInformationService);
    Objects.requireNonNull(personSearchService);

    titleField = new TextField();
    formLayout = new FormLayout();
    experimentalDesignField = new TextArea();
    projectObjective = new TextArea();

    this.projectManager = new ComboBox<>();
    this.projectManager.setPlaceholder("Select a Project Manager");

    projectManager.setItemLabelGenerator(item ->
        item.fullName() + ", " + item.getEmailAddress());
    projectManager.setRenderer(createRenderer());

    projectManager.getStyle().set("--vaadin-combo-box-overlay-width", "16em");
    projectManager.getStyle().set("--vaadin-combo-box-width", "16em");


    this.handler = new Handler(this, projectInformationService, personSearchService);

    initLayout();
    setComponentStyles();
  }

  private Renderer<PersonReference> createRenderer() {
    //todo move that to css stylings
    String tpl = "<div style=\"display: flex;\">" +
        "  <div>" +
        "    ${item.fullName}" +
        "    <div style=\"font-size: var(--lumo-font-size-s); color: var(--lumo-secondary-text-color);\">${item.email}</div>" +
        "  </div>" +
        "</div>";

    return LitRenderer.<PersonReference> of(tpl)
        .withProperty("fullName", PersonReference::fullName)
        .withProperty("email", PersonReference::getEmailAddress);
  }

  private void initLayout() {
    formLayout.addFormItem(titleField, "Project Title");
    formLayout.addFormItem(projectObjective, "Project Objective");
    formLayout.addFormItem(experimentalDesignField, "Experimental Design");
    formLayout.addFormItem(projectManager, "Project Manager");
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
    private final PersonSearchService personSearchService;

    private ProjectId selectedProject;

    public Handler(ProjectDetailsComponent component,
                   ProjectInformationService projectInformationService, PersonSearchService personSearchService) {
      this.component = component;

      this.projectInformationService = projectInformationService;
      this.personSearchService = personSearchService;

      setFieldsEditableOnlyOnFocus();
      attachSubmissionActionOnBlur();
      restrictInputLength();
      setUpPersonSearch(component.projectManager);
    }

    public void projectId(String projectId) {
      projectInformationService.find(ProjectId.parse(projectId)).ifPresentOrElse(
          this::loadProjectData,
          () -> component.titleField.setValue("Not found"));
    }

    private void restrictInputLength() {
      
      titleField.setMaxLength((int) ProjectTitle.maxLength());
      projectObjective.setMaxLength((int) ProjectObjective.maxLength());
      experimentalDesignField.setMaxLength(
          (int) ExperimentalDesignDescription.maxLength());

      titleField.setValueChangeMode(ValueChangeMode.EAGER);
      projectObjective.setValueChangeMode(ValueChangeMode.EAGER);
      experimentalDesignField.setValueChangeMode(ValueChangeMode.EAGER);

      addConsumedLengthHelper(titleField, titleField.getValue());
      addConsumedLengthHelper(projectObjective, projectObjective.getValue());
      addConsumedLengthHelper(experimentalDesignField, experimentalDesignField.getValue());

      titleField.addValueChangeListener(
          e -> addConsumedLengthHelper(e.getSource(), e.getValue()));
      projectObjective.addValueChangeListener(
          e -> addConsumedLengthHelper(e.getSource(), e.getValue()));
      experimentalDesignField.addValueChangeListener(
          e -> addConsumedLengthHelper(e.getSource(), e.getValue()));
    }

    private void addConsumedLengthHelper(TextArea textArea, String newValue) {
      int maxLength = textArea.getMaxLength();
      int consumedLength = newValue.length();
      textArea.setHelperText(consumedLength + "/" + maxLength);
    }

    private void addConsumedLengthHelper(TextField textField, String newValue) {
      int maxLength = textField.getMaxLength();
      int consumedLength = newValue.length();
      textField.setHelperText(consumedLength + "/" + maxLength);
    }

    private void loadProjectData(Project project) {
      this.selectedProject = project.getId();
      component.titleField.setValue(project.getProjectIntent().projectTitle().title());
      component.projectObjective.setValue(project.getProjectIntent().objective().value());

      project.getProjectIntent().experimentalDesign().ifPresentOrElse(
          experimentalDesignDescription -> component.experimentalDesignField.setValue(
              experimentalDesignDescription.value()),
          () -> component.experimentalDesignField.setPlaceholder("No description yet."));

      component.projectManager.setValue(project.getProjectManager());
    }

    private void setUpPersonSearch(ComboBox<PersonReference> comboBox) {
      comboBox.setItems(query ->
          personSearchService.find(query.getFilter().orElse(""), query.getOffset(),
                  query.getLimit())
              .stream());
    }

    private void setFieldsEditableOnlyOnFocus() {
      editableOnFocus(titleField);
      editableOnFocus(projectObjective);
      editableOnFocus(experimentalDesignField);
    }

    private void attachSubmissionActionOnBlur() {
      ProjectDetailsComponent.Handler.submitOnBlur(titleField, value ->
          projectInformationService.updateTitle(selectedProject.value(), value.trim()));
      ProjectDetailsComponent.Handler.submitOnBlur(projectObjective, value ->
          projectInformationService.stateObjective(selectedProject.value(), value.trim()));
      ProjectDetailsComponent.Handler.submitOnBlur(experimentalDesignField, value ->
          projectInformationService.describeExperimentalDesign(selectedProject.value(),
              value.trim()));
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
