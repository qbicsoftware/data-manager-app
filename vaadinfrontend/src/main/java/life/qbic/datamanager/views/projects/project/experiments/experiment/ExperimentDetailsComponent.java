package life.qbic.datamanager.views.projects.project.experiments.experiment;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.board.Board;
import com.vaadin.flow.component.board.Row;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.ChartType;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.List;
import java.util.Objects;
import life.qbic.datamanager.views.general.ToggleDisplayEditComponent;
import life.qbic.datamanager.views.layouts.CardLayout;
import life.qbic.datamanager.views.notifications.InformationMessage;
import life.qbic.datamanager.views.notifications.StyledNotification;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentInformationPage;
import life.qbic.datamanager.views.projects.project.experiments.experiment.AddExperimentalGroupsDialog.ExperimentalGroupSubmitEvent;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.application.ExperimentInformationService.ExperimentalGroupDTO;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentalDesign.AddExperimentalGroupResponse;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentalDesign.AddExperimentalGroupResponse.ResponseCode;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentalVariable;
import life.qbic.projectmanagement.domain.project.experiment.VariableLevel;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <b>Experimental Details Component</b>
 *
 * <p>A CardLayout based Composite showing the information stored in the
 * {@link life.qbic.projectmanagement.domain.project.experiment.ExperimentalDesign} associated with
 * a {@link Project} within the {@link ExperimentInformationPage}
 */
@UIScope
@SpringComponent
public class ExperimentDetailsComponent extends Composite<CardLayout> {

  @Serial
  private static final long serialVersionUID = -8992991642015281245L;
  private final transient Handler handler;
  private ToggleDisplayEditComponent<Span, TextField, String> experimentNotes;
  private Chart registeredSamples;
  private HorizontalLayout topLayout;
  private HorizontalLayout tagLayout;
  private VerticalLayout detailLayout;
  private TabSheet experimentSheet;
  private Board summaryCardBoard;
  private ExperimentalGroupsLayout experimentalGroupsLayoutBoard;
  private CardLayout sampleOriginCard;
  private VerticalLayout speciesForm;
  private VerticalLayout specimenForm;
  private VerticalLayout analyteForm;

  //Todo Move all cardLayouts into separate components.
  private CardLayout blockingVariableCard;
  private ExperimentalVariableCard experimentalVariableCard;
  private Button addBlockingVariableButton;

  private final AddVariablesDialog addVariablesDialog;
  private final AddExperimentalGroupsDialog experimentalGroupsDialog;


  public ExperimentDetailsComponent(@Autowired ProjectInformationService projectInformationService,
      @Autowired ExperimentInformationService experimentInformationService) {
    Objects.requireNonNull(projectInformationService);
    Objects.requireNonNull(experimentInformationService);
    this.addVariablesDialog = new AddVariablesDialog(experimentInformationService);
    initTopLayout();
    initTabSheet(experimentInformationService);
    experimentalGroupsDialog = createExperimentalGroupDialog();
    this.handler = new Handler(projectInformationService, experimentInformationService);
  }

  public void projectId(ProjectId projectId) {
    this.handler.setProjectId(projectId);
  }

  private void initTopLayout() {
    topLayout = new HorizontalLayout();
    registeredSamples = new Chart(ChartType.AREASPLINE);
    initDetailLayout();
    topLayout.add(detailLayout, registeredSamples);
    getContent().addFields(topLayout);
    topLayout.setWidthFull();
  }

  private void initDetailLayout() {
    detailLayout = new VerticalLayout();
    tagLayout = new HorizontalLayout();
    Span noNotesDefined = new Span("Click to add Notes");
    experimentNotes = new ToggleDisplayEditComponent<>(Span::new, new TextField(), noNotesDefined);
    detailLayout.setWidthFull();
    detailLayout.add(tagLayout, experimentNotes);
  }

  private void initTabSheet(ExperimentInformationService experimentInformationService) {
    experimentSheet = new TabSheet();
    initSummaryCardBoard(experimentInformationService);
    initSampleGroupsCardBoard();
    experimentSheet.add("Summary", summaryCardBoard);
    experimentSheet.add("Experimental Groups", experimentalGroupsLayoutBoard);
    getContent().addFields(experimentSheet);
    experimentSheet.setSizeFull();
  }

  private void initSummaryCardBoard(ExperimentInformationService experimentInformationService) {
    summaryCardBoard = new Board();
    initSampleOriginCard();
    initBlockingVariableCard();
    initExperimentalVariableCard(experimentInformationService);
    Row topRow = new Row(sampleOriginCard, blockingVariableCard);
    Row bottomRow = new Row(experimentalVariableCard);
    summaryCardBoard.add(topRow, bottomRow);
    summaryCardBoard.setSizeFull();
  }

  private AddExperimentalGroupsDialog createExperimentalGroupDialog() {
    AddExperimentalGroupsDialog dialog = new AddExperimentalGroupsDialog();
    dialog.addExperimentalGroupSubmitListener(
        groupSubmitted -> handler.onGroupSubmitted(groupSubmitted));
    return dialog;
  }


  private void initSampleOriginCard() {
    sampleOriginCard = new CardLayout();
    sampleOriginCard.addTitle("Sample Origin");
    FormLayout sampleOriginLayout = new FormLayout();
    sampleOriginLayout.setResponsiveSteps(new ResponsiveStep("0", 1));
    speciesForm = new VerticalLayout();
    specimenForm = new VerticalLayout();
    analyteForm = new VerticalLayout();
    sampleOriginLayout.addFormItem(speciesForm, "Species");
    sampleOriginLayout.addFormItem(specimenForm, "Specimen");
    sampleOriginLayout.addFormItem(analyteForm, "Analyte");
    sampleOriginLayout.setSizeFull();
    sampleOriginCard.addFields(sampleOriginLayout);
  }

