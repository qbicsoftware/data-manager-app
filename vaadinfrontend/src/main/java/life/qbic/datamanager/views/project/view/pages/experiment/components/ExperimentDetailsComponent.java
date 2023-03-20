package life.qbic.datamanager.views.project.view.pages.experiment.components;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.board.Board;
import com.vaadin.flow.component.board.Row;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.ChartType;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
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
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Project Details Component
 * <p>
 * Shows project details to the user.
 *
 * @since 1.0.0
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
  VerticalLayout speciesForm;
  VerticalLayout specimenForm;
  VerticalLayout analyteForm;
  private CardLayout blockingVariableCard;
  private CardLayout experimentalVariableCard;


  public ExperimentDetailsComponent(@Autowired ProjectInformationService projectInformationService,
      @Autowired ExperimentInformationService experimentInformationService) {
    Objects.requireNonNull(projectInformationService);
    Objects.requireNonNull(experimentInformationService);
    initTopLayout();
    initTabSheet();
    this.handler = new Handler(projectInformationService, experimentInformationService);
  }

  public void projectId(String parameter) {
    this.handler.setProjectId(ProjectId.parse(parameter));
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

  private void initTabSheet() {
    experimentSheet = new TabSheet();
    initSummaryCardBoard();
    initSampleGroupsCardBoard();
    experimentSheet.add("Summary", summaryCardBoard);
    experimentSheet.add("Sample Groups", sampleGroupsCardBoard);
    getContent().addFields(experimentSheet);
    experimentSheet.setSizeFull();
  }

  private void initSummaryCardBoard() {
    summaryCardBoard = new Board();
    initSampleOriginCard();
    initBlockingVariableCard();
    initExperimentalVariableCard();
    Div div = new Div();
    Row topRow = new Row(sampleOriginCard, blockingVariableCard);
    Row bottomRow = new Row(experimentalVariableCard, div);
    summaryCardBoard.add(topRow, bottomRow);
    summaryCardBoard.setSizeFull();
  }

  private void initSampleOriginCard() {
    sampleOriginCard = new CardLayout();
    sampleOriginCard.addTitle("Sample Origin");
    FormLayout sampleOriginLayout = new FormLayout();
    speciesForm = new VerticalLayout();
    specimenForm = new VerticalLayout();
    analyteForm = new VerticalLayout();
    sampleOriginLayout.addFormItem(speciesForm, "Origin");
    sampleOriginLayout.addFormItem(specimenForm, "Specimen");
    sampleOriginLayout.addFormItem(analyteForm, "Analyte");
    sampleOriginLayout.setSizeFull();
    sampleOriginCard.addFields(sampleOriginLayout);
  }

  private void initExperimentalVariableCard() {
    experimentalVariableCard = new CardLayout();
    experimentalVariableCard.addTitle("Experimental Variables");
    //ToDo Replace this layout once backend with information is ready
    VerticalLayout templateLayout = new VerticalLayout();
    Span templateText = new Span("No Experimental Variables defined");
    Button addButton = new Button("Add");
    addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    templateLayout.add(templateText, addButton);
    templateLayout.setAlignItems(Alignment.CENTER);
    templateLayout.setSizeFull();
    experimentalVariableCard.addFields(templateLayout);
  }

  private void initBlockingVariableCard() {
    blockingVariableCard = new CardLayout();
    blockingVariableCard.addTitle("Blocking Variables");
    //ToDo Replace this layout once backend with information is ready
    VerticalLayout templateLayout = new VerticalLayout();
    Span templateText = new Span("No Blocking Variable defined");
    Button addButton = new Button("Add");
    addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    templateLayout.add(templateText, addButton);
    templateLayout.setAlignItems(Alignment.CENTER);
    templateLayout.setSizeFull();
    blockingVariableCard.addFields(templateLayout);
  }

  private void initSampleGroupsCardBoard() {
    sampleGroupsCardBoard = new Board();
    sampleGroupsCardBoard.setWidthFull();
    //ToDo Fill with Content
  }


  public void setStyles(String... componentStyles) {
    getContent().addClassNames(componentStyles);
  }

  /**
   * Component logic for the {@link ExperimentDetailsComponent}
   */
  private final class Handler {

    private ProjectId projectId;
    private final ProjectInformationService projectInformationService;
    private final ExperimentInformationService experimentInformationService;

    public Handler(ProjectInformationService projectInformationService,
        ExperimentInformationService experimentInformationService) {
      this.projectInformationService = projectInformationService;
      this.experimentInformationService = experimentInformationService;

    }

    public void setProjectId(ProjectId projectId) {
      this.projectId = projectId;
      projectInformationService.find(projectId.value())
          .ifPresentOrElse(this::getActiveExperimentFromProject, this::emptyAction);
    }

    private void getActiveExperimentFromProject(Project project) {
      experimentInformationService.find(project.activeExperiment().value())
          .ifPresentOrElse(this::loadExperimentInformation, this::emptyAction);
    }

    private void loadExperimentInformation(Experiment experiment) {
      getContent().addTitle(experiment.getName());
      loadTagInformation(experiment);
      loadSampleOriginInformation(experiment);
      loadBlockingVariableInformation();
      loadExperimentalVariableInformation();
    }

    private void loadTagInformation(Experiment experiment) {
      tagLayout.removeAll();
      experiment.getSpecies().forEach(species -> tagLayout.add(new Span(species.value())));
      experiment.getSpecimens().forEach(specimen -> tagLayout.add(new Span(specimen.value())));
      experiment.getAnalytes().forEach(analyte -> tagLayout.add(new Span(analyte.value())));
      initTagButton();
    }

    //Todo what should be added here? Additional separate button building and functionality
    private void initTagButton() {
      Icon plusIcon = LumoIcon.PLUS.create();
      plusIcon.addClassNames(IconSize.SMALL);
      tagLayout.add(plusIcon);
      tagLayout.getChildren()
          .forEach(component -> {
            component.getElement().getThemeList().add("badge small");
            component.getElement().getThemeList().add(FontSize.SMALL);
          });
    }

    private void loadSampleOriginInformation(Experiment experiment) {
      //Todo What do we want to show here if multiple values are defined
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

    private void loadExperimentalVariableInformation() {
      //Todo load information from backend once implemented

    }

    //ToDo what should happen in the UI if neither project nor experiment has been found?
    private void emptyAction() {

    }

  }
}
