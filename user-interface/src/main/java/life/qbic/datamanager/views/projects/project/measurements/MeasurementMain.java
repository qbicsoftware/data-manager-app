package life.qbic.datamanager.views.projects.project.measurements;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.util.Objects;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.InfoBox;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.general.download.MeasurementTemplateDownload;
import life.qbic.datamanager.views.projects.overview.ProjectOverviewMain;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentMainLayout;
import life.qbic.datamanager.views.projects.project.measurements.MeasurementTemplateListComponent.DownloadMeasurementTemplateEvent;
import life.qbic.datamanager.views.projects.project.samples.SampleInformationMain;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.measurement.MeasurementService;
import life.qbic.projectmanagement.application.measurement.MeasurementService.MeasurementRegistrationException;
import life.qbic.projectmanagement.application.measurement.validation.ValidationService;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Measurement Main Component
 * <p>
 * This component hosts the components necessary to show and update the Measurement information
 * associated with an {@link Experiment} within a {@link Project} via the provided
 * {@link ExperimentId} and {@link ProjectId} in the URL
 */

@SpringComponent
@UIScope
@Route(value = "projects/:projectId?/experiments/:experimentId?/measurements", layout = ExperimentMainLayout.class)
@PermitAll
public class MeasurementMain extends Main implements BeforeEnterObserver {

  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  public static final String EXPERIMENT_ID_ROUTE_PARAMETER = "experimentId";
  @Serial
  private static final long serialVersionUID = 3778218989387044758L;
  private static final Logger log = LoggerFactory.logger(SampleInformationMain.class);
  private final MeasurementTemplateDownload measurementTemplateDownload;
  private final MeasurementDetailsComponent measurementDetailsComponent;
  private final TextField measurementSearchField = new TextField();
  private final transient MeasurementService measurementService;
  private final transient ValidationService validationService;
  private transient Context context;
  private final Div content = new Div();
  private final InfoBox rawDataAvailableInfo = new InfoBox();

  public MeasurementMain(
      @Autowired MeasurementTemplateListComponent measurementTemplateListComponent,
      @Autowired MeasurementDetailsComponent measurementDetailsComponent,
      @Autowired MeasurementService measurementService,
      @Autowired ValidationService validationService) {
    Objects.requireNonNull(measurementTemplateListComponent);
    Objects.requireNonNull(measurementDetailsComponent);
    Objects.requireNonNull(measurementService);
    Objects.requireNonNull(validationService);
    this.measurementDetailsComponent = measurementDetailsComponent;
    this.measurementService = measurementService;
    this.validationService = validationService;
    measurementTemplateDownload = new MeasurementTemplateDownload();
    measurementTemplateListComponent.addDownloadMeasurementTemplateClickListener(
        this::onDownloadMeasurementTemplateClicked);
    initContent();
    add(measurementTemplateListComponent);
    add(measurementTemplateDownload);
    add(measurementDetailsComponent);
    addClassName("measurement");
    measurementDetailsComponent.addRegisterMeasurementClickedListener(
        event -> openRegisterMeasurementDialog());
    log.debug(String.format(
        "New instance for %s(#%s) created with %s(#%s)",
        getClass().getSimpleName(), System.identityHashCode(this),
        measurementTemplateListComponent.getClass().getSimpleName(),
        System.identityHashCode(measurementTemplateListComponent)));
  }

  private void initContent() {
    Span titleField = new Span();
    titleField.setText("Register Measurements");
    titleField.addClassNames("title");
    content.add(titleField);
    initRawDataAvailableInfo();
    initSearchFieldAndButtonBar();
    add(content);
    content.addClassName("measurement-main-content");
  }

