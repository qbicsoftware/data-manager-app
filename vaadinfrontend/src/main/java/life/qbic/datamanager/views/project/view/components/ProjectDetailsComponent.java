package life.qbic.datamanager.views.project.view.components;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import life.qbic.datamanager.views.general.ContactElement;
import life.qbic.datamanager.views.general.ToggleDisplayEditComponent;
import life.qbic.datamanager.views.layouts.CardLayout;
import life.qbic.projectmanagement.application.PersonSearchService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.domain.project.ExperimentalDesignDescription;
import life.qbic.projectmanagement.domain.project.PersonReference;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.ProjectObjective;
import life.qbic.projectmanagement.domain.project.ProjectTitle;
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
  private final FormLayout formLayout;
  private ToggleDisplayEditComponent<?, ?, ?> titleComponent;
  private ToggleDisplayEditComponent<?, ?, ?> projectObjectiveComponent;
  private ToggleDisplayEditComponent<?, ?, ?> experimentalDesignComponent;
  private ToggleDisplayEditComponent<?, ?, ?> projectManagerComponent;
  private TextField titleEditComponent;
  private TextArea projectObjectiveEditComponent;
  private TextArea experimentalDesignEditComponent;
  private ComboBox<PersonReference> projectManagerEditComponent;
  private Span titleDisplayComponent;
  private Span projectObjectiveDisplayComponent;
  private Span experimentalDesignDisplayComponent;
  private ContactElement projectManagerDisplayComponent;
  private final transient Handler handler;

  public ProjectDetailsComponent(@Autowired ProjectInformationService projectInformationService,
      @Autowired PersonSearchService personSearchService) {
    Objects.requireNonNull(projectInformationService);
    Objects.requireNonNull(personSearchService);
    formLayout = new FormLayout();
    initFormLayout();
    setComponentStyles();
    this.handler = new Handler(projectInformationService, personSearchService);
  }

  private ComboBox<PersonReference> initProjectManagerComboBox() {
    ComboBox<PersonReference> comboBox = new ComboBox<>();
    comboBox.setItemLabelGenerator(PersonReference::fullName);
    comboBox.setRenderer(new ComponentRenderer<>(ContactElement::from));
    comboBox.setPlaceholder("Select a Project Manager");
    comboBox.setVisible(false);
    return comboBox;
  }

  private void initFormFieldComponents() {
    initDisplayComponents();
    initEditComponents();
    titleComponent = new ToggleDisplayEditComponent<>(titleEditComponent,
        createInputFunction(titleDisplayComponent), titleDisplayComponent);
    projectObjectiveComponent = new ToggleDisplayEditComponent<>(projectObjectiveEditComponent,
        createInputFunction(projectObjectiveDisplayComponent), projectObjectiveDisplayComponent);
    experimentalDesignComponent = new ToggleDisplayEditComponent<>(experimentalDesignEditComponent,
        createInputFunction(experimentalDesignDisplayComponent),
        experimentalDesignDisplayComponent);
    projectManagerComponent = new ToggleDisplayEditComponent<>(projectManagerEditComponent,
        createInputFunction(projectManagerDisplayComponent), projectManagerDisplayComponent);
  }

  private Function<String, Span> createInputFunction(Span span) {
    return string -> {
      span.setText(string);
      return span;
    };
  }

  private Function<PersonReference, ContactElement> createInputFunction(
      ContactElement contactElement) {
    return personReference -> {
      contactElement.setContent(personReference.fullName(), personReference.getEmailAddress());
      return contactElement;
    };
  }

  private void initDisplayComponents() {
    titleDisplayComponent = new Span();
    projectObjectiveDisplayComponent = new Span();
    experimentalDesignDisplayComponent = new Span();
    projectManagerDisplayComponent = new ContactElement();
  }

  private void initEditComponents() {
    titleEditComponent = new TextField();
    projectObjectiveEditComponent = new TextArea();
    experimentalDesignEditComponent = new TextArea();
    projectManagerEditComponent = initProjectManagerComboBox();
  }

  private void initFormLayout() {
    initFormFieldComponents();
    formLayout.addFormItem(titleComponent, "Project Title");
    formLayout.addFormItem(projectObjectiveComponent, "Project Objective");
    formLayout.addFormItem(experimentalDesignComponent, "Experimental Design");
    formLayout.addFormItem(projectManagerComponent, "Project Manager");
    // set form layout to only have one column (for any width)
    formLayout.setResponsiveSteps(new ResponsiveStep("0", 1));
    getContent().addFields(formLayout);
    getContent().addTitle(TITLE);
  }

  private void setComponentStyles() {
    titleEditComponent.setSizeFull();
    projectObjectiveEditComponent.setWidthFull();
    experimentalDesignEditComponent.setWidthFull();
    formLayout.setClassName("create-project-form");
    projectManagerEditComponent.getStyle().set("--vaadin-combo-box-overlay-width", "16em");
    projectManagerEditComponent.getStyle().set("--vaadin-combo-box-width", "16em");
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

    private final ProjectInformationService projectInformationService;
    private final PersonSearchService personSearchService;

    private ProjectId selectedProject;

    public Handler(ProjectInformationService projectInformationService,
        PersonSearchService personSearchService) {

      this.projectInformationService = projectInformationService;
      this.personSearchService = personSearchService;

      attachSubmissionActionOnBlur();
      restrictInputLength();
      setUpPersonSearch(projectManagerEditComponent);
    }

    public void projectId(String projectId) {
      projectInformationService.find(ProjectId.parse(projectId)).ifPresentOrElse(
          this::loadProjectData,
          () -> titleEditComponent.setValue("Not found"));
    }

    private void restrictInputLength() {

      titleEditComponent.setMaxLength((int) ProjectTitle.maxLength());
      projectObjectiveEditComponent.setMaxLength((int) ProjectObjective.maxLength());
      experimentalDesignEditComponent.setMaxLength(
          (int) ExperimentalDesignDescription.maxLength());

      titleEditComponent.setValueChangeMode(ValueChangeMode.EAGER);
      projectObjectiveEditComponent.setValueChangeMode(ValueChangeMode.EAGER);
      experimentalDesignEditComponent.setValueChangeMode(ValueChangeMode.EAGER);

      addConsumedLengthHelper(titleEditComponent, titleEditComponent.getValue());
      addConsumedLengthHelper(projectObjectiveEditComponent,
          projectObjectiveEditComponent.getValue());
      addConsumedLengthHelper(experimentalDesignEditComponent,
          experimentalDesignEditComponent.getValue());

      titleEditComponent.addValueChangeListener(
          e -> addConsumedLengthHelper(e.getSource(), e.getValue()));
      projectObjectiveEditComponent.addValueChangeListener(
          e -> addConsumedLengthHelper(e.getSource(), e.getValue()));
      experimentalDesignEditComponent.addValueChangeListener(
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
      titleEditComponent.setValue(project.getProjectIntent().projectTitle().title());
      projectObjectiveEditComponent.setValue(project.getProjectIntent().objective().value());
      projectManagerEditComponent.setValue(project.getProjectManager());
      project.getProjectIntent().experimentalDesign().ifPresentOrElse(
          experimentalDesignDescription -> experimentalDesignEditComponent.setValue(
              experimentalDesignDescription.value()),
          () -> experimentalDesignEditComponent.setValue(""));
    }

    private void setUpPersonSearch(ComboBox<PersonReference> comboBox) {
      comboBox.setItems(
          query -> personSearchService.find(query.getFilter().orElse(""), query.getOffset(),
                  query.getLimit())
              .stream());
    }

    private void attachSubmissionActionOnBlur() {
      ProjectDetailsComponent.Handler.submitOnBlur(titleEditComponent, value ->
          projectInformationService.updateTitle(selectedProject.value(), value.trim()));
      ProjectDetailsComponent.Handler.submitOnBlur(projectObjectiveEditComponent, value ->
          projectInformationService.stateObjective(selectedProject.value(), value.trim()));
      ProjectDetailsComponent.Handler.submitOnBlur(experimentalDesignEditComponent, value ->
          projectInformationService.describeExperimentalDesign(selectedProject.value(),
              value.trim()));
      ProjectDetailsComponent.Handler.submitOnBlur(projectManagerEditComponent,
          value -> projectInformationService.manageProject(selectedProject.value(), value));
    }
    private static <V, T extends HasValue<?, V> & Focusable<?>> void submitOnBlur(T element,
        Consumer<V> submitAction) {
      element.addBlurListener(it -> submitAction.accept(element.getValue()));
    }

  }
}
