package life.qbic.datamanager.views.project.view.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
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
  private ToggleDisplayEditComponent<Span, TextField, String> titleToggleComponent;
  private ToggleDisplayEditComponent<Span, TextArea, String> projectObjectiveToggleComponent;
  private ToggleDisplayEditComponent<Span, TextArea, String> experimentalDesignToggleComponent;
  private ToggleDisplayEditComponent<Component, ComboBox<PersonReference>, PersonReference> projectManagerToggleComponent;
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
    initFormFields();
    formLayout.addFormItem(titleToggleComponent, "Project Title");
    formLayout.addFormItem(projectObjectiveToggleComponent, "Project Objective");
    formLayout.addFormItem(experimentalDesignToggleComponent, "Experimental Design");
    formLayout.addFormItem(projectManagerToggleComponent, "Project Manager");
    // set form layout to only have one column (for any width)
    formLayout.setResponsiveSteps(new ResponsiveStep("0", 1));
    getContent().addFields(formLayout);
    getContent().addTitle(TITLE);
  }

  private void initFormFields() {
    titleToggleComponent = new ToggleDisplayEditComponent<>(Span::new, new TextField(),
        createPlaceHolderSpan("Project Title"));
    projectObjectiveToggleComponent = new ToggleDisplayEditComponent<>(Span::new, new TextArea(),
        createPlaceHolderSpan("Project Objective"));
    experimentalDesignToggleComponent = new ToggleDisplayEditComponent<>(Span::new, new TextArea(),
        createPlaceHolderSpan("Experimental Design"));
    projectManagerToggleComponent = new ToggleDisplayEditComponent<>(ContactElement::from,
        initProjectManagerComboBox(),
        createPlaceHolderSpan("Project Manager"));
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
    titleToggleComponent.getInputComponent().setSizeFull();
    projectObjectiveToggleComponent.getInputComponent().setWidthFull();
    experimentalDesignToggleComponent.getInputComponent().setWidthFull();
    titleToggleComponent.setWidthFull();
    projectObjectiveToggleComponent.setWidthFull();
    experimentalDesignToggleComponent.setWidthFull();
    formLayout.setClassName("create-project-form");
    projectManagerToggleComponent.getInputComponent().getStyle()
        .set("--vaadin-combo-box-overlay-width", "16em");
    projectManagerToggleComponent.getInputComponent().getStyle()
        .set("--vaadin-combo-box-width", "16em");
    titleToggleComponent.getInputComponent().setRequired(true);
    projectObjectiveToggleComponent.getInputComponent().setRequired(true);
    projectManagerToggleComponent.getInputComponent().setRequired(true);
    titleToggleComponent.setRequiredIndicatorVisible(true);
    projectObjectiveToggleComponent.setRequiredIndicatorVisible(true);
    projectManagerToggleComponent.setRequiredIndicatorVisible(true);
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

      attachSubmissionActionOnValueChange();
      restrictInputLength();
      setUpPersonSearch(projectManagerToggleComponent.getInputComponent());
    }

    public void projectId(String projectId) {
      projectInformationService.find(ProjectId.parse(projectId)).ifPresentOrElse(
          this::loadProjectData,
          () -> titleToggleComponent.setValue("Not found"));
    }

    private void restrictInputLength() {

      titleToggleComponent.getInputComponent().setMaxLength((int) ProjectTitle.maxLength());
      projectObjectiveToggleComponent.getInputComponent()
          .setMaxLength((int) ProjectObjective.maxLength());
      experimentalDesignToggleComponent.getInputComponent().setMaxLength(
          (int) ExperimentalDesignDescription.maxLength());

      titleToggleComponent.getInputComponent().setValueChangeMode(ValueChangeMode.EAGER);
      projectObjectiveToggleComponent.getInputComponent().setValueChangeMode(ValueChangeMode.EAGER);
      experimentalDesignToggleComponent.getInputComponent()
          .setValueChangeMode(ValueChangeMode.EAGER);

      addConsumedLengthHelper(titleToggleComponent.getInputComponent(),
          titleToggleComponent.getValue());
      addConsumedLengthHelper(projectObjectiveToggleComponent.getInputComponent(),
          projectObjectiveToggleComponent.getValue());
      addConsumedLengthHelper(experimentalDesignToggleComponent.getInputComponent(),
          experimentalDesignToggleComponent.getValue());

      titleToggleComponent.getInputComponent().addValueChangeListener(
          e -> addConsumedLengthHelper(e.getSource(), e.getValue()));
      projectObjectiveToggleComponent.getInputComponent().addValueChangeListener(
          e -> addConsumedLengthHelper(e.getSource(), e.getValue()));
      experimentalDesignToggleComponent.getInputComponent().addValueChangeListener(
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
      titleToggleComponent.setValue(project.getProjectIntent().projectTitle().title());
      projectObjectiveToggleComponent.setValue(project.getProjectIntent().objective().value());
      projectManagerToggleComponent.setValue(project.getProjectManager());
      project.getProjectIntent().experimentalDesign().ifPresentOrElse(
          experimentalDesignDescription -> experimentalDesignToggleComponent.setValue(
              experimentalDesignDescription.value()), experimentalDesignToggleComponent::clear);
    }

    private void setUpPersonSearch(ComboBox<PersonReference> comboBox) {
      comboBox.setItems(
          query -> personSearchService.find(query.getFilter().orElse(""), query.getOffset(),
                  query.getLimit())
              .stream());
    }

    private void attachSubmissionActionOnValueChange() {
      ProjectDetailsComponent.Handler.submitOnValueChange(titleToggleComponent,
          value ->
              projectInformationService.updateTitle(selectedProject.value(), value.trim()));

      ProjectDetailsComponent.Handler.submitOnValueChange(projectObjectiveToggleComponent,
          value ->
              projectInformationService.stateObjective(selectedProject.value(), value.trim()));

      ProjectDetailsComponent.Handler.submitOnValueChange(experimentalDesignToggleComponent,
          value ->
              projectInformationService.describeExperimentalDesign(selectedProject.value(),
                  value.trim()));

      ProjectDetailsComponent.Handler.submitOnValueChange(projectManagerToggleComponent,
          value ->
              projectInformationService.manageProject(selectedProject.value(), value));
    }

    private static <V, T extends HasValue<?, V>> void submitOnValueChange(T element,
        Consumer<V> submitAction) {
      element.addValueChangeListener(it -> submitAction.accept(element.getValue()));
    }

  }
}
