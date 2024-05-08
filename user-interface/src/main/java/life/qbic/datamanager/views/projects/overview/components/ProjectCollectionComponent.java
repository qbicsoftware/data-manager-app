package life.qbic.datamanager.views.projects.overview.components;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.component.avatar.AvatarGroup.AvatarGroupItem;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.dataview.GridLazyDataView;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.spring.annotation.RouteScope;
import java.io.Serial;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import life.qbic.application.commons.SortOrder;
import life.qbic.datamanager.ClientDetailsProvider;
import life.qbic.datamanager.ClientDetailsProvider.ClientDetails;
import life.qbic.datamanager.views.general.Card;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.general.Tag;
import life.qbic.datamanager.views.general.Tag.TagColor;
import life.qbic.datamanager.views.projects.project.info.ProjectInformationMain;
import life.qbic.identity.api.UserInfo;
import life.qbic.identity.api.UserInformationService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.ProjectPreview;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService;
import life.qbic.projectmanagement.application.measurement.MeasurementService;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.stereotype.Component;

/**
 * <b>Project Collection</b>
 * <p>
 * A component that displays previews of accessible project previews.
 * <p>
 * The component also fires {@link ProjectCreationSubmitEvent} to all registered listeners, if a
 * user has the intend to add a new project.
 *
 * @since 1.0.0
 */
@Component
@RouteScope
public class ProjectCollectionComponent extends PageArea {

  @Serial
  private static final long serialVersionUID = 8579375312838977742L;
  final TextField projectSearchField = new TextField();
  final Grid<ProjectPreview> projectGrid = new Grid<>(ProjectPreview.class, false);
  final Button createProjectButton = new Button("Create");
  private final Div header = new Div();
  private final ClientDetailsProvider clientDetailsProvider;
  private final transient ProjectInformationService projectInformationService;
  private final transient ProjectAccessService projectAccessService;
  private final transient UserInformationService userInformationService;
  private final transient MeasurementService measurementService;
  private String projectPreviewFilter = "";
  private GridLazyDataView<ProjectPreview> projectPreviewGridLazyDataView;

  public ProjectCollectionComponent(ClientDetailsProvider clientDetailsProvider,
      ProjectInformationService projectInformationService,
      ProjectAccessService projectAccessService,
      UserInformationService userInformationService,
      MeasurementService measurementService) {
    this.clientDetailsProvider = Objects.requireNonNull(clientDetailsProvider,
        "Client details provider cannot be null");
    this.projectInformationService = Objects.requireNonNull(projectInformationService,
        "Project information service cannot be null");
    this.projectAccessService = Objects.requireNonNull(projectAccessService,
        "Project access service cannot be null");
    this.userInformationService = Objects.requireNonNull(userInformationService,
        "User information service cannot be null");
    this.measurementService = Objects.requireNonNull(measurementService,
        "Measurement service cannot be null");
    layoutComponent();
    createLazyProjectView();
    configureSearch();
    configureProjectCreationButton();
  }

  private void initHeader() {
    header.addClassName("header");
    Span title = new Span("My Projects");
    title.addClassName("title");
    createProjectButton.addClassName("primary");
    projectSearchField.setPlaceholder("Search");
    projectSearchField.setClearButtonVisible(true);
    projectSearchField.setSuffixComponent(VaadinIcon.SEARCH.create());
    projectSearchField.addClassNames("search-field");
    Span controls = new Span(projectSearchField, createProjectButton);
    controls.addClassName("controls");
    header.add(title, controls);
    add(header);
  }

  private void layoutComponent() {
    addClassNames("project-collection-component");
    initHeader();
    layoutGrid();
  }

  private void createLazyProjectView() {
    projectPreviewGridLazyDataView = projectGrid.setItems(query -> {
      List<SortOrder> sortOrders = query.getSortOrders().stream().map(
              it -> new SortOrder(it.getSorted(), it.getDirection().equals(SortDirection.DESCENDING)))
          .collect(Collectors.toList());
      // if no order is provided by the grid order by last modified (the least priority)
      sortOrders.add(SortOrder.of("lastModified").descending());
      return projectInformationService.queryPreview(projectPreviewFilter, query.getOffset(),
          query.getLimit(), List.copyOf(sortOrders)).stream();
    });
  }

  private void configureSearch() {
    projectSearchField.setValueChangeMode(ValueChangeMode.LAZY);
    projectSearchField.addValueChangeListener(event -> {
      projectPreviewFilter = event.getValue().trim();
      projectPreviewGridLazyDataView.refreshAll();
    });
  }

  private void configureProjectCreationButton() {
    createProjectButton.addClickListener(listener -> fireCreateClickedEvent());
  }

