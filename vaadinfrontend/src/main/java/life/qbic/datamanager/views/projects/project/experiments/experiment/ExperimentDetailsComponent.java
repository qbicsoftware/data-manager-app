package life.qbic.datamanager.views.projects.project.experiments.experiment;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.board.Board;
import com.vaadin.flow.component.board.Row;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoUtility.Display;
import com.vaadin.flow.theme.lumo.LumoUtility.Overflow;
import com.vaadin.flow.theme.lumo.LumoUtility.TextOverflow;
import com.vaadin.flow.theme.lumo.LumoUtility.Whitespace;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import life.qbic.datamanager.views.general.ToggleDisplayEditComponent;
import life.qbic.datamanager.views.layouts.CardComponent;
import life.qbic.datamanager.views.layouts.PageComponent;
import life.qbic.datamanager.views.notifications.InformationMessage;
import life.qbic.datamanager.views.notifications.StyledNotification;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentInformationPage;
import life.qbic.datamanager.views.projects.project.experiments.experiment.AddExperimentalGroupsDialog.ExperimentalGroupSubmitEvent;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.application.ExperimentInformationService.ExperimentalGroupDTO;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentalDesign.AddExperimentalGroupResponse;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentalVariable;
import life.qbic.projectmanagement.domain.project.experiment.VariableLevel;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <b>Experimental Details Component</b>
 *
 * <p>A PageComponent based Composite showing the information stored in the
 * {@link life.qbic.projectmanagement.domain.project.experiment.ExperimentalDesign} associated with
 * a {@link Project} within the {@link ExperimentInformationPage}
 */
@UIScope
@SpringComponent
public class ExperimentDetailsComponent extends Composite<PageComponent> {

  @Serial
  private static final long serialVersionUID = -8992991642015281245L;
  private final transient Handler handler;
  private final HorizontalLayout tagLayout = new HorizontalLayout();
  private final TabSheet experimentSheet = new TabSheet();
  private final Board summaryCardBoard = new Board();
  private final ExperimentalGroupsLayout experimentalGroupsLayoutBoard = new ExperimentalGroupsLayout();

  private final ExperimentalGroupsCollection experimentalGroupsCollection = new ExperimentalGroupsCollection();
  private final CardComponent sampleOriginCard = new CardComponent();
  private final VerticalLayout speciesForm = new VerticalLayout();
  private final VerticalLayout specimenForm = new VerticalLayout();
  private final VerticalLayout analyteForm = new VerticalLayout();
  private final CardComponent blockingVariableCard = new CardComponent();
  private ExperimentalVariableCard experimentalVariableCard;
  private final Button addBlockingVariableButton = new Button("Add");
  private final AddVariablesDialog addVariablesDialog;
  private final AddExperimentalGroupsDialog experimentalGroupsDialog;


  public ExperimentDetailsComponent(@Autowired ProjectInformationService projectInformationService,
      @Autowired ExperimentInformationService experimentInformationService) {
    Objects.requireNonNull(projectInformationService);
    Objects.requireNonNull(experimentInformationService);
    this.addVariablesDialog = new AddVariablesDialog(experimentInformationService);
    getContent().indentContent(false);
    initTagAndNotesLayout();
    initTabSheet(experimentInformationService);
    experimentalGroupsDialog = createExperimentalGroupDialog();
    this.handler = new Handler(projectInformationService, experimentInformationService);
  }

  public void experimentId(ExperimentId experimentId) {
    this.handler.setExperimentId(experimentId);
  }

  private void initTagAndNotesLayout() {
    VerticalLayout tagAndNotesLayout = new VerticalLayout();
    tagLayout.setWidthFull();
    tagLayout.addClassName("spacing-s");
    tagLayout.addClassName(Overflow.HIDDEN);
    tagLayout.addClassName(Whitespace.NOWRAP);
    tagLayout.addClassName(TextOverflow.ELLIPSIS);
    tagLayout.addClassName(Display.INLINE);
    Span noNotesDefined = new Span("Click to add Notes");
    ToggleDisplayEditComponent<Span, TextField, String> experimentNotes = new ToggleDisplayEditComponent<>(
        Span::new, new TextField(), noNotesDefined);
    tagAndNotesLayout.setWidthFull();
    tagAndNotesLayout.add(tagLayout, experimentNotes);
    tagAndNotesLayout.setPadding(false);
    tagAndNotesLayout.setMargin(false);
    getContent().addContent(tagAndNotesLayout);
  }

