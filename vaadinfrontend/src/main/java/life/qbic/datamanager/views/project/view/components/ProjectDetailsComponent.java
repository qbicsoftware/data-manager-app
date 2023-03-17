package life.qbic.datamanager.views.project.view.components;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoUtility.TextColor;
import java.io.Serial;
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Consumer;
import javax.annotation.security.PermitAll;
import life.qbic.datamanager.views.general.ContactElement;
import life.qbic.datamanager.views.general.ToggleDisplayEditComponent;
import life.qbic.datamanager.views.layouts.CardLayout;
import life.qbic.datamanager.views.project.view.ProjectViewPage;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.application.ExperimentalDesignSearchService;
import life.qbic.projectmanagement.application.PersonSearchService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.domain.project.ExperimentalDesignDescription;
import life.qbic.projectmanagement.domain.project.PersonReference;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.ProjectObjective;
import life.qbic.projectmanagement.domain.project.ProjectTitle;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Species;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Specimen;
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
@Route(value = "projects/:projectId?/info", layout = ProjectViewPage.class)
@PermitAll
public class ProjectDetailsComponent extends Composite<CardLayout> {

  private static final Logger log = logger(ProjectDetailsComponent.class);

  @Serial
  private static final long serialVersionUID = -5781313306040217724L;
  private static final String TITLE = "Project Information";
  private final FormLayout formLayout;
  private ToggleDisplayEditComponent<Span, TextField, String> titleToggleComponent;
  private ToggleDisplayEditComponent<Span, TextArea, String> projectObjectiveToggleComponent;
  private ToggleDisplayEditComponent<Span, TextArea, String> experimentalDesignToggleComponent;
  private ToggleDisplayEditComponent<Component, ComboBox<PersonReference>, PersonReference> projectManagerToggleComponent;
  private ToggleDisplayEditComponent<Component, ComboBox<PersonReference>, PersonReference> principalInvestigatorToggleComponent;
  private MultiSelectComboBox<Species> speciesMultiSelectComboBox;
  private MultiSelectComboBox<Specimen> specimenMultiSelectComboBox;
  private MultiSelectComboBox<Analyte> analyteMultiSelectComboBox;
  private ToggleDisplayEditComponent<Component, ComboBox<PersonReference>, PersonReference> responsiblePersonToggleComponent;
  private final transient Handler handler;

  public ProjectDetailsComponent(@Autowired ProjectInformationService projectInformationService,
      @Autowired PersonSearchService personSearchService,
      @Autowired ExperimentalDesignSearchService experimentalDesignSearchService, @Autowired
  ExperimentInformationService experimentInformationService) {
    Objects.requireNonNull(projectInformationService);
    Objects.requireNonNull(personSearchService);
    Objects.requireNonNull(experimentalDesignSearchService);
    Objects.requireNonNull(experimentInformationService);
    formLayout = new FormLayout();
    initFormLayout();
    setComponentStyles();
    this.handler = new Handler(projectInformationService, personSearchService,
        experimentalDesignSearchService, experimentInformationService);
  }

  private ComboBox<PersonReference> initPersonReferenceCombobox(String personReferenceType) {
    ComboBox<PersonReference> comboBox = new ComboBox<>();
    comboBox.setItemLabelGenerator(PersonReference::fullName);
    comboBox.setRenderer(new ComponentRenderer<>(ContactElement::from));
    comboBox.setPlaceholder(String.format("Select a %s", personReferenceType));
    comboBox.setVisible(false);
    return comboBox;
  }

  private void initFormLayout() {
    initFormFields();
    formLayout.addFormItem(titleToggleComponent, "Project Title");
    formLayout.addFormItem(projectObjectiveToggleComponent, "Project Objective");
    formLayout.addFormItem(experimentalDesignToggleComponent, "Experimental Design");
    formLayout.addFormItem(speciesMultiSelectComboBox, "Species");
    formLayout.addFormItem(specimenMultiSelectComboBox, "Specimen");
    formLayout.addFormItem(analyteMultiSelectComboBox, "Analyte");
    formLayout.addFormItem(principalInvestigatorToggleComponent, "Principal Investigator");
    formLayout.addFormItem(responsiblePersonToggleComponent, "Responsible Person");
    formLayout.addFormItem(projectManagerToggleComponent, "Project Manager");
    // set form layout to only have one column (for any width)
    formLayout.setResponsiveSteps(new ResponsiveStep("0", 1));
    getContent().addFields(formLayout);
    getContent().addTitle(TITLE);
  }