  private void layoutGrid() {
    projectGrid.setSelectionMode(SelectionMode.NONE);
    projectGrid.addComponentColumn(projectPreview -> {
      Project project = projectInformationService.find(projectPreview.projectId()).orElseThrow();
      String lastModified = asClientLocalDateTime(projectPreview.lastModified()).format(
          DateTimeFormatter.ISO_LOCAL_DATE);
      ProjectPreviewItem projectPreviewItem = new ProjectPreviewItem(
          projectPreview.projectId().value(), projectPreview.projectCode(),
          projectPreview.projectTitle(), lastModified);
      var userNames = retrieveProjectCollaborators(projectPreview.projectId());
      projectPreviewItem.setCollaborators(userNames);
      String projectManagerName = project.getProjectManager().fullName();
      String responsiblePersonName = "";
      if (project.getResponsiblePerson().isPresent()) {
        responsiblePersonName = project.getResponsiblePerson().get().fullName();
      }
      projectPreviewItem.setProjectDetails(projectManagerName, responsiblePersonName);
      var measurementTypesInProject = retrieveRegisteredMeasurementTypes(project.getId(),
          project.experiments());
      projectPreviewItem.setMeasurementTypes(measurementTypesInProject);
      return projectPreviewItem;
    });
    projectGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS);
    projectGrid.addClassName("project-grid");
    add(projectGrid);
  }

  private Collection<MeasurementType> retrieveRegisteredMeasurementTypes(ProjectId projectId,
      Collection<ExperimentId> experimentsInProject) {
    long proteomicsMeasurementCount = experimentsInProject.stream().map(
            experimentId -> measurementService.countProteomicsMeasurements(experimentId, projectId))
        .reduce(
            0L, Long::sum);
    long ngsMeasurementCount = experimentsInProject.stream()
        .map(experimentId -> measurementService.countNGSMeasurements(experimentId, projectId))
        .reduce(
            0L, Long::sum);
    Collection<MeasurementType> measurementTypes = new ArrayList<>();
    if (proteomicsMeasurementCount != 0) {
      measurementTypes.add(MeasurementType.PROTEOMICS);
    }
    if (ngsMeasurementCount != 0) {
      measurementTypes.add(MeasurementType.GENOMICS);
    }
    return measurementTypes;
  }

  private List<String> retrieveProjectCollaborators(ProjectId projectId) {
    return projectAccessService.listCollaborators(projectId).stream()
        .map(projectCollaborator -> userInformationService.findById(projectCollaborator.userId())
            //We can't throw an exception here since projects can be linked to deleted users
            .map(UserInfo::alias).orElse("")).toList();
  }

  private void fireCreateClickedEvent() {
    var clickedEvent = new ProjectCreationSubmitEvent(this, true);
    fireEvent(clickedEvent);
  }

  private LocalDateTime asClientLocalDateTime(Instant instant) {
    ZonedDateTime zonedDateTime = instant.atZone(ZoneId.of(
        this.clientDetailsProvider.latestDetails().map(ClientDetails::timeZoneId).orElse("UTC")));
    return zonedDateTime.toLocalDateTime();
  }

  /**
   * Add a listener that is called, when a new {@link ProjectCreationSubmitEvent event} is emitted.
   *
   * @param listener a listener that should be called
   * @since 1.0.0
   */
  public void addCreateClickedListener(
      ComponentEventListener<ProjectCreationSubmitEvent> listener) {
    Objects.requireNonNull(listener);
    addListener(ProjectCreationSubmitEvent.class, listener);
  }

  public void refresh() {
    projectGrid.getDataProvider().refreshAll();
  }

  /**
   * Tag color enum is used to set the tag color to one of the predefined values to allow different
   * coloration of tags as necessary
   */
  public enum MeasurementType {
    PROTEOMICS("Proteomics"),
    GENOMICS("Genomics");

    private final String type;

    MeasurementType(String type) {
      this.type = type;
    }

    public String getType() {
      return type;
    }
  }

  /**
   * ProjectPreviewItem
   * <p>
   * The Project Preview Item is a Div container styled similar to the {@link Card} component,
   * hosting the project information of interest provided by a {@link ProjectPreview}
   */
  private static class ProjectPreviewItem extends Div {

    private static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
    private static final int MAXIMUM_NUMBER_OF_SHOWN_AVATARS = 3;
    private final Span tags = new Span();
    private final Div projectDetails = new Div();
    private final AvatarGroup usersWithAccess = new AvatarGroup();

    public ProjectPreviewItem(String projectId, String projectCode,
        String projectTitle, String lastModificationDate) {
      add(createHeader(projectCode, projectTitle));
      Span lastModified = new Span(String.format("Last modified on %s", lastModificationDate));
      lastModified.addClassName("tertiary");
      add(lastModified);
      projectDetails.addClassName("details");
      add(projectDetails);
      usersWithAccess.setMaxItemsVisible(MAXIMUM_NUMBER_OF_SHOWN_AVATARS);
      add(usersWithAccess);
      addClassNames("project-preview-item");
      addClickListener(event -> getUI().ifPresent(ui -> ui.navigate(ProjectInformationMain.class,
          new RouteParam(PROJECT_ID_ROUTE_PARAMETER, projectId))));
    }

    private Span createHeader(String projectCode, String projectTitle) {
      Span title = new Span(String.format("%s - %s", projectCode, projectTitle));
      title.addClassName("project-preview-item-title");
      tags.addClassNames("tag-collection");
      Span header = new Span(title, tags);
      header.addClassName("header");
      return header;
    }

    public void setCollaborators(List<String> collaboratorNames) {
      usersWithAccess.getItems().forEach(usersWithAccess::remove);
      collaboratorNames.stream().map(AvatarGroupItem::new).forEach(usersWithAccess::add);
    }

    public void setProjectDetails(String principalInvestigatorName, String responsiblePartyName) {
      projectDetails.removeAll();
      Span principalInvestigator = new Span(
          String.format("Principal Investigator: %s", principalInvestigatorName));
      Span projectResponsible = new Span();
      if (!responsiblePartyName.isBlank()) {
        projectResponsible.setText(String.format("Project Responsible: %s", responsiblePartyName));
      }
      projectDetails.add(principalInvestigator, projectResponsible);
    }

    public void setMeasurementTypes(Collection<MeasurementType> measurementTypes) {
      tags.removeAll();
      measurementTypes.forEach(measurementType -> {
        Tag tag = new Tag(measurementType.getType());
        tag.setTagColor(getMeasurementSpecificTagColor(measurementType));
        tags.add(tag);
      });
    }

    private TagColor getMeasurementSpecificTagColor(MeasurementType measurementType) {
      return switch (measurementType) {
        case PROTEOMICS -> TagColor.VIOLET;
        case GENOMICS -> TagColor.PINK;
      };
    }
  }
}
