package life.qbic.datamanager.views.projects.project.samples;

import static org.slf4j.LoggerFactory.getLogger;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.beans.PropertyDescriptor;
import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import life.qbic.application.commons.Result;
import life.qbic.datamanager.views.AppRoutes.Projects;
import life.qbic.datamanager.views.general.DisclaimerCard;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.notifications.ErrorMessage;
import life.qbic.datamanager.views.notifications.StyledNotification;
import life.qbic.datamanager.views.notifications.SuccessMessage;
import life.qbic.datamanager.views.projects.project.ProjectViewPage;
import life.qbic.datamanager.views.projects.project.experiments.experiment.Tag;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.BatchRegistrationContent;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.BatchRegistrationDialog;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.SampleRegistrationContent;
import life.qbic.projectmanagement.application.SampleInformationService;
import life.qbic.projectmanagement.application.SampleRegistrationService;
import life.qbic.projectmanagement.application.batch.BatchInformationService;
import life.qbic.projectmanagement.application.batch.BatchRegistrationService;
import life.qbic.projectmanagement.application.batch.BatchRegistrationService.ResponseCode;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.experiment.BiologicalReplicate;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.project.experiment.VariableLevel;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Species;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Specimen;
import life.qbic.projectmanagement.domain.project.sample.Batch;
import life.qbic.projectmanagement.domain.project.sample.BatchId;
import life.qbic.projectmanagement.domain.project.sample.Sample;
import life.qbic.projectmanagement.domain.project.sample.SampleOrigin;
import life.qbic.projectmanagement.domain.project.sample.SampleRegistrationRequest;
import org.slf4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Sample Overview Component
 * <p>
 * Component embedded within the {@link SampleInformationPage} in the {@link ProjectViewPage}. It
 * allows the user to see the information associated for all {@link Sample} for each
 * {@link Experiment within a {@link life.qbic.projectmanagement.domain.project.Project}
 */

@SpringComponent
@UIScope
public class SampleOverviewComponent extends PageArea implements Serializable {

  @Serial
  private static final long serialVersionUID = 2893730975944372088L;
  private final Div content = new Div();
  private final Span title = new Span("Samples");
  private final Div buttonAndFieldBar = new Div();
  private final Span fieldBar = new Span();
  private final Span buttonBar = new Span();
  private final TextField searchField = new TextField();
  private final Select<String> tabFilterSelect = new Select<>();
  public final Button registerButton = new Button("Register");
  private final Button metadataDownloadButton = new Button("Download Metadata");
  private final TabSheet sampleExperimentTabSheet = new TabSheet();
  private final BatchRegistrationDialog batchRegistrationDialog = new BatchRegistrationDialog();
  private static final Logger log = getLogger(SampleOverviewComponent.class);
  private final transient SampleOverviewComponentHandler sampleOverviewComponentHandler;
  private static ProjectId projectId;

  public SampleOverviewComponent(@Autowired BatchInformationService batchInformationService,
      @Autowired SampleInformationService sampleInformationService,
      @Autowired BatchRegistrationService batchRegistrationService,
      @Autowired SampleRegistrationService sampleRegistrationService) {
    initSampleView();
    this.sampleOverviewComponentHandler = new SampleOverviewComponentHandler(
        batchInformationService, sampleInformationService, batchRegistrationService,
        sampleRegistrationService);
  }

  private void initSampleView() {
    this.addClassName("sample-overview-component");
    initButtonAndFieldBar();
    this.add(title);
    title.addClassName("title");
    this.add(content);
    content.addClassName("content");
  }

  private void initButtonAndFieldBar() {
    searchField.setPlaceholder("Search");
    searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
    searchField.setValueChangeMode(ValueChangeMode.EAGER);
    tabFilterSelect.setLabel("Search in");
    tabFilterSelect.setEmptySelectionAllowed(true);
    tabFilterSelect.setEmptySelectionCaption("All tabs");
    fieldBar.add(searchField, tabFilterSelect);
    fieldBar.addClassName("search-bar");
    //Items in layout should be aligned at the end due to searchFieldLabel taking up space
    registerButton.addClassName("primary");
    buttonBar.add(registerButton, metadataDownloadButton);
    buttonBar.addClassName("button-bar");
    //Moves buttonbar to right side of sample grid
    buttonAndFieldBar.add(fieldBar, buttonBar);
    buttonAndFieldBar.addClassName("button-and-search-bar");
  }

  private Grid<SamplePreview> createSampleGrid(Collection<SamplePreview> samplePreviews) {
    Grid<SamplePreview> sampleGrid = new Grid<>();
    sampleGrid.addColumn(createSampleIdComponentRenderer()).setHeader("Sample Id").setSortable(true)
        .setComparator(SamplePreview::sampleCode);
    sampleGrid.addColumn(SamplePreview::sampleLabel).setHeader("Sample Label").setSortable(true);
    sampleGrid.addColumn(SamplePreview::batchLabel).setHeader("Batch").setSortable(true);
    sampleGrid.addColumn(SamplePreview::sampleSource)
        .setHeader("Sample Source").setSortable(true);
    sampleGrid.addColumn(createConditionRenderer()).setHeader("Condition").setAutoWidth(true);
    sampleGrid.addColumn(SamplePreview::species).setHeader("Species").setSortable(true);
    sampleGrid.addColumn(SamplePreview::specimen).setSortable(true)
        .setHeader("Specimen").setSortable(true);
    sampleGrid.addColumn(SamplePreview::analyte).setHeader("Analyte").setSortable(true);
    sampleGrid.setItems(samplePreviews);
    return sampleGrid;
  }

  private static ComponentRenderer<Anchor, SamplePreview> createSampleIdComponentRenderer() {
    return new ComponentRenderer<>(Anchor::new, styleSampleIdAnchor);
  }

  private static final SerializableBiConsumer<Anchor, SamplePreview> styleSampleIdAnchor = (anchor, samplePreview) -> {
    String anchorURL = String.format(Projects.MEASUREMENT, projectId.value(),
        samplePreview.sampleId);
    anchor.setHref(anchorURL);
    anchor.setText(samplePreview.sampleCode);
  };

  private static ComponentRenderer<Div, SamplePreview> createConditionRenderer() {
    return new ComponentRenderer<>(Div::new, styleConditionValue);
  }

  private static final SerializableBiConsumer<Div, SamplePreview> styleConditionValue = (div, samplePreview) -> samplePreview.condition.forEach(
      (key, value) -> {
        div.addClassName("tag-collection");
        String experimentalVariable = key + ": " + value;
        Tag tag = new Tag(experimentalVariable);
        tag.setTitle(experimentalVariable);
        div.add(tag);
      });

  public void setStyles(String... componentStyles) {
    addClassNames(componentStyles);
  }

  public void setProject(ProjectId projectId) {
    SampleOverviewComponent.projectId = projectId;
  }


  public void setExperiments(Collection<Experiment> experiments) {
    sampleOverviewComponentHandler.setExperiments(experiments);
  }

  private record SamplePreview(String sampleCode, String sampleId, String batchLabel,
                               String sampleSource, String sampleLabel,
                               Map<String, String> condition,
                               String species, String specimen, String analyte) {

  }

  private final class SampleOverviewComponentHandler {

    private final transient BatchInformationService batchInformationService;
    private final transient SampleInformationService sampleInformationService;
    private final transient BatchRegistrationService batchRegistrationService;
    private final transient SampleRegistrationService sampleRegistrationService;

    public SampleOverviewComponentHandler(BatchInformationService batchInformationService,
        SampleInformationService sampleInformationService,
        BatchRegistrationService batchRegistrationService,
        SampleRegistrationService sampleRegistrationService) {
      this.batchInformationService = batchInformationService;
      this.sampleInformationService = sampleInformationService;
      this.batchRegistrationService = batchRegistrationService;
      this.sampleRegistrationService = sampleRegistrationService;
      addEventListeners();
    }

    public void setExperiments(Collection<Experiment> experiments) {
      resetContent();
      createSampleOverview(experiments);
    }

    private void createSampleOverview(Collection<Experiment> experiments) {
      resetSampleOverview();
      experiments.forEach(this::addExperimentTabToTabSheet);
      setExperimentsInSelect(experiments);
      setExperimentsInRegistrationDialog(experiments);
      content.add(buttonAndFieldBar);
      content.add(sampleExperimentTabSheet);
    }

    private void resetContent() {
      content.removeAll();
    }

    private void resetSampleOverview() {
      resetTabSheet();
      resetTabFilterSelect();
    }

    private void resetTabFilterSelect() {
      tabFilterSelect.removeAll();
    }

    private void resetTabSheet() {
      sampleExperimentTabSheet.getChildren()
          .forEach(component -> component.getElement().removeAllChildren());
    }


    private void setExperimentsInRegistrationDialog(Collection<Experiment> experiments) {
      List<Experiment> experimentsWithGroups = experiments.stream()
          .filter(experiment -> !experiment.getExperimentalGroups().isEmpty()).collect(
              Collectors.toList());
      batchRegistrationDialog.setExperiments(experimentsWithGroups);
    }

    private void addEventListeners() {
      batchRegistrationDialog.addBatchRegistrationEventListener(batchRegistrationEvent -> {
        BatchRegistrationDialog batchRegistrationSource = batchRegistrationEvent.getSource();
        registerBatchAndSamples(batchRegistrationSource.batchRegistrationContent(),
            batchRegistrationSource.sampleRegistrationContent()).onValue(batchId -> {
          batchRegistrationDialog.resetAndClose();
          displayRegistrationSuccess();
          //ToDo load new samples into dataProvider before reload
        });
      });
      registerButton.addClickListener(event -> batchRegistrationDialog.open());
      batchRegistrationDialog.addCancelEventListener(
          event -> batchRegistrationDialog.resetAndClose());
    }

    private void addExperimentTabToTabSheet(Experiment experiment) {
      Div experimentTabContent = new Div();
      SampleExperimentTab experimentTab = new SampleExperimentTab(experiment.getName(),
          0);
      if (!isExperimentGroupInExperiment(experiment)) {
        experimentTabContent.add(createNoGroupsDefinedDisclaimer(experiment));
      } else {
        Collection<SamplePreview> samplePreviews = retrieveSamplesForExperiment(experiment);
        if (samplePreviews.isEmpty()) {
          experimentTabContent.add(createNoSamplesRegisteredDisclaimer(experiment));
        } else {
          Grid<SamplePreview> sampleGrid = createSampleGrid(samplePreviews);
          experimentTab.setSampleCount(samplePreviews.size());
          //Update Number count in tab if user searches for value
          sampleGrid.getListDataView().addItemCountChangeListener(
              event -> experimentTab.setSampleCount(event.getItemCount()));
          //Make sampleGrid filterable via select component and searchbar
          sampleOverviewComponentHandler.setupSearchFieldForExperimentTabs(experiment.getName(),
              sampleGrid.getListDataView());
          experimentTabContent.add(sampleGrid);
        }
      }
      sampleExperimentTabSheet.add(experimentTab, experimentTabContent);
    }

    private boolean isExperimentGroupInExperiment(Experiment experiment) {
      return !experiment.getExperimentalGroups().isEmpty();
    }

    private DisclaimerCard createNoGroupsDefinedDisclaimer(Experiment experiment) {
      DisclaimerCard noGroupsDefinedCard = DisclaimerCard.createWithTitle(
          "No experimental groups defined",
          "Start the sample registration process by registering the first experimental group",
          "Add Experimental Group");
      String experimentId = experiment.experimentId().value();
      noGroupsDefinedCard.subscribe(event -> routeToExperimentalGroupCreation(event, experimentId));
      return noGroupsDefinedCard;
    }

    private void routeToExperimentalGroupCreation(ComponentEvent<?> componentEvent,
        String experimentId) {
      if (componentEvent.isFromClient()) {
        log.debug(String.format("Rerouting to experiment page for experiment %s of project %s",
            experimentId, projectId.value()));
        String routeToExperimentPage = String.format(Projects.EXPERIMENT, projectId.value(),
            experimentId);
        componentEvent.getSource().getUI().ifPresent(ui ->
            ui.navigate(routeToExperimentPage));
      }
    }

    private DisclaimerCard createNoSamplesRegisteredDisclaimer(Experiment experiment) {
      DisclaimerCard noSamplesDefinedCard = DisclaimerCard.createWithTitle("No samples registered",
          "Register your first samples for this experiment", "Register Samples");
      noSamplesDefinedCard.subscribe(event -> {
        batchRegistrationDialog.setSelectedExperiment(experiment);
        batchRegistrationDialog.open();
      });
      return noSamplesDefinedCard;
    }

    private Collection<SamplePreview> retrieveSamplesForExperiment(Experiment experiment) {
      return sampleInformationService.retrieveSamplesForExperiment(experiment.experimentId())
          .onError(error -> displaySampleRetrievalError(experiment.getName()))
          .fold(
              samples -> samples.stream()
                  .map(sample -> mapSampleToSamplePreview(experiment, sample)).toList(),
              error -> new ArrayList<>());
    }

    private SamplePreview mapSampleToSamplePreview(Experiment experiment, Sample sample) {
      String batchLabel = getBatchLabel(sample);
      String biologicalReplicateLabel = getSampleReplicateLabelInExperiment(experiment, sample);
      Map<String, String> conditions = getConditionOfExperimentalGroup(experiment, sample);
      return new SamplePreview(sample.sampleCode().code(), sample.sampleId().value(), batchLabel,
          biologicalReplicateLabel,
          sample.label(), conditions,
          sample.sampleOrigin().getSpecies().value(),
          sample.sampleOrigin().getSpecimen().value(),
          sample.sampleOrigin().getAnalyte().value());
    }

    private void displaySampleRetrievalError(String experimentName) {
      ErrorMessage errorMessage = new ErrorMessage("Sample Retrieval Error",
          "Samples for experiment: " + experimentName + "could not be retrieved."
              + "Please try again by reloading this page");
      StyledNotification notification = new StyledNotification(errorMessage);
      notification.open();
    }


    private String getBatchLabel(Sample sample) {
      Optional<Batch> foundBatch = batchInformationService.find(sample.assignedBatch());
      if (foundBatch.isPresent()) {
        return foundBatch.get().label();
      } else {
        return "---";
      }
    }

    private String getSampleReplicateLabelInExperiment(Experiment experiment, Sample sample) {
      Set<BiologicalReplicate> biologicalReplicateSet = new HashSet<>();
      for (ExperimentalGroup experimentalGroup : experiment.getExperimentalGroups()) {
        biologicalReplicateSet.addAll(experimentalGroup.biologicalReplicates());
      }
      Optional<BiologicalReplicate> foundReplicate = biologicalReplicateSet.stream().filter(
              biologicalReplicate -> biologicalReplicate.id().equals(sample.getBiologicalReplicateId()))
          .findFirst();
      if (foundReplicate.isPresent()) {
        return foundReplicate.get().label();
      } else {
        return "---";
      }
    }

    private Map<String, String> getConditionOfExperimentalGroup(Experiment experiment,
        Sample sample) {
      Optional<ExperimentalGroup> foundExperimentalGroup = experiment.getExperimentalGroups()
          .stream()
          .filter(experimentalGroup -> experimentalGroup.id() == sample.getExperimentalGroupId())
          .findFirst();
      TreeMap<String, String> conditionMap = new TreeMap<>();
      if (foundExperimentalGroup.isPresent()) {
        for (VariableLevel variableLevel : foundExperimentalGroup.get().condition()
            .getVariableLevels()) {
          String variableName = variableLevel.variableName().value();
          String experimentalValueUnit = variableLevel.experimentalValue().unit().orElse("");
          String experimentalValueName = variableLevel.experimentalValue().value();
          conditionMap.put(variableName,
              String.join(" ", experimentalValueName, experimentalValueUnit).trim());
        }
      }
      return conditionMap;
    }

    private void setExperimentsInSelect(Collection<Experiment> experimentList) {
      tabFilterSelect.removeAll();
      tabFilterSelect.setItems(experimentList.stream().map(Experiment::getName).toList());
    }

    private void setupSearchFieldForExperimentTabs(String experimentName,
        GridListDataView<SamplePreview> sampleGridDataView) {
      searchField.addValueChangeListener(e -> sampleGridDataView.refreshAll());
      sampleGridDataView.addFilter(samplePreview -> {
        String searchTerm = searchField.getValue().trim();
        //Only filter grid if selected in filterSelect or if no filter was selected
        if (tabFilterSelect.getValue() == null || tabFilterSelect.getValue()
            .equals(experimentName)) {
          return isInSample(samplePreview, searchTerm);
        } else {
          return true;
        }
      });
    }

    private boolean isInSample(SamplePreview samplePreview, String searchTerm) {
      boolean result = false;
      for (PropertyDescriptor descriptor : BeanUtils.getPropertyDescriptors(SamplePreview.class)) {
        if (!descriptor.getName().equals("class")) {
          try {
            String value = descriptor.getReadMethod().invoke(samplePreview).toString();
            result |= matchesTerm(value, searchTerm);
          } catch (IllegalAccessException | InvocationTargetException e) {
            log.info("Could not invoke " + descriptor.getName()
                + " getter when filtering samples. Ignoring property.");
          }
        }
      }
      return result;
    }

    private boolean matchesTerm(String fieldValue, String searchTerm) {
      return fieldValue.toLowerCase().contains(searchTerm.toLowerCase());
    }

    private Result<?, ?> registerBatchAndSamples(BatchRegistrationContent batchRegistrationContent,
        List<SampleRegistrationContent> sampleRegistrationContent) {
      return registerBatchInformation(batchRegistrationContent).onValue(
          batchId -> {
            List<SampleRegistrationRequest> sampleRegistrationsRequests = createSampleRegistrationRequests(
                batchId, batchRegistrationContent.experimentId(), sampleRegistrationContent);
            registerSamples(sampleRegistrationsRequests);
          });
    }

    private Result<BatchId, ResponseCode> registerBatchInformation(
        BatchRegistrationContent batchRegistrationContent) {
      return batchRegistrationService.registerBatch(batchRegistrationContent.batchLabel(),
          batchRegistrationContent.isPilot()).onError(responseCode -> displayRegistrationFailure());
    }

    private List<SampleRegistrationRequest> createSampleRegistrationRequests(BatchId batchId,
        ExperimentId experimentId,
        List<SampleRegistrationContent> sampleRegistrationContents) {
      return sampleRegistrationContents.stream()
          .map(sampleRegistrationContent -> {
            Analyte analyte = new Analyte(sampleRegistrationContent.analyte());
            Specimen specimen = new Specimen(sampleRegistrationContent.specimen());
            Species species = new Species(sampleRegistrationContent.species());
            SampleOrigin sampleOrigin = SampleOrigin.create(species, specimen, analyte);
            return new SampleRegistrationRequest(sampleRegistrationContent.label(), batchId,
                experimentId,
                sampleRegistrationContent.experimentalGroupId(),
                sampleRegistrationContent.biologicalReplicateId(), sampleOrigin);
          }).toList();
    }

    private void registerSamples(List<SampleRegistrationRequest> sampleRegistrationRequests) {
      sampleRegistrationService.registerSamples(sampleRegistrationRequests, projectId)
          .onError(responseCode -> displayRegistrationFailure());
    }

    private void displayRegistrationSuccess() {
      SuccessMessage successMessage = new SuccessMessage("Batch registration succeeded.", "");
      StyledNotification notification = new StyledNotification(successMessage);
      notification.open();
    }

    private void displayRegistrationFailure() {
      ErrorMessage errorMessage = new ErrorMessage("Batch registration failed.", "");
      StyledNotification notification = new StyledNotification(errorMessage);
      notification.open();
    }

    private class SampleExperimentTab extends Tab {

      private final Span sampleCountComponent;
      private final Span experimentNameComponent;

      public SampleExperimentTab(String experimentName, int sampleCount) {
        this.experimentNameComponent = new Span(experimentName);
        this.sampleCountComponent = createBadge(sampleCount);
        this.add(experimentNameComponent, sampleCountComponent);
      }

      public String getExperimentName() {
        return experimentNameComponent.getText();
      }

      public void setExperimentName(String experimentName) {
        experimentNameComponent.setText(experimentName);
      }

      public int getSampleCount() {
        return Integer.parseInt(sampleCountComponent.getText());
      }

      public void setSampleCount(int sampleCount) {
        sampleCountComponent.setText(Integer.toString(sampleCount));
      }

      /**
       * Helper method for creating a badge.
       */
      private Span createBadge(int numberOfSamples) {
        //ToDo Set styling in css
        Span badge = new Span(String.valueOf(numberOfSamples));
        badge.getElement().getThemeList().add("badge small contrast");
        badge.getStyle().set("margin-inline-start", "var(--lumo-space-xs)");
        return badge;
      }
    }
  }
}
