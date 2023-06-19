package life.qbic.datamanager.views.projects.project.samples;

import static org.slf4j.LoggerFactory.getLogger;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoUtility.Display;
import com.vaadin.flow.theme.lumo.LumoUtility.FlexWrap;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin.Bottom;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin.Right;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin.Top;
import java.beans.PropertyDescriptor;
import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import life.qbic.datamanager.views.AppRoutes.Projects;
import life.qbic.datamanager.views.notifications.ErrorMessage;
import life.qbic.datamanager.views.notifications.StyledNotification;
import life.qbic.datamanager.views.projects.project.ProjectViewPage;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.BatchRegistrationDialog;
import life.qbic.projectmanagement.application.SampleInformationService;
import life.qbic.projectmanagement.application.batch.BatchInformationService;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.experiment.BiologicalReplicate;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.project.experiment.VariableLevel;
import life.qbic.projectmanagement.domain.project.sample.Batch;
import life.qbic.projectmanagement.domain.project.sample.Sample;
import org.slf4j.Logger;
import org.springframework.beans.BeanUtils;

/**
 * Sample Overview Component
 * <p>
 * Component embedded within the {@link SampleInformationPage} in the {@link ProjectViewPage}. It
 * allows the user to see the information associated for all {@link Sample} for each
 * {@link Experiment within a {@link life.qbic.projectmanagement.domain.project.Project}
 */

@SpringComponent
@UIScope
public class SampleOverviewComponent extends Div implements Serializable {

  @Serial
  private static final long serialVersionUID = 2893730975944372088L;
  private final VerticalLayout sampleContentLayout = new VerticalLayout();
  private final HorizontalLayout buttonAndFieldBar = new HorizontalLayout();
  private final HorizontalLayout fieldBar = new HorizontalLayout();
  private final HorizontalLayout buttonBar = new HorizontalLayout();
  private final BatchRegistrationDialog batchRegistrationDialog = new BatchRegistrationDialog();
  private final TextField searchField = new TextField();
  private final Select<String> tabFilterSelect = new Select<>();
  public final Button registerButton = new Button("Register");
  private final Button metadataDownloadButton = new Button("Download Metadata");
  private final TabSheet sampleExperimentTabSheet = new TabSheet();
  private static final Logger log = getLogger(SampleOverviewComponent.class);
  private static ProjectId projectId;
  private final transient SampleOverviewComponentHandler sampleOverviewComponentHandler;

  public SampleOverviewComponent(BatchInformationService batchInformationService,
      SampleInformationService sampleInformationService) {
    Objects.requireNonNull(sampleInformationService);
    Objects.requireNonNull(batchInformationService);
    initSampleView();
    this.sampleOverviewComponentHandler = new SampleOverviewComponentHandler(
        batchInformationService, sampleInformationService);
  }

  private void initSampleView() {
    this.setSizeFull();
    this.addClassName("sample-overview-component");
    sampleExperimentTabSheet.setSizeFull();
    initButtonAndFieldBar();
    sampleContentLayout.add(buttonAndFieldBar);
    sampleContentLayout.add(sampleExperimentTabSheet);
    add(sampleContentLayout);
    sampleContentLayout.setSizeFull();
  }

  private void initButtonAndFieldBar() {
    searchField.setPlaceholder("Search");
    searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
    searchField.setValueChangeMode(ValueChangeMode.EAGER);
    tabFilterSelect.setLabel("Search in");
    tabFilterSelect.setEmptySelectionAllowed(true);
    tabFilterSelect.setEmptySelectionCaption("All tabs");
    registerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    fieldBar.add(searchField, tabFilterSelect);
    //Items in layout should be aligned at the end due to searchFieldLabel taking up space
    buttonBar.add(registerButton, metadataDownloadButton);
    fieldBar.setAlignSelf(Alignment.START, buttonAndFieldBar);
    buttonBar.setAlignSelf(Alignment.END, buttonAndFieldBar);
    fieldBar.setAlignItems(Alignment.END);
    buttonBar.setAlignItems(Alignment.END);
    //Moves buttonbar to right side of sample grid
    fieldBar.setWidthFull();
    buttonAndFieldBar.add(fieldBar, buttonBar);
    buttonAndFieldBar.setWidthFull();
  }

  private Grid<SamplePreview> createSampleGrid() {
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
        String experimentalVariable = key + ": " + value;
        Span tag = new Span(experimentalVariable);
        tag.setTitle(experimentalVariable);
        tag.getElement().getThemeList().add("badge");
        tag.addClassName(Right.XSMALL);
        tag.addClassName(Bottom.XSMALL);
        tag.addClassName(Top.XSMALL);
        div.addClassName(FlexWrap.WRAP);
        div.addClassName(Display.INLINE_FLEX);
        div.add(tag);
      });

  public void setStyles(String... componentStyles) {
    addClassNames(componentStyles);
  }

  public void setProjectId(ProjectId projectId) {
    SampleOverviewComponent.projectId = projectId;
  }

  public void setExperiments(Collection<Experiment> experiments) {
    sampleOverviewComponentHandler.setExperiments(experiments);
    batchRegistrationDialog.setExperiments(experiments);
  }

  private record SamplePreview(String sampleCode, String sampleId, String batchLabel,
                               String sampleSource, String sampleLabel,
                               Map<String, String> condition,
                               String species, String specimen, String analyte) {

  }

  private final class SampleOverviewComponentHandler {

    private final transient BatchInformationService batchInformationService;
    private final transient SampleInformationService sampleInformationService;

    public SampleOverviewComponentHandler(BatchInformationService batchInformationService,
        SampleInformationService sampleInformationService) {
      this.batchInformationService = batchInformationService;
      this.sampleInformationService = sampleInformationService;
    }

    public void setExperiments(Collection<Experiment> experiments) {
      resetTabSheet();
      resetTabSelect();
      experiments.forEach(this::generateExperimentTab);
      addExperimentsToTabSelect(experiments);
    }

    private void generateExperimentTab(Experiment experiment) {
      Collection<SamplePreview> samplePreviews = retrieveSamplesForExperiment(
          experiment);
      SampleExperimentTab experimentTab = new SampleExperimentTab(experiment.getName(),
          samplePreviews.size());
      Grid<SamplePreview> sampleGrid = createSampleGrid();
      GridListDataView<SamplePreview> sampleGridDataView = sampleGrid.setItems(samplePreviews);
      sampleOverviewComponentHandler.setupSearchFieldForExperimentTabs(experiment.getName(),
          sampleGridDataView);
      sampleExperimentTabSheet.add(experimentTab, sampleGrid);
      //Update Number count in tab if user searches for value
      sampleGridDataView.addItemCountChangeListener(
          event -> experimentTab.setSampleCount(event.getItemCount()));
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

    private void resetTabSelect() {
      tabFilterSelect.removeAll();
    }

    private void resetTabSheet() {
      sampleExperimentTabSheet.getChildren()
          .forEach(component -> component.getElement().removeAllChildren());
    }

    private void addExperimentsToTabSelect(Collection<Experiment> experimentList) {
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
        Span badge = new Span(String.valueOf(numberOfSamples));
        badge.getElement().getThemeList().add("badge small contrast");
        badge.getStyle().set("margin-inline-start", "var(--lumo-space-xs)");
        return badge;
      }
    }
  }
}
