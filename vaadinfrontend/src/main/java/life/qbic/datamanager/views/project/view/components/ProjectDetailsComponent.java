package life.qbic.datamanager.views.project.view.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.Objects;
import java.util.function.Consumer;
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
  private TextField titleComponent;
  private TextArea projectObjectiveComponent;
  private TextArea experimentalDesignComponent;
  private ComboBox<PersonReference> projectManagerComponent;
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

  private void initFormLayout() {
    titleComponent = new TextField();
    projectObjectiveComponent = new TextArea();
    experimentalDesignComponent = new TextArea();
    projectManagerComponent = initProjectManagerComboBox();
    ToggleDisplayEditComponent<Span, TextField, String> titleToggleComponent = new ToggleDisplayEditComponent<>(
        titleComponent, Span::new,
        createPlaceHolderSpan("Project Title"));
    ToggleDisplayEditComponent<Span, TextArea, String> projectObjectiveToggleComponent = new ToggleDisplayEditComponent<>(
        projectObjectiveComponent, Span::new,
        createPlaceHolderSpan("Project Objective"));
    ToggleDisplayEditComponent<Span, TextArea, String> experimentalDesignToggleComponent = new ToggleDisplayEditComponent<>(
        experimentalDesignComponent, Span::new,
        createPlaceHolderSpan("Experimental Design"));
    ToggleDisplayEditComponent<Component, ComboBox<PersonReference>, PersonReference> projectManagerToggleComponent = new ToggleDisplayEditComponent<>(
        projectManagerComponent, ContactElement::from,
        createPlaceHolderSpan("Project Manager"));
    formLayout.addFormItem(titleToggleComponent, "Project Title");
    formLayout.addFormItem(projectObjectiveToggleComponent, "Project Objective");
    formLayout.addFormItem(experimentalDesignToggleComponent, "Experimental Design");
    formLayout.addFormItem(projectManagerToggleComponent, "Project Manager");
    // set form layout to only have one column (for any width)
    formLayout.setResponsiveSteps(new ResponsiveStep("0", 1));
    titleToggleComponent.setRequiredIndicatorVisible(true);
    projectObjectiveToggleComponent.setRequiredIndicatorVisible(true);
    experimentalDesignToggleComponent.setRequiredIndicatorVisible(true);
    projectManagerToggleComponent.setRequiredIndicatorVisible(true);
    getContent().addFields(formLayout);
    getContent().addTitle(TITLE);
  }

  private Span createPlaceHolderSpan(String projectDetail) {
    Span placeholderSpan = new Span();
    Icon placeholderIcon = new Icon(VaadinIcon.PLUS_CIRCLE);
    Text placeholderText = new Text("Add %s".formatted(projectDetail));
    placeholderSpan.add(placeholderIcon, placeholderText);
    placeholderSpan.getElement().getThemeList().add("badge");
    placeholderIcon.getElement().getStyle().set("padding", "var(--lumo-space-xs");
    placeholderIcon.getElement().getStyle().set("margin-right", "var(--lumo-space-xs");
    return placeholderSpan;
  }

  private void setComponentStyles() {
    titleComponent.setSizeFull();
    projectObjectiveComponent.setWidthFull();
    experimentalDesignComponent.setWidthFull();
    formLayout.setClassName("create-project-form");
    projectManagerComponent.getStyle().set("--vaadin-combo-box-overlay-width", "16em");
    projectManagerComponent.getStyle().set("--vaadin-combo-box-width", "16em");
    titleComponent.setRequired(true);
    projectObjectiveComponent.setRequired(true);
    projectManagerComponent.setRequired(true);
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
      setUpPersonSearch(projectManagerComponent);
    }

    public void projectId(String projectId) {
      projectInformationService.find(ProjectId.parse(projectId)).ifPresentOrElse(
          this::loadProjectData,
          () -> titleComponent.setValue("Not found"));
    }

    private void restrictInputLength() {

      titleComponent.setMaxLength((int) ProjectTitle.maxLength());
      projectObjectiveComponent.setMaxLength((int) ProjectObjective.maxLength());
      experimentalDesignComponent.setMaxLength(
          (int) ExperimentalDesignDescription.maxLength());

      titleComponent.setValueChangeMode(ValueChangeMode.EAGER);
      projectObjectiveComponent.setValueChangeMode(ValueChangeMode.EAGER);
      experimentalDesignComponent.setValueChangeMode(ValueChangeMode.EAGER);

      addConsumedLengthHelper(titleComponent, titleComponent.getValue());
      addConsumedLengthHelper(projectObjectiveComponent,
          projectObjectiveComponent.getValue());
      addConsumedLengthHelper(experimentalDesignComponent,
          experimentalDesignComponent.getValue());

      titleComponent.addValueChangeListener(
          e -> addConsumedLengthHelper(e.getSource(), e.getValue()));
      projectObjectiveComponent.addValueChangeListener(
          e -> addConsumedLengthHelper(e.getSource(), e.getValue()));
      experimentalDesignComponent.addValueChangeListener(
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
      titleComponent.setValue(project.getProjectIntent().projectTitle().title());
      projectObjectiveComponent.setValue(project.getProjectIntent().objective().value());
      projectManagerComponent.setValue(project.getProjectManager());
      project.getProjectIntent().experimentalDesign().ifPresentOrElse(
          experimentalDesignDescription -> experimentalDesignComponent.setValue(
              experimentalDesignDescription.value()), experimentalDesignComponent::clear);
    }

    private void setUpPersonSearch(ComboBox<PersonReference> comboBox) {
      comboBox.setItems(
          query -> personSearchService.find(query.getFilter().orElse(""), query.getOffset(),
                  query.getLimit())
              .stream());
    }

    private void attachSubmissionActionOnBlur() {
      ProjectDetailsComponent.Handler.submitOnBlur(titleComponent, value ->
          projectInformationService.updateTitle(selectedProject.value(), value.trim()));
      ProjectDetailsComponent.Handler.submitOnBlur(projectObjectiveComponent, value ->
          projectInformationService.stateObjective(selectedProject.value(), value.trim()));
      ProjectDetailsComponent.Handler.submitOnBlur(experimentalDesignComponent, value ->
          projectInformationService.describeExperimentalDesign(selectedProject.value(),
              value.trim()));
      ProjectDetailsComponent.Handler.submitOnBlur(projectManagerComponent,
          value -> projectInformationService.manageProject(selectedProject.value(), value));
    }

    private static <V, T extends HasValue<?, V> & Focusable<?>> void submitOnBlur(T element,
        Consumer<V> submitAction) {
      element.addBlurListener(it -> submitAction.accept(element.getValue()));
    }

  }
}
