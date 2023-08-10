package life.qbic.datamanager.views.projects.project.info;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.PersonSearchService;
import life.qbic.projectmanagement.domain.project.ExperimentalDesignDescription;
import life.qbic.projectmanagement.domain.project.PersonReference;
import life.qbic.projectmanagement.domain.project.ProjectObjective;
import life.qbic.projectmanagement.domain.project.ProjectTitle;

/**
 * <b>Define Project Component</b>
 *
 * <p>Component to define the minimum required project information</p>
 */
public class DefineProjectComponent extends Div {

  private static final Logger log = logger(DefineProjectComponent.class);
  @Serial
  private static final long serialVersionUID = 5220409505242578485L;
  private final TextField projectTitle = new TextField("Title");
  private final TextArea projectObjective = new TextArea("Objective");
  private final TextArea experimentalDesign = new TextArea(
      "Experimental Design Description");
  private final ComboBox<PersonReference> principalInvestigator = new ComboBox<>(
      "Principal Investigator");
  private final ComboBox<PersonReference> responsiblePerson = new ComboBox<>(
      "Project Responsible (optional)");
  private final ComboBox<PersonReference> projectManager = new ComboBox<>("Project Manager");
  private final List<Binder<?>> binders = new ArrayList<>();
  private final transient PersonSearchService personSearchService;

  /**
   * Creates a new empty DefineProjectComponent.
   * <p>
   * This component is intended to be used within the {@link ProjectInformationDialog} and
   * {@link life.qbic.datamanager.views.projects.create.AddProjectDialog} containing the validation
   * and vaadin field logic for the project information
   */
  public DefineProjectComponent(PersonSearchService personSearchService) {
    this.personSearchService = personSearchService;
    this.addClassName("define-project-content");
    initProjectsDefinitionLayout();
    configureComponent();
  }

  private void initProjectsDefinitionLayout() {
    initProjectInformationLayout();
    initProjectContactsLayout();
  }

  private void initProjectInformationLayout() {
    Div projectInformationContainer = new Div();
    projectInformationContainer.addClassName("information");
    projectInformationContainer.add(projectTitle, projectObjective, experimentalDesign);
    this.add(projectInformationContainer);
  }

  private void initProjectContactsLayout() {
    Div projectContactsContainer = new Div();
    projectContactsContainer.addClassName("contacts");
    Span projectContactsTitle = new Span("Project Contacts");
    projectContactsTitle.addClassName("title");
    Span projectContactsDescription = new Span("Important contact people of the project");
    responsiblePerson.setClearButtonVisible(true);
    experimentalDesign.setClearButtonVisible(true);
    projectContactsContainer.add(projectContactsTitle, projectContactsDescription,
        principalInvestigator,
        responsiblePerson, projectManager);
    this.add(projectContactsContainer);
  }

  private void configureComponent() {
    configureValidators();
    configurePersonSearch();
  }

  private void configurePersonSearch() {
    setUpPersonSearch(principalInvestigator);
    setUpPersonSearch(projectManager);
    setUpPersonSearch(responsiblePerson);
  }

  private void setUpPersonSearch(ComboBox<PersonReference> comboBox) {
    comboBox.setItems(
        query -> personSearchService.find(query.getFilter().orElse(""), query.getOffset(),
            query.getLimit()).stream());
    comboBox.setRenderer(
        new ComponentRenderer<>(personReference -> new Text(personReference.fullName())));
    comboBox.setItemLabelGenerator(
        (ItemLabelGenerator<PersonReference>) PersonReference::fullName);
  }

  /**
   * Sets the initial values for each field within this component, necessary if this content is used
   * for editing functionality
   *
   * @param projectTitle                  {@link ProjectTitle} of the project to be edited
   * @param projectObjective              {@link ProjectObjective} of the project to be edited
   * @param experimentalDesignDescription {@link ExperimentalDesignDescription} of the project to be
   *                                      edited
   * @param principalInvestigator         {@link PersonReference} of the principal investigator of
   *                                      the project to be edited
   * @param responsiblePerson             {@link PersonReference} of the responsible person to be
   *                                      contacted of the project to be edited
   * @param projectManager                {@link PersonReference} of the project manager of the
   *                                      project to be edited
   */
  public void setProjectInformation(ProjectTitle projectTitle,
      ProjectObjective projectObjective,
      ExperimentalDesignDescription experimentalDesignDescription,
      PersonReference principalInvestigator, PersonReference responsiblePerson,
      PersonReference projectManager) {
    this.projectTitle.setValue(projectTitle.title());
    this.projectObjective.setValue(projectObjective.value());
    this.experimentalDesign.setValue(experimentalDesignDescription.value());
    this.principalInvestigator.setValue(principalInvestigator);
    this.responsiblePerson.setValue(responsiblePerson);
    this.projectManager.setValue(projectManager);
  }