  private void initFormFields() {
    titleToggleComponent = new ToggleDisplayEditComponent<>(Span::new, new TextField(),
        createPlaceHolderSpan());
    projectObjectiveToggleComponent = new ToggleDisplayEditComponent<>(Span::new, new TextArea(),
        createPlaceHolderSpan());
    experimentalDesignToggleComponent = new ToggleDisplayEditComponent<>(Span::new, new TextArea(),
        createPlaceHolderSpan());
    speciesMultiSelectComboBox = new MultiSelectComboBox<>();
    speciesMultiSelectComboBox.setClearButtonVisible(false);
    specimenMultiSelectComboBox = new MultiSelectComboBox<>();
    specimenMultiSelectComboBox.setClearButtonVisible(false);
    analyteMultiSelectComboBox = new MultiSelectComboBox<>();
    analyteMultiSelectComboBox.setClearButtonVisible(false);

    principalInvestigatorToggleComponent = new ToggleDisplayEditComponent<>(ContactElement::from,
        initPersonReferenceCombobox("Principal Investigator"),
        createPlaceHolderSpan());
    responsiblePersonToggleComponent = new ToggleDisplayEditComponent<>(ContactElement::from,
        initPersonReferenceCombobox("Responsible Person"),
        createPlaceHolderSpan());
    projectManagerToggleComponent = new ToggleDisplayEditComponent<>(ContactElement::from,
        initPersonReferenceCombobox("Project Manager"),
        createPlaceHolderSpan());

  }

  private Span createPlaceHolderSpan() {
    Span placeholderSpan = new Span("None");
    placeholderSpan.addClassName(TextColor.SECONDARY);
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
    principalInvestigatorToggleComponent.getInputComponent().getStyle()
        .set("--vaadin-combo-box-overlay-width", "16em");
    responsiblePersonToggleComponent.getInputComponent().getStyle()
        .set("--vaadin-combo-box-width", "16em");
    //Workaround since combobox does not allow empty selection https://github.com/vaadin/flow-components/issues/1998
    responsiblePersonToggleComponent.getInputComponent().setClearButtonVisible(true);
    titleToggleComponent.getInputComponent().setRequired(true);
    projectObjectiveToggleComponent.getInputComponent().setRequired(true);
    experimentalDesignToggleComponent.getInputComponent().setRequired(true);
    projectManagerToggleComponent.getInputComponent().setRequired(true);
    principalInvestigatorToggleComponent.getInputComponent().setRequired(true);
    titleToggleComponent.setRequiredIndicatorVisible(true);
    projectObjectiveToggleComponent.setRequiredIndicatorVisible(true);
    experimentalDesignToggleComponent.setRequiredIndicatorVisible(true);
    projectManagerToggleComponent.setRequiredIndicatorVisible(true);
    principalInvestigatorToggleComponent.setRequiredIndicatorVisible(true);
    speciesMultiSelectComboBox.setWidth(50, Unit.VW);
    specimenMultiSelectComboBox.setWidth(50, Unit.VW);
    analyteMultiSelectComboBox.setWidth(50, Unit.VW);
    formLayout.setClassName("create-project-form");
    speciesMultiSelectComboBox.addClassName("chip-badge");
    specimenMultiSelectComboBox.addClassName("chip-badge");
    analyteMultiSelectComboBox.addClassName("chip-badge");
  }

