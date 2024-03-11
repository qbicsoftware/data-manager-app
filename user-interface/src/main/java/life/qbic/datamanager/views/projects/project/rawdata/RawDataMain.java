package life.qbic.datamanager.views.projects.project.rawdata;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
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
import life.qbic.datamanager.views.account.PersonalAccessTokenMain;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentMainLayout;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.measurement.MeasurementMetadata;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Raw Data Main Component
 * <p>
 * This component hosts the components necessary to show and download the raw data
 * associated with the {@link MeasurementMetadata} within an {@link Experiment} via the provided
 * {@link ExperimentId} and {@link ProjectId} in the URL
 */

@SpringComponent
@UIScope
@Route(value = "projects/:projectId?/experiments/:experimentId?/rawdata", layout = ExperimentMainLayout.class)
@PermitAll
public class RawDataMain extends Main implements BeforeEnterObserver {

  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  public static final String EXPERIMENT_ID_ROUTE_PARAMETER = "experimentId";
  @Serial
  private static final long serialVersionUID = -4506659645977994192L;
  private static final Logger log = LoggerFactory.logger(RawDataMain.class);
  private final RawDataDetailsComponent rawdataDetailsComponent;
  private final RawDataDownloadInformationComponent rawDataDownloadInformationComponent;
  private final TextField rawDataSearchField = new TextField();
  private transient Context context;
  private final Div content = new Div();

  public RawDataMain(@Autowired RawDataDetailsComponent rawDataDetailsComponent,
                     @Autowired RawDataDownloadInformationComponent rawDataDownloadInformationComponent) {

    this.rawdataDetailsComponent = Objects.requireNonNull(rawDataDetailsComponent);
    this.rawDataDownloadInformationComponent = Objects.requireNonNull(rawDataDownloadInformationComponent);
    initContent();
    add(rawDataDetailsComponent);
    add(rawDataDownloadInformationComponent);
    addListeners();
    addClassName("raw-data");
    log.debug(String.format(
        "New instance for %s(#%s) created with %s(#%s)",
        getClass().getSimpleName(), System.identityHashCode(this),
        rawdataDetailsComponent.getClass().getSimpleName(),
        System.identityHashCode(rawDataDetailsComponent)));
  }

  private void initContent() {
    Span titleField = new Span();
    titleField.setText("Download Raw Data");
    titleField.addClassNames("title");
    content.add(titleField);
    initSearchFieldAndButtonBar();
    add(content);
    content.addClassName("raw-data-main-content");
  }

  private void addListeners() {
    rawDataDownloadInformationComponent.addDownloadUrlListener(event -> handleUrlDownload());
    rawDataDownloadInformationComponent.addPersonalAccessTokenNavigationListener(
        event -> UI.getCurrent().navigate(
            PersonalAccessTokenMain.class));
  }

  private void initSearchFieldAndButtonBar() {
    rawDataSearchField.setPlaceholder("Search");
    rawDataSearchField.setClearButtonVisible(true);
    rawDataSearchField.setSuffixComponent(VaadinIcon.SEARCH.create());
    rawDataSearchField.addClassNames("search-field");
    rawDataSearchField.setValueChangeMode(ValueChangeMode.LAZY);
    rawDataSearchField.addValueChangeListener(
        event -> rawdataDetailsComponent.setSearchedRawDataValue((event.getValue())));
    Button downloadRawDataUrl = new Button("Download URL list");
    downloadRawDataUrl.addClassName("primary");
    downloadRawDataUrl.addClickListener(event -> handleUrlDownload());
    Span buttonAndField = new Span(rawDataSearchField, downloadRawDataUrl);
    buttonAndField.addClassName("buttonAndField");
    content.add(buttonAndField);
  }

  //ToDo Implement
  private void handleUrlDownload(){}

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
    rawdataDetailsComponent.setContext(context);
    //Todo Handle empty state
  }
}