  private void configureValidators() {
    restrictInputLength();
    projectTitle.setRequired(true);
    projectObjective.setRequired(true);
    principalInvestigator.setRequired(true);
    projectManager.setRequired(true);
    Binder<Container<String>> binderTitle = new Binder<>();
    binderTitle.forField(projectTitle)
        .withValidator(value -> !value.isBlank(), "Please provide a title")
        .bind(Container::value, Container::setValue);
    Binder<Container<String>> binderObjective = new Binder<>();
    binderObjective.forField(projectObjective)
        .withValidator(value -> !value.isBlank(), "Please provide an " + "objective")
        .bind(Container::value, Container::setValue);
    Binder<Container<PersonReference>> binderPI = new Binder<>();
    binderPI.forField(principalInvestigator).asRequired("Please select at least one PI")
        .bind(Container::value, Container::setValue);
    Binder<Container<PersonReference>> binderPM = new Binder<>();
    binderPM.forField(projectManager).asRequired("Please select at least one PM")
        .bind(Container::value, Container::setValue);
    binders.addAll(List.of(binderTitle, binderObjective, binderPI, binderPM));
  }

  private void restrictInputLength() {
    projectTitle.setMaxLength((int) ProjectTitle.maxLength());
    projectObjective.setMaxLength((int) ProjectObjective.maxLength());
    experimentalDesign.setMaxLength(
        (int) ExperimentalDesignDescription.maxLength());

    projectTitle.setValueChangeMode(ValueChangeMode.EAGER);
    projectObjective.setValueChangeMode(ValueChangeMode.EAGER);
    experimentalDesign.setValueChangeMode(
        ValueChangeMode.EAGER);

    addConsumedLengthHelper(projectTitle, projectTitle.getValue());
    addConsumedLengthHelper(projectObjective, projectObjective.getValue());
    addConsumedLengthHelper(experimentalDesign,
        experimentalDesign.getValue());

    projectTitle.addValueChangeListener(e -> addConsumedLengthHelper(e.getSource(), e.getValue()));
    projectObjective.addValueChangeListener(
        e -> addConsumedLengthHelper(e.getSource(), e.getValue()));
    experimentalDesign.addValueChangeListener(
        e -> addConsumedLengthHelper(e.getSource(), e.getValue()));
  }

  private void addConsumedLengthHelper(TextField textField, String newValue) {
    int maxLength = textField.getMaxLength();
    int consumedLength = newValue.length();
    textField.setHelperText(consumedLength + "/" + maxLength);
  }

  private void addConsumedLengthHelper(TextArea textArea, String newValue) {
    int maxLength = textArea.getMaxLength();
    int consumedLength = newValue.length();
    textArea.setHelperText(consumedLength + "/" + maxLength);
  }

  /**
   * Resets the values and validity of all components that implement value storing and validity
   * interfaces
   */
  private void reset() {
    resetChildValues();
    resetChildValidation();
  }

  private void resetChildValues() {
    getChildren().filter(comp -> comp instanceof HasValue<?, ?>)
        .forEach(comp -> ((HasValue<?, ?>) comp).clear());
  }

  private void resetChildValidation() {
    getChildren().filter(comp -> comp instanceof HasValidation)
        .forEach(comp -> ((HasValidation) comp).setInvalid(false));
  }

  /**
   * Checks if any fields within this component contains an invalid value
   *
   * @return boolean indicating if any field of this component contains an invalid value
   */

  public boolean isInputValid() {
    return validateInput();
  }

  protected boolean validateInput() {
    binders.forEach(Binder::validate);
    return binders.stream().allMatch(Binder::isValid);
  }

  /**
   * Provides the content set in the fields of this component
   *
   * @return {@link ProjectInformationContent} providing the information filled by the user for each
   * field
   */

  public ProjectInformationContent getProjectInformationContent() {
    return new ProjectInformationContent(projectTitle.getValue(), projectObjective.getValue(),
        experimentalDesign.getValue(), principalInvestigator.getValue(),
        responsiblePerson.getValue(),
        projectManager.getValue());
  }

  static class Container<T> {

    private T value;

    T value() {
      return this.value;
    }

    void setValue(T newValue) {
      this.value = newValue;
    }

  }

}