  private void initSearchFieldAndButtonBar() {
    measurementSearchField.setPlaceholder("Search");
    measurementSearchField.setClearButtonVisible(true);
    measurementSearchField.setSuffixComponent(VaadinIcon.SEARCH.create());
    measurementSearchField.addClassNames("search-field");
    measurementSearchField.setValueChangeMode(ValueChangeMode.LAZY);
    measurementSearchField.addValueChangeListener(
        event -> measurementDetailsComponent.setSearchedMeasurementValue((event.getValue())));
    Button registerMeasurementButton = new Button("Register Measurements");
    registerMeasurementButton.addClassName("primary");
    registerMeasurementButton.addClickListener(
        event -> openRegisterMeasurementDialog());
    Span buttonAndField = new Span(measurementSearchField, registerMeasurementButton);
    buttonAndField.addClassName("buttonAndField");
    content.add(buttonAndField);
  }

  /**
   * Callback executed before navigation to attaching Component chain is made.
   *
   * @param event before navigation event with event details
   */
  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    String projectID = event.getRouteParameters().get(PROJECT_ID_ROUTE_PARAMETER)
        .orElseThrow();
    if (!ProjectId.isValid(projectID)) {
      throw new ApplicationException("invalid project id " + projectID);
    }
    ProjectId parsedProjectId = ProjectId.parse(projectID);
    context = new Context().with(parsedProjectId);
    String experimentId = event.getRouteParameters().get(EXPERIMENT_ID_ROUTE_PARAMETER)
        .orElseThrow();
    if (!ExperimentId.isValid(experimentId)) {
      throw new ApplicationException("invalid experiment id " + experimentId);
    }
    ExperimentId parsedExperimentId = ExperimentId.parse(experimentId);
    this.context = context.with(parsedExperimentId);
    measurementDetailsComponent.setExperimentId(parsedExperimentId);
    isRawDataAvailable();
  }

  private void isRawDataAvailable() {
    /*Todo check for raw data if available*/
    rawDataAvailableInfo.setVisible(false);
  }

  private void onDownloadMeasurementTemplateClicked(
      DownloadMeasurementTemplateEvent downloadMeasurementTemplateEvent) {
    measurementTemplateDownload.trigger(downloadMeasurementTemplateEvent.measurementTemplate());
  }

  private void openRegisterMeasurementDialog() {
    var dialog = new MeasurementMetadataUploadDialog(validationService,
        context.experimentId().orElseThrow());
    dialog.addCancelListener(cancelEvent -> cancelEvent.getSource().close());
    dialog.addConfirmListener(confirmEvent -> {
      var uploads = confirmEvent.uploads();
      boolean allSuccessfull = true;
      for (var upload : uploads) {
        try {
          measurementService.registerMultiple(upload.measurementMetadata());
        } catch (MeasurementRegistrationException measurementRegistrationException) {
          allSuccessfull = false;
          String errorMessage = switch (measurementRegistrationException.reason()) {
            case FAILED, SUCCESSFUL -> "Registration failed. Please try again.";
            case UNKNOWN_ORGANISATION_ROR_ID -> "Could not resolve ROR identifier.";
            case UNKNOWN_ONTOLOGY_TERM -> "Encountered unknown ontology term.";
            case WRONG_EXPERIMENT -> "There are samples that do not belong to this experiment.";
          };
          confirmEvent.getSource().showError(upload.fileName(), errorMessage);
          continue;
        }
        confirmEvent.getSource().markSuccessful(upload.fileName());
      }
      if (allSuccessfull) {
        measurementDetailsComponent.setExperimentId(context.experimentId().orElseThrow());
        confirmEvent.getSource().close();
      }
    });
    dialog.open();
  }

  private void initRawDataAvailableInfo() {
    rawDataAvailableInfo.setInfoText(
        "Raw data results for your registered measurement are available now");
    Button navigateToDownloadRawDataButton = new Button("Go to Download Raw data");
    //ToDo Replace with Raw Data Main Class as soon as it's written
    navigateToDownloadRawDataButton.addClickListener(event -> UI.getCurrent().navigate(
        ProjectOverviewMain.class));
    navigateToDownloadRawDataButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
    rawDataAvailableInfo.add(navigateToDownloadRawDataButton);
    rawDataAvailableInfo.setClosable(true);
    content.add(rawDataAvailableInfo);
    rawDataAvailableInfo.setVisible(false);
  }
}
