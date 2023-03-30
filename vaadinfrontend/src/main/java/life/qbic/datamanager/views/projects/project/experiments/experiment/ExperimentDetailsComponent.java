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
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoIcon;
import com.vaadin.flow.theme.lumo.LumoUtility.FontSize;
import com.vaadin.flow.theme.lumo.LumoUtility.IconSize;
import java.io.Serial;
import java.util.Objects;
import life.qbic.datamanager.views.general.ToggleDisplayEditComponent;
import life.qbic.datamanager.views.layouts.CardLayout;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentInformationPage;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
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
  private static final Logger logger = LoggerFactory.logger(ExperimentDetailsComponent.class);
  private ToggleDisplayEditComponent<Span, TextField, String> experimentNotes;
  private Chart registeredSamples;
  private HorizontalLayout topLayout;
  private HorizontalLayout tagLayout;
  private VerticalLayout detailLayout;
  private TabSheet experimentSheet;
  private Board summaryCardBoard;
  private Board sampleGroupsCardBoard;
  private CardLayout sampleOriginCard;
  private VerticalLayout speciesForm;
  private VerticalLayout specimenForm;
  private VerticalLayout analyteForm;

  //Todo Move all cardLayouts into separate components.
  private CardLayout blockingVariableCard;
  private ExperimentalVariableCard experimentalVariableCard;
  private Button addBlockingVariableButton;

  public ExperimentDetailsComponent(@Autowired ProjectInformationService projectInformationService,
      @Autowired ExperimentInformationService experimentInformationService) {
    Objects.requireNonNull(projectInformationService);
    Objects.requireNonNull(experimentInformationService);
    initTopLayout();
    initTabSheet(experimentInformationService);
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
    experimentSheet.add("Sample Groups", sampleGroupsCardBoard);
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
  }

  private void initSampleGroupsCardBoard() {
    sampleGroupsCardBoard = new Board();
    sampleGroupsCardBoard.setWidthFull();
    //ToDo Fill with Content
  }

  public void setStyles(String... componentStyles) {
    getContent().addClassNames(componentStyles);
  }

  private final class Handler {

    private final ProjectInformationService projectInformationService;
    private final ExperimentInformationService experimentInformationService;

    public Handler(ProjectInformationService projectInformationService,
        ExperimentInformationService experimentInformationService) {
      this.projectInformationService = projectInformationService;
      this.experimentInformationService = experimentInformationService;
    }

    public void setProjectId(ProjectId projectId) {
      projectInformationService.find(projectId)
          .ifPresent(this::getActiveExperimentFromProject);
    }

    private void getActiveExperimentFromProject(Project project) {
      experimentInformationService.find(project.activeExperiment().value())
          .ifPresent(this::loadExperimentInformation);
    }

    private void loadExperimentInformation(Experiment experiment) {
      getContent().addTitle(experiment.getName());
      loadTagInformation(experiment);
      loadSampleOriginInformation(experiment);
      loadBlockingVariableInformation();
      experimentalVariableCard.experimentId(experiment.experimentId());
    }

    private void loadTagInformation(Experiment experiment) {
      tagLayout.removeAll();
      experiment.getSpecies().forEach(species -> tagLayout.add(new Span(species.value())));
      experiment.getSpecimens().forEach(specimen -> tagLayout.add(new Span(specimen.value())));
      experiment.getAnalytes().forEach(analyte -> tagLayout.add(new Span(analyte.value())));
      initTagPlusButton();
    }

    //Todo what should be added here? Additional separate button building and functionality
    private void initTagPlusButton() {
      Icon plusIcon = LumoIcon.PLUS.create();
      plusIcon.addClassNames(IconSize.SMALL);
      tagLayout.add(plusIcon);
      tagLayout.getChildren().forEach(component -> {
        component.getElement().getThemeList().add("badge small");
        component.getElement().getThemeList().add(FontSize.SMALL);
      });
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

  }
}
