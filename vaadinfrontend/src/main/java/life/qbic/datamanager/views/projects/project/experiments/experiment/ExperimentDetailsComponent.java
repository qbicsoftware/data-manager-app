package life.qbic.datamanager.views.projects.project.experiments.experiment;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.board.Board;
import com.vaadin.flow.component.board.Row;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
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
import life.qbic.application.commons.Result;
import life.qbic.datamanager.views.general.CreationCard;
import life.qbic.datamanager.views.general.CreationClickedEvent;
import life.qbic.datamanager.views.general.DisclaimerCard;
import life.qbic.datamanager.views.general.DisclaimerConfirmedEvent;
import life.qbic.datamanager.views.general.Tag;
import life.qbic.datamanager.views.general.ToggleDisplayEditComponent;
import life.qbic.datamanager.views.layouts.CardComponent;
import life.qbic.datamanager.views.layouts.PageComponent;
import life.qbic.datamanager.views.notifications.InformationMessage;
import life.qbic.datamanager.views.notifications.StyledNotification;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentInformationPage;
import life.qbic.datamanager.views.projects.project.experiments.experiment.AddExperimentalGroupsDialog.ExperimentalGroupSubmitEvent;
import life.qbic.datamanager.views.projects.project.experiments.experiment.component.ExperimentInfoComponent;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.ExperimentalGroupCardCollection;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.application.ExperimentInformationService.ExperimentalGroupDTO;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentalDesign.AddExperimentalGroupResponse.ResponseCode;
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

  private ExperimentalVariablesCard experimentalVariablesCard;
  @Serial
  private static final long serialVersionUID = -8992991642015281245L;
  private final transient Handler handler;
  private final HorizontalLayout tagLayout = new HorizontalLayout();
  private final TabSheet experimentSheet = new TabSheet();

  private final Div contentExperimentalGroupsTab = new Div();

  private final Board summaryCardBoard = new Board();

  private final Div experimentSummary = new Div();
  private final ExperimentalGroupCardCollection experimentalGroupsCollection = new ExperimentalGroupCardCollection();
  private final CardComponent sampleOriginCard = new CardComponent();
  private final CardComponent blockingVariableCard = new CardComponent();
  private final Button addBlockingVariableButton = new Button("Add");
  private final AddVariablesDialog addVariablesDialog;
  private final AddExperimentalGroupsDialog experimentalGroupsDialog;

  private final DisclaimerCard noExperimentalVariablesDefined;

  private final CreationCard experimentalGroupCreationCard = CreationCard.create(
      "Add Experimental Group");


  public ExperimentDetailsComponent(@Autowired ProjectInformationService projectInformationService,
      @Autowired ExperimentInformationService experimentInformationService) {
    Objects.requireNonNull(projectInformationService);
    Objects.requireNonNull(experimentInformationService);
    this.addVariablesDialog = new AddVariablesDialog(experimentInformationService);
    this.noExperimentalVariablesDefined = createDisclaimer();
    getContent().indentContent(false);
    initTagAndNotesLayout();
    initTabSheet(experimentInformationService);
    addCreationCard();
    experimentalGroupsDialog = createExperimentalGroupDialog();

    this.handler = new Handler(experimentInformationService);
    setUpCreationCard();
  }

  public DisclaimerCard createDisclaimer() {
    var disclaimer = DisclaimerCard.createWithTitle("Missing variables",
        "No experiment variables defined", "Add");
    disclaimer.subscribe(this::handleEvent);
    return disclaimer;
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
    experimentSheet.add("Summary", experimentSummary);
    experimentSheet.add("Experimental Groups", contentExperimentalGroupsTab);
    getContent().addContent(experimentSheet);
    experimentSheet.setSizeFull();
  }

  private void addCreationCard() {
    experimentalGroupsCollection.addComponentAsFirst(experimentalGroupCreationCard);
  }

  private AddExperimentalGroupsDialog createExperimentalGroupDialog() {
    AddExperimentalGroupsDialog dialog = new AddExperimentalGroupsDialog();
    dialog.addExperimentalGroupSubmitListener(
        groupSubmitted -> handler.onGroupSubmitted(groupSubmitted));
    return dialog;
  }

  private void setUpCreationCard() {
    experimentalGroupCreationCard.addListener(this::handleEvent);
  }

  private void handleEvent(DisclaimerConfirmedEvent disclaimerConfirmedEvent) {
    experimentSheet.setSelectedIndex(0);
  }

  private void initSummaryCardBoard(ExperimentInformationService experimentInformationService) {
    initBlockingVariableCard();
    initExperimentalVariableCard(experimentInformationService);
    Row topRow = new Row(sampleOriginCard, experimentalVariablesCard);
    Row bottomRow = new Row(blockingVariableCard);
    summaryCardBoard.add(topRow, bottomRow);
    summaryCardBoard.setSizeFull();

  }

  private void initExperimentalGroupsBoard() {
    experimentalGroupsCollection.setWidthFull();
  }

  public void handleEvent(CreationClickedEvent creationClickedEvent) {
    experimentalGroupsDialog.open();
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
    experimentalVariablesCard = new ExperimentalVariablesCard(experimentInformationService);
    experimentalVariablesCard.setMargin(false);
    experimentalVariablesCard.setAddButtonAction(addVariablesDialog::open);
  }

  public void loadExperiment(ExperimentId experimentId) {
    this.handler.loadExperiment(experimentId);
  }

  public void setStyles(String... componentStyles) {
    getContent().addClassNames(componentStyles);
  }

  public void handleEvent(ExperimentalGroupDeletionEvent experimentalGroupDeletionEvent) {
    handler.experimentInformationService.deleteExperimentGroup(handler.experimentId,
        experimentalGroupDeletionEvent.getSource().groupId());
    experimentalGroupsCollection.remove(experimentalGroupDeletionEvent.getSource());
  }

  private final class Handler {

    private ExperimentId experimentId;
    private final ExperimentInformationService experimentInformationService;

    public Handler(ExperimentInformationService experimentInformationService) {
      this.experimentInformationService = experimentInformationService;
      addCloseListenerForAddVariableDialog();
    }

    private void addCloseListenerForAddVariableDialog() {
      addVariablesDialog.addOpenedChangeListener(it -> {
        if (!it.isOpened()) {
          experimentalVariablesCard.refresh();
        }
      });
    }

    private void loadExperiment(ExperimentId experimentId) {
      experimentInformationService.find(experimentId).ifPresent(this::loadExperimentInformation);
    }

    private void loadExperimentInformation(Experiment experiment) {
      this.experimentId = experiment.experimentId();
      getContent().addTitle(experiment.getName());
      loadTagInformation(experiment);
      loadExperimentInfo(experiment);
      loadBlockingVariableInformation();
      experimentalVariablesCard.experimentId(experiment.experimentId());
      addVariablesDialog.experimentId(experiment.experimentId());
      fillExperimentalGroupDialog();
      loadExperimentalGroups();
      if (experiment.variables().isEmpty()) {
        displayDisclaimer();
        hideExperimentalGroupsCollection();
      } else {
        removeDisclaimer();
        displayExperimentalGroupsCollection();
      }
    }

    private void displayExperimentalGroupsCollection() {
      contentExperimentalGroupsTab.add(experimentalGroupsCollection);
    }

    private void hideExperimentalGroupsCollection() {
      contentExperimentalGroupsTab.remove(experimentalGroupsCollection);
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

    private void loadExperimentInfo(Experiment experiment) {
      ExperimentInfoComponent factSheet = ExperimentInfoComponent.create(experiment.getSpecies(),
          experiment.getSpecimens(), experiment.getAnalytes());
      ExperimentDetailsComponent.this.experimentSummary.add(factSheet);
      factSheet.showMenu();
    }

    private void loadBlockingVariableInformation() {
      //ToDo load information from backend once implemented
    }

    private void fillExperimentalGroupDialog() {
      Objects.requireNonNull(experimentId, "experiment id not set");
      List<ExperimentalVariable> variables = experimentInformationService.getVariablesOfExperiment(
          experimentId);
      List<VariableLevel> levels = variables.stream()
          .flatMap(variable -> variable.levels().stream()).toList();
      experimentalGroupsDialog.setLevels(levels);
    }

    private void loadExperimentalGroups() {
      Objects.requireNonNull(experimentId, "Experiment id not set");
      // We load the experimental groups of the experiment and render them as cards
      List<ExperimentalGroupCard> experimentalGroupsCards = experimentInformationService.experimentalGroupsFor(
          experimentId).stream().map(ExperimentalGroupCard::new).toList();

      // We register the experimental details component as listener for group deletion events
      experimentalGroupsCards.forEach(this::subscribeToDeletionClickEvent);
      experimentalGroupsCollection.removeAll();
      experimentalGroupsCollection.addComponents(experimentalGroupsCards);
    }

    private void displayDisclaimer() {
      contentExperimentalGroupsTab.add(noExperimentalVariablesDefined);
    }

    private void removeDisclaimer() {
      contentExperimentalGroupsTab.remove(noExperimentalVariablesDefined);
    }

    private void subscribeToDeletionClickEvent(ExperimentalGroupCard experimentalGroupCard) {
      experimentalGroupCard.addDeletionEventListener(ExperimentDetailsComponent.this::handleEvent);
    }

    public void onGroupSubmitted(ExperimentalGroupSubmitEvent groupSubmitted) {
      Result<ExperimentalGroup, ResponseCode> response = experimentInformationService.addExperimentalGroupToExperiment(
          experimentId,
          new ExperimentalGroupDTO(groupSubmitted.variableLevels(), groupSubmitted.sampleSize()));
      if (response.isValue()) {
        handleGroupSubmittedSuccess();
      } else {
        handleDuplicateConditionInput();
      }
    }

    private void handleGroupSubmittedSuccess() {
      loadExperimentalGroups();
      addCreationCard();
      experimentalGroupsDialog.close();
    }

    private void handleDuplicateConditionInput() {
      InformationMessage infoMessage = new InformationMessage(
          "A group with the same condition exists already.", "");
      StyledNotification notification = new StyledNotification(infoMessage);
      notification.open();
    }

  }
}