  private void initBlockingVariableCard() {
    blockingVariableCard = new CardLayout();
    blockingVariableCard.addTitle("Blocking Variables");
    VerticalLayout templateLayout = new VerticalLayout();
    Span templateText = new Span("No Blocking Variable defined");
    addBlockingVariableButton = new Button("Add");
    addBlockingVariableButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    templateLayout.add(templateText, addBlockingVariableButton);
    templateLayout.setAlignItems(Alignment.CENTER);
    templateLayout.setSizeFull();
    blockingVariableCard.addFields(templateLayout);
  }

  private void initExperimentalVariableCard(
      ExperimentInformationService experimentInformationService) {
    experimentalVariableCard = new ExperimentalVariableCard(experimentInformationService);
    experimentalVariableCard.setAddButtonAction(addVariablesDialog::open);
  }

  private void initSampleGroupsCardBoard() {
    experimentalGroupsLayoutBoard = new ExperimentalGroupsLayout();
    experimentalGroupsLayoutBoard.setWidthFull();
    //ToDo Fill with Content
  }

  public void setStyles(String... componentStyles) {
    getContent().addClassNames(componentStyles);
  }

  private final class Handler {

    private ExperimentId experimentId;
    private final ProjectInformationService projectInformationService;
    private final ExperimentInformationService experimentInformationService;

    public Handler(ProjectInformationService projectInformationService,
        ExperimentInformationService experimentInformationService) {
      this.projectInformationService = projectInformationService;
      this.experimentInformationService = experimentInformationService;
      addCloseListenerForAddVariableDialog();

      experimentalGroupsLayoutBoard.setExperimentalGroupCommandListener(it -> {
        fillExperimentalGroupDialog();
        handleAddExperimentalGroups();
      });
    }

    private void handleAddExperimentalGroups() {
      List<ExperimentalVariable> variables = experimentInformationService.getVariablesOfExperiment(
          experimentId);
      if(!variables.isEmpty()) {
        experimentalGroupsDialog.open();
      } else {
        selectSummaryTab();
        InformationMessage successMessage = new InformationMessage("No experimental variables are defined",
            "Please define all of your experimental variables before adding groups.");
        StyledNotification notification = new StyledNotification(successMessage);
        notification.open();
      }
    }

    private void selectSummaryTab() {
      experimentSheet.setSelectedIndex(0);
    }

    private void loadExperimentalGroups() {
      Objects.requireNonNull(experimentId, "experiment id not set");
      List<ExperimentalGroupDTO> experimentalGroups = experimentInformationService.getExperimentalGroups(
          experimentId);
      experimentalGroupsLayoutBoard.setExperimentalGroups(experimentalGroups);

    }

    public void setProjectId(ProjectId projectId) {
      projectInformationService.find(projectId)
          .ifPresent(this::getActiveExperimentFromProject);
    }

    private void addCloseListenerForAddVariableDialog() {
      addVariablesDialog.addOpenedChangeListener(it -> {
        if (!it.isOpened()) {
          experimentalVariableCard.refresh();
        }
      });
    }

    private void getActiveExperimentFromProject(Project project) {
      experimentInformationService.find(project.activeExperiment())
          .ifPresent(this::loadExperimentInformation);
    }

    private void fillExperimentalGroupDialog() {
      Objects.requireNonNull(experimentId, "experiment id not set");
      List<ExperimentalVariable> variables = experimentInformationService.getVariablesOfExperiment(
          experimentId);
      List<VariableLevel> levels = variables.stream()
          .flatMap(variable -> variable.levels().stream())
          .toList();
      experimentalGroupsDialog.setLevels(levels);
    }

    private void loadExperimentInformation(Experiment experiment) {
      this.experimentId = experiment.experimentId();
      getContent().addTitle(experiment.getName());
      loadTagInformation(experiment);
      loadSampleOriginInformation(experiment);
      loadBlockingVariableInformation();
      experimentalVariableCard.experimentId(experiment.experimentId());
      addVariablesDialog.experimentId(experiment.experimentId());
      fillExperimentalGroupDialog();
      loadExperimentalGroups();
    }

    private void loadTagInformation(Experiment experiment) {
      tagLayout.removeAll();
      experiment.getSpecies().forEach(species -> tagLayout.add(new Tag(species.value())));
      experiment.getSpecimens().forEach(specimen -> tagLayout.add(new Tag(specimen.value())));
      experiment.getAnalytes().forEach(analyte -> tagLayout.add(new Tag(analyte.value())));
    }

    private void loadSampleOriginInformation(Experiment experiment) {
      speciesForm.removeAll();
      specimenForm.removeAll();
      analyteForm.removeAll();
      experiment.getSpecies().forEach(species -> speciesForm.add(new Span(species.value())));
      experiment.getSpecimens().forEach(specimen -> specimenForm.add(new Span(specimen.value())));
      experiment.getAnalytes().forEach(analyte -> analyteForm.add(new Span(analyte.value())));
    }

    private void loadBlockingVariableInformation() {
      //ToDo load information from backend once implemented
    }

    public void onGroupSubmitted(ExperimentalGroupSubmitEvent groupSubmitted) {
      AddExperimentalGroupResponse response = experimentInformationService.addExperimentalGroupToExperiment(
          experimentId,
          new ExperimentalGroupDTO(groupSubmitted.variableLevels(), groupSubmitted.sampleSize()));
      if (response.responseCode() == ResponseCode.SUCCESS) {
        loadExperimentalGroups();
        groupSubmitted.source().close();
      }
    }
  }
}