  private void initTabSheet(ExperimentInformationService experimentInformationService) {
    initSummaryCardBoard(experimentInformationService);
    initExperimentalGroupsBoard();
    experimentSheet.add("Summary", summaryCardBoard);
    experimentSheet.add("Experimental Groups", experimentalGroupsLayoutBoard);
    getContent().addContent(experimentSheet);
    experimentSheet.setSizeFull();
  }

  private void initSummaryCardBoard(ExperimentInformationService experimentInformationService) {
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
    sampleOriginCard.addTitle("Sample Origin");
    FormLayout sampleOriginLayout = new FormLayout();
    sampleOriginLayout.setResponsiveSteps(new ResponsiveStep("0", 1));
    sampleOriginLayout.addFormItem(speciesForm, "Species");
    sampleOriginLayout.addFormItem(specimenForm, "Specimen");
    sampleOriginLayout.addFormItem(analyteForm, "Analyte");
    sampleOriginLayout.setSizeFull();
    sampleOriginCard.setMargin(false);
    sampleOriginCard.addContent(sampleOriginLayout);
  }

  private void initBlockingVariableCard() {
    blockingVariableCard.addTitle("Blocking Variables");
    VerticalLayout templateLayout = new VerticalLayout();
    Span templateText = new Span("No Blocking Variable defined");
    addBlockingVariableButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    templateLayout.add(templateText, addBlockingVariableButton);
    templateLayout.setAlignItems(Alignment.CENTER);
    templateLayout.setSizeFull();
    templateLayout.setJustifyContentMode(JustifyContentMode.CENTER);
    blockingVariableCard.addContent(templateLayout);
    blockingVariableCard.setMargin(false);
  }

  private void initExperimentalVariableCard(
      ExperimentInformationService experimentInformationService) {
    experimentalVariableCard = new ExperimentalVariableCard(experimentInformationService);
    experimentalVariableCard.setMargin(false);
    experimentalVariableCard.setAddButtonAction(addVariablesDialog::open);
  }

  private void initExperimentalGroupsBoard() {
    experimentalGroupsLayoutBoard.setWidthFull();
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
      if (!variables.isEmpty()) {
        experimentalGroupsDialog.open();
      } else {
        selectSummaryTab();
        InformationMessage successMessage = new InformationMessage(
            "No experimental variables are defined",
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

      List<ExperimentalGroup> expGroups = experimentInformationService.experimentalGroupsFor(experimentId);

      experimentalGroupsCollection.addComponents(expGroups.stream().map(ExperimentalGroupCard::new).toList());
    }

    private void addCloseListenerForAddVariableDialog() {
      addVariablesDialog.addOpenedChangeListener(it -> {
        if (!it.isOpened()) {
          experimentalVariableCard.refresh();
        }
      });
    }

    private void setExperimentId(ExperimentId experimentId) {
      experimentInformationService.find(experimentId).ifPresent(this::loadExperimentInformation);
    }

    private void fillExperimentalGroupDialog() {
      Objects.requireNonNull(experimentId, "experiment id not set");
      List<ExperimentalVariable> variables = experimentInformationService.getVariablesOfExperiment(
          experimentId);
      List<VariableLevel> levels = variables.stream()
          .flatMap(variable -> variable.levels().stream()).toList();
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
      List<String> tags = new ArrayList<>();
      experiment.getSpecies().forEach(species -> tags.add(species.value()));
      experiment.getSpecimens().forEach(specimen -> tags.add(specimen.value()));
      experiment.getAnalytes().forEach(analyte -> tags.add(analyte.value()));
      tags.forEach(tag -> tagLayout.add(new Tag(tag)));
      tagLayout.getElement().setAttribute("Title", String.join(" ", tags));
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
      switch (response.responseCode()) {
        case SUCCESS -> handleGroupSubmittedSuccess(groupSubmitted);
        case CONDITION_EXISTS -> handleDuplicateConditionInput();
      }
    }

    private void handleGroupSubmittedSuccess(ExperimentalGroupSubmitEvent groupSubmitted) {
      loadExperimentalGroups();
      groupSubmitted.eventSourceDialog().close();
    }

    private void handleDuplicateConditionInput() {
      InformationMessage infoMessage = new InformationMessage(
          "A group with the same condition exists already.", "");
      StyledNotification notification = new StyledNotification(infoMessage);
      notification.open();
    }
  }
}
