package life.qbic.datamanager.views.projects.overview.components;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridLazyDataView;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LocalDateTimeRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.Result;
import life.qbic.datamanager.ClientDetailsProvider;
import life.qbic.datamanager.ClientDetailsProvider.ClientDetails;
import life.qbic.datamanager.views.AppRoutes.Projects;
import life.qbic.datamanager.views.layouts.CardLayout;
import life.qbic.datamanager.views.notifications.StyledNotification;
import life.qbic.datamanager.views.notifications.SuccessMessage;
import life.qbic.datamanager.views.projects.create.ProjectCreationContent;
import life.qbic.datamanager.views.projects.create.ProjectCreationDialog;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.ExperimentalDesignSearchService;
import life.qbic.projectmanagement.application.PersonSearchService;
import life.qbic.projectmanagement.application.ProjectRegistrationService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.ProjectPreview;
import life.qbic.projectmanagement.application.SortOrder;
import life.qbic.projectmanagement.application.finances.offer.OfferLookupService;
import life.qbic.projectmanagement.domain.finances.offer.Offer;
import life.qbic.projectmanagement.domain.finances.offer.OfferId;
import life.qbic.projectmanagement.domain.finances.offer.OfferPreview;
import life.qbic.projectmanagement.domain.project.PersonReference;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <b>Projects Overview</b>
 *
 * <p>The page the user is navigated to after successful login</p>
 *
 * @since 1.0.0
 */
@SpringComponent
@UIScope
public class ProjectOverviewComponent extends Composite<CardLayout> {

  @Serial
  private static final long serialVersionUID = 5435551053955979169L;
  final Button create = new Button("Create");
  final TextField projectSearchField = new TextField();
  final Grid<ProjectPreview> projectGrid = new Grid<>(ProjectPreview.class, false);
  final ProjectCreationDialog projectCreationDialog;
  private final ClientDetailsProvider clientDetailsProvider;

  public ProjectOverviewComponent(@Autowired ClientDetailsProvider clientDetailsProvider,
      @Autowired OfferLookupService offerLookupService,
      @Autowired ProjectRepository projectRepository,
      @Autowired ProjectInformationService projectInformationService,
      @Autowired ProjectRegistrationService projectRegistrationService,
      @Autowired PersonSearchService personSearchService,
      @Autowired ExperimentalDesignSearchService experimentalDesignSearchService) {
    this.clientDetailsProvider = clientDetailsProvider;
    this.projectCreationDialog = new ProjectCreationDialog(experimentalDesignSearchService);
    new Handler(offerLookupService, projectRepository, projectInformationService,
        projectRegistrationService, personSearchService);
    layoutComponents();
  }

  private void layoutComponents() {
    HorizontalLayout layout = new HorizontalLayout();
    create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    create.addClassNames("mt-s", "mb-s");

    projectSearchField.setPlaceholder("Search");
    projectSearchField.setClearButtonVisible(true);
    projectSearchField.setPrefixComponent(VaadinIcon.SEARCH.create());
    projectSearchField.addClassNames("mt-xs", "mb-xs");

    layout.add(projectSearchField, create);
    layout.setWidthFull();
    layout.setVerticalComponentAlignment(FlexComponent.Alignment.END, create);
    layout.setVerticalComponentAlignment(FlexComponent.Alignment.START, projectSearchField);
    projectGrid.addColumn(new ComponentRenderer<>(
        item -> new Anchor(String.format(Projects.PROJECT_INFO, item.projectId().value()),
            item.projectCode()))).setHeader("Code").setWidth("7em").setFlexGrow(0);

    projectGrid.addColumn(new ComponentRenderer<>(
        item -> new Anchor(String.format(Projects.PROJECT_INFO, item.projectId().value()),
            item.projectTitle()))).setHeader("Title").setKey("projectTitle");

    projectGrid.addColumn(new LocalDateTimeRenderer<>(
        projectPreview -> asClientLocalDateTime(projectPreview.lastModified()),
        "yyyy-MM-dd HH:mm:ss")).setKey("lastModified").setHeader("Last Modified");

    projectGrid.setMultiSort(true);
    getContent().addFields(layout, projectGrid);
  }

  private LocalDateTime asClientLocalDateTime(Instant instant) {
    String clientTimeZone = clientDetailsProvider.latestDetails().map(ClientDetails::timeZoneId)
        .orElse("UTC");
    ZonedDateTime zonedDateTime = instant.atZone(ZoneId.of(clientTimeZone));
    return zonedDateTime.toLocalDateTime();
  }

  /**
   * <b>Handler</b>
   *
   * <p>Orchestrates the layout {@link ProjectOverviewComponent} and determines how the components
   * behave.</p>
   *
   * @since 1.0.0
   */

  private class Handler {

    private static final Logger log = logger(Handler.class);
    private final OfferLookupService offerLookupService;
    private final ProjectRegistrationService projectRegistrationService;
    private final ProjectInformationService projectInformationService;
    private final PersonSearchService personSearchService;

    private String projectPreviewFilter = "";
    private GridLazyDataView<ProjectPreview> projectPreviewGridLazyDataView;