  public void projectId(String projectId) {
    handler.setProjectId(projectId);
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
    private final ExperimentalDesignSearchService experimentalDesignSearchService;
    private final ExperimentInformationService experimentInformationService;
    private ProjectId selectedProject;
    private ExperimentId activeExperimentId;

    public Handler(ProjectInformationService projectInformationService,
        PersonSearchService personSearchService,
        ExperimentalDesignSearchService experimentalDesignSearchService,
        ExperimentInformationService experimentInformationService) {

      this.projectInformationService = projectInformationService;
      this.personSearchService = personSearchService;
      this.experimentalDesignSearchService = experimentalDesignSearchService;
      this.experimentInformationService = experimentInformationService;

      attachSubmissionActionOnValueChange();
      restrictInputLength();
      setUpPersonSearch(projectManagerToggleComponent.getInputComponent());
      setUpPersonSearch(principalInvestigatorToggleComponent.getInputComponent());
      setUpPersonSearch(responsiblePersonToggleComponent.getInputComponent());
      setupExperimentalDesignSearch();
    }

    public void setProjectId(String projectId) {
      projectInformationService.find(projectId).ifPresentOrElse(this::loadProjectData,
          this::emptyAction);
    }

    //ToDo what should be done if projectID could not be retrieved
    private void emptyAction() {
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
      experimentalDesignToggleComponent.setValue(
          project.getProjectIntent().experimentalDesign().value());
      projectManagerToggleComponent.setValue(project.getProjectManager());
      principalInvestigatorToggleComponent.setValue(project.getPrincipalInvestigator());
      responsiblePersonToggleComponent.setValue(project.getResponsiblePerson());
      activeExperimentId = project.activeExperiment();
      analyteMultiSelectComboBox.setValue(
          experimentInformationService.getAnalytesOfExperiment(activeExperimentId));
      speciesMultiSelectComboBox.setValue(
          experimentInformationService.getSpeciesOfExperiment((activeExperimentId)));
      specimenMultiSelectComboBox.setValue(
          experimentInformationService.getSpecimensOfExperiment((activeExperimentId)));
    }

    private void setupExperimentalDesignSearch() {
      speciesMultiSelectComboBox.setItems(
          experimentalDesignSearchService.retrieveSpecies().stream()
              .sorted(Comparator.comparing(Species::label)).toList());
      speciesMultiSelectComboBox.setItemLabelGenerator(Species::value);
      specimenMultiSelectComboBox.setItems(
          experimentalDesignSearchService.retrieveSpecimens().stream()
              .sorted(Comparator.comparing(Specimen::label)).toList());
      specimenMultiSelectComboBox.setItemLabelGenerator(Specimen::value);
      analyteMultiSelectComboBox.setItems(
          experimentalDesignSearchService.retrieveAnalytes().stream()
              .sorted(Comparator.comparing(Analyte::label)).toList());
      analyteMultiSelectComboBox.setItemLabelGenerator(Analyte::value);
    }

    private void setUpPersonSearch(ComboBox<PersonReference> comboBox) {
      comboBox.setItems(
          query -> personSearchService.find(query.getFilter().orElse(""), query.getOffset(),
                  query.getLimit())
              .stream());
    }

    private void attachSubmissionActionOnValueChange() {
      ProjectDetailsComponent.Handler.submitOnValueChange(titleToggleComponent,
          value -> {
            if (Objects.isNull(selectedProject)) {
              return;
            }
            projectInformationService.updateTitle(selectedProject.value(), value.trim());
          });

      ProjectDetailsComponent.Handler.submitOnValueChange(projectObjectiveToggleComponent,
          value -> {
            if (Objects.isNull(selectedProject)) {
              return;
            }
            projectInformationService.stateObjective(selectedProject.value(), value.trim());
          });

      ProjectDetailsComponent.Handler.submitOnValueChange(experimentalDesignToggleComponent,
          value -> {
            if (Objects.isNull(selectedProject)) {
              return;
            }
            projectInformationService.describeExperimentalDesign(selectedProject.value(),
                value.trim());
          });

      ProjectDetailsComponent.Handler.submitOnValueChange(projectManagerToggleComponent,
          value ->
          {
            if (Objects.isNull(selectedProject)) {
              return;
            }
            projectInformationService.manageProject(selectedProject.value(), value);
          });

      ProjectDetailsComponent.Handler.submitOnValueChange(principalInvestigatorToggleComponent,
          value ->
          {
            if (Objects.isNull(selectedProject)) {
              return;
            }
            projectInformationService.investigateProject(selectedProject.value(), value);
          });
      ProjectDetailsComponent.Handler.submitOnValueChange(responsiblePersonToggleComponent,
          value ->
          {
            if (Objects.isNull(selectedProject)) {
              return;
            }
            projectInformationService.setResponsibility(selectedProject.value(), value);
          });

      ProjectDetailsComponent.Handler.submitOnValueAdded(speciesMultiSelectComboBox,
          value ->
          {
            if (Objects.isNull((activeExperimentId))) {
              return;
            }
            experimentInformationService.addSpeciesToExperiment(activeExperimentId,
                value.toArray(Species[]::new));
          });
      ProjectDetailsComponent.Handler.submitOnValueAdded(specimenMultiSelectComboBox,
          value ->
          {
            if (Objects.isNull(activeExperimentId)) {
              return;
            }
            experimentInformationService.addSpecimenToExperiment(activeExperimentId,
                value.toArray(Specimen[]::new));
          });

      ProjectDetailsComponent.Handler.submitOnValueAdded(analyteMultiSelectComboBox,
          value ->
          {
            if (Objects.isNull(activeExperimentId)) {
              return;
            }
            experimentInformationService.addAnalyteToExperiment(activeExperimentId,
                value.toArray(Analyte[]::new));
          });
    }

    private static <V, T extends HasValue<?, V>> void submitOnValueChange(T element,
        Consumer<V> submitAction) {
      element.addValueChangeListener(it -> submitAction.accept(element.getValue()));
    }

    private static <V extends Collection<?>, T extends HasValue<?, V>> void submitOnValueAdded(
        T element,
        Consumer<V> submitAction) {
      element.addValueChangeListener(it -> {
        V oldValue = it.getOldValue();
        V value = it.getValue();
        if (oldValue.containsAll(value)) {
          // nothing was added -> so we do not need to do anything
        } else if (value.containsAll(oldValue)) {
          // only added something
          submitAction.accept(value);
        } else {
          //FIXME what to do? there seem to be elements deleted and added in this event
        }
      });
    }

  }
}
