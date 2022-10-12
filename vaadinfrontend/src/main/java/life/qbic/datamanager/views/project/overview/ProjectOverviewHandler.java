package life.qbic.datamanager.views.project.overview;

import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.QueryParameters;

import java.util.Objects;

import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.Result;
import life.qbic.datamanager.views.Command;
import life.qbic.datamanager.views.components.StyledNotification;
import life.qbic.datamanager.views.components.SuccessMessage;
import life.qbic.projectmanagement.application.ProjectCreationService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.finances.offer.OfferLookupService;
import life.qbic.projectmanagement.domain.finances.offer.OfferPreview;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * <b>Handler</b>
 *
 * <p>Orchestrates the layout {@link ProjectOverviewLayout} and determines how the components
 * behave.</p>
 *
 * @since 1.0.0
 */
@Component
public class ProjectOverviewHandler implements ProjectOverviewHandlerInterface {

  private static final String PROJECT_CREATION_URL = "projects/create";

  private static final String OFFER_ID_QUERY_PARAMETER = "offerId";

  private static final String QUERY_PARAMETER_SEPARATOR = "=";
  private ProjectOverviewLayout registeredProjectOverview;
  private final OfferLookupService offerLookupService;
  private final ProjectCreationService projectCreationService;

  private final ProjectRepository projectRepository;

  private final ProjectInformationService projectInformationService;


  public ProjectOverviewHandler(@Autowired OfferLookupService offerLookupService,
                                @Autowired ProjectRepository projectRepository,
                                @Autowired ProjectInformationService projectInformationService, @Autowired ProjectCreationService projectCreationService) {
    Objects.requireNonNull(offerLookupService);
    this.offerLookupService = offerLookupService;

    Objects.requireNonNull(projectRepository);
    this.projectRepository = projectRepository;

    Objects.requireNonNull(projectInformationService);
    this.projectInformationService = projectInformationService;

    Objects.requireNonNull(projectCreationService);
    this.projectCreationService = projectCreationService;
  }

  @Override
  public void handle(ProjectOverviewLayout layout) {
    if (registeredProjectOverview != layout) {
      this.registeredProjectOverview = layout;

      configurePageButtons();
      configureProjectCreationDialog();

      setProjectsToGrid();
      setupSearchBar();
    }
  }

  private void setupSearchBar() {
    registeredProjectOverview.projectSearchField.setValueChangeMode(ValueChangeMode.LAZY);
    registeredProjectOverview.projectSearchField
        .addValueChangeListener(event -> loadProjectPreview(event.getValue()).execute());
  }

  private Command loadProjectPreview() {
    var searchTerm = registeredProjectOverview.projectSearchField.getValue().trim();
    return loadProjectPreview(searchTerm);
  }

  private Command loadProjectPreview(String filter) {
    return () -> registeredProjectOverview.projectGrid.setItems(
        query -> projectInformationService.queryPreview(filter,
            query.getOffset(), query.getLimit()).stream());
  }

  private void setProjectsToGrid() {
    loadProjectPreview().execute();
  }

  private void configurePageButtons() {
    registeredProjectOverview.create.addClickListener(
        e -> registeredProjectOverview.projectInformationDialog.open());

  }

  private void configureProjectCreationDialog() {
    registeredProjectOverview.projectInformationDialog.saveButton.addClickListener(
        e -> saveClicked());
    registeredProjectOverview.projectInformationDialog.cancelButton.addClickListener(
        e -> cancelSelection().execute());
    registeredProjectOverview.projectInformationDialog.isCloseOnEsc();
  }

  private Command cancelSelection() {
    return () -> {
      registeredProjectOverview.projectInformationDialog.close();
      registeredProjectOverview.projectInformationDialog.reset();
    };
  }

  private void saveClicked() {
    String titleFieldValue = registeredProjectOverview.projectInformationDialog.getTitle();
    String objectiveFieldValue = registeredProjectOverview.projectInformationDialog.getObjective();
    String experimentalDesignDescription = registeredProjectOverview.projectInformationDialog.getExperimentalDesign();
    //String loadedOfferId = registeredProjectOverview.projectInformationDialog.linkedOffers().stream()
    //    .findFirst().orElse(null);
    Result<Project, ApplicationException> project = projectCreationService.createProject(
        titleFieldValue, objectiveFieldValue, experimentalDesignDescription, null);
    //todo
    /*project.ifSuccessOrElse(
        result -> displaySuccessfulProjectCreationNotification(),
        applicationException -> exceptionHandler.handle(UI.getCurrent(), applicationException));*/
    registeredProjectOverview.projectInformationDialog.close();
  }

  private void displaySuccessfulProjectCreationNotification() {
    SuccessMessage successMessage = new SuccessMessage("Project creation succeeded.", "");
    StyledNotification notification = new StyledNotification(successMessage);
    notification.open();
  }

  private Command navigateToProjectPage(OfferPreview offerPreview) {
    return () -> {
      registeredProjectOverview.projectInformationDialog.close();
      //QueryParameters queryParameters = QueryParameters.fromString(
      //OFFER_ID_QUERY_PARAMETER + QUERY_PARAMETER_SEPARATOR + offerPreview.offerId().id());
      //todo load generated project here
      //UI.getCurrent().navigate(PROJECT_CREATION_URL, queryParameters);
    };
  }

  private void loadOfferPreview() {
    // Configure the filter and pagination for the lazy loaded OfferPreview items
    registeredProjectOverview.searchDialog.searchField.setItems(
        query -> offerLookupService.findOfferContainingProjectTitleOrId(
            query.getFilter().orElse(""),
            query.getFilter().orElse(""), query.getOffset(), query.getLimit()).stream());

    // Render the preview
    registeredProjectOverview.searchDialog.searchField.setRenderer(
        new ComponentRenderer<>(preview ->
            new Text(previewToString(preview))));

    // Generate labels like the rendering
    registeredProjectOverview.searchDialog.searchField.setItemLabelGenerator(
        (ItemLabelGenerator<OfferPreview>) ProjectOverviewHandler::previewToString);
  }

  /**
   * Render the preview like `#offer-id, #project title`
   *
   * @param offerPreview the offer preview
   * @return the formatted String representation
   * @since 1.0.0
   */
  private static String previewToString(OfferPreview offerPreview) {
    return offerPreview.offerId().id() + ", " + offerPreview.getProjectTitle().title();
  }


}