    public Handler(OfferLookupService offerLookupService, ProjectRepository projectRepository,
        ProjectInformationService projectInformationService,
        ProjectRegistrationService projectRegistrationService, PersonSearchService personSearchService) {

      Objects.requireNonNull(offerLookupService);
      this.offerLookupService = offerLookupService;

      Objects.requireNonNull(projectRepository);

      Objects.requireNonNull(projectInformationService);
      this.projectInformationService = projectInformationService;

      Objects.requireNonNull(projectRegistrationService);
      this.projectRegistrationService = projectRegistrationService;

      Objects.requireNonNull(personSearchService);
      this.personSearchService = personSearchService;

      configurePageButtons();
      configureProjectCreationDialog();
      loadOfferPreview();
      setProjectsToGrid();
      setupSearchBar();
      setUpPrincipalInvestigatorSearch();
      setUpResponsiblePersonSearch();
      setUpProjectManagerSearch();
    }

    private void setupSearchBar() {
      projectSearchField.setValueChangeMode(ValueChangeMode.LAZY);
      projectSearchField.addValueChangeListener(event -> {
        projectPreviewFilter = event.getValue().trim();
        projectPreviewGridLazyDataView.refreshAll();
      });
    }


    private void setProjectsToGrid() {
      projectPreviewGridLazyDataView = projectGrid.setItems(query -> {
        List<SortOrder> sortOrders = query.getSortOrders().stream().map(
                it -> new SortOrder(it.getSorted(), it.getDirection().equals(SortDirection.DESCENDING)))
            .collect(Collectors.toList());
        // if no order is provided by the grid order by last modified (least priority)
        sortOrders.add(SortOrder.of("lastModified").descending());
        return projectInformationService.queryPreview(projectPreviewFilter, query.getOffset(),
            query.getLimit(), List.copyOf(sortOrders)).stream();
      });
    }

    private void configurePageButtons() {
      create.addClickListener(e -> projectCreationDialog.open());

    }

    private void configureProjectCreationDialog() {
      projectCreationDialog.addProjectCreationEventListener(
          event -> processProjectCreation(event.getSource().content()));
      projectCreationDialog.addCancelEventListener(event -> projectCreationDialog.resetAndClose());
    }

    private void processProjectCreation(ProjectCreationContent projectCreationContent) {

      Result<Project, ApplicationException> project = projectRegistrationService.registerProject(
          projectCreationContent.offerId(), projectCreationContent.projectCode(),
          projectCreationContent.title(), projectCreationContent.objective(),
          projectCreationContent.experimentalDesignDescription(), projectCreationContent.species(),
          projectCreationContent.specimen(), projectCreationContent.analyte(),
          projectCreationContent.principalInvestigator(),
          projectCreationContent.projectResponsible(),
          projectCreationContent.projectManager());

      project
          .onValue(result -> {
            displaySuccessfulProjectCreationNotification();
            projectCreationDialog.resetAndClose();
            projectGrid.getDataProvider().refreshAll();
          })
          .onError(e -> {
            throw e;
          });
    }

    private void displaySuccessfulProjectCreationNotification() {
      SuccessMessage successMessage = new SuccessMessage("Project creation succeeded.", "");
      StyledNotification notification = new StyledNotification(successMessage);
      notification.open();
    }

    private void loadOfferPreview() {
      // Configure the filter and pagination for the lazy loaded OfferPreview items
      projectCreationDialog.offerSearchField.setItems(
          query -> offerLookupService.findOfferContainingProjectTitleOrId(
              query.getFilter().orElse(""), query.getFilter().orElse(""), query.getOffset(),
              query.getLimit()).stream());

      // Render the preview
      projectCreationDialog.offerSearchField.setRenderer(
          new ComponentRenderer<>(preview -> new Text(previewToString(preview))));

      // Generate labels like the rendering
      projectCreationDialog.offerSearchField.setItemLabelGenerator(
          (ItemLabelGenerator<OfferPreview>) it -> it.offerId().id());

      projectCreationDialog.offerSearchField.addValueChangeListener(e -> {
        if (projectCreationDialog.offerSearchField.getValue() != null) {
          preloadContentFromOffer(projectCreationDialog.offerSearchField.getValue().offerId().id());
        }
      });
    }

    private void setUpPersonSearch(ComboBox<PersonReference> comboBox) {
      comboBox.setItems(
          query -> personSearchService.find(query.getFilter().orElse(""), query.getOffset(),
              query.getLimit()).stream());
      comboBox.setRenderer(
          new ComponentRenderer<>(personReference -> new Text(personReference.fullName())));
      comboBox.setItemLabelGenerator(
          (ItemLabelGenerator<PersonReference>) PersonReference::fullName);
    }

    private void setUpProjectManagerSearch() {
      setUpPersonSearch(projectCreationDialog.projectManager);
    }

    private void setUpPrincipalInvestigatorSearch() {
      setUpPersonSearch(projectCreationDialog.principalInvestigator);
    }

    private void setUpResponsiblePersonSearch() {
      setUpPersonSearch(projectCreationDialog.responsiblePerson);
    }

    private void preloadContentFromOffer(String offerId) {
      log.info("Receiving offerId " + offerId);
      OfferId id = OfferId.from(offerId);
      Optional<Offer> offer = offerLookupService.findOfferById(id);
      offer.ifPresentOrElse(projectCreationDialog::setOffer,
          () -> log.error("No offer found with id: " + offerId));
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
}
