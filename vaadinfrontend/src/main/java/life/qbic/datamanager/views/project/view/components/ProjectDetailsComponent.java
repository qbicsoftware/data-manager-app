package life.qbic.datamanager.views.project.view.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.Objects;
import java.util.function.Consumer;
import life.qbic.datamanager.views.ContactElement;
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

  private final TextField titleField;
  private final FormLayout formLayout;
  private final TextArea projectObjective;
  private final TextArea experimentalDesignField;

  private final HorizontalLayout projectManagerLayout;
  private final ContactElement projectManagerContact;
  private final ComboBox<PersonReference> projectManagerComboBox;

  private final transient Handler handler;

  public ProjectDetailsComponent(@Autowired ProjectInformationService projectInformationService, @Autowired PersonSearchService personSearchService) {
    Objects.requireNonNull(projectInformationService);
    Objects.requireNonNull(personSearchService);

    titleField = new TextField();
    formLayout = new FormLayout();
    experimentalDesignField = new TextArea();
    projectObjective = new TextArea();

    projectManagerComboBox = initProjectManagerComboBox();
    projectManagerContact = new ContactElement();

    projectManagerLayout = new HorizontalLayout();
    projectManagerLayout.add(projectManagerComboBox);
    projectManagerLayout.add(projectManagerContact);

    initLayout();
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

  private void initLayout() {
    formLayout.addFormItem(titleField, "Project Title");
    formLayout.addFormItem(projectObjective, "Project Objective");
    formLayout.addFormItem(experimentalDesignField, "Experimental Design");
    formLayout.addFormItem(projectManagerLayout, "Project Manager");
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
    projectManagerComboBox.getStyle().set("--vaadin-combo-box-overlay-width", "16em");
    projectManagerComboBox.getStyle().set("--vaadin-combo-box-width", "16em");
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

      setFieldsEditableOnlyOnFocus();
      attachSubmissionActionOnBlur();
      restrictInputLength();
      setUpPersonSearch(projectManagerComboBox);
      switchModeForProjectManagerField();
    }

    private void switchModeForProjectManagerField() {
      // click focuses the field and hide component
      projectManagerLayout.addClickListener(click -> {
        projectManagerContact.setVisible(false);
        projectManagerComboBox.setVisible(true);
        projectManagerComboBox.focus();
      });
      // value change updates the component and blurs the field
      projectManagerComboBox.addValueChangeListener(it -> {
        PersonReference reference = projectManagerComboBox.getValue();
        projectManagerContact.setContent(reference.fullName(), reference.getEmailAddress());
        projectManagerComboBox.blur();
      });
      // blur triggers switch back to component
      projectManagerComboBox.addBlurListener(it -> {
        projectManagerComboBox.setVisible(false);
        projectManagerContact.setVisible(true);
      });
    }

    public void projectId(String projectId) {
      projectInformationService.find(ProjectId.parse(projectId)).ifPresentOrElse(
          this::loadProjectData,
          () -> titleField.setValue("Not found"));
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
      titleField.setValue(project.getProjectIntent().projectTitle().title());
      projectObjective.setValue(project.getProjectIntent().objective().value());
      projectManagerContact.setContent(project.getProjectManager().fullName(),
          project.getProjectManager().getEmailAddress());
      projectManagerComboBox.setValue(project.getProjectManager());

      project.getProjectIntent().experimentalDesign().ifPresentOrElse(
          experimentalDesignDescription -> experimentalDesignField.setValue(
              experimentalDesignDescription.value()),
          () -> experimentalDesignField.setValue(""));
    }

    private void setUpPersonSearch(ComboBox<PersonReference> comboBox) {
      comboBox.setItems(
          query -> personSearchService.find(query.getFilter().orElse(""), query.getOffset(),
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
      ProjectDetailsComponent.Handler.submitOnBlur(projectManagerComboBox,
          value -> projectInformationService.manageProject(selectedProject.value(), value));
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
