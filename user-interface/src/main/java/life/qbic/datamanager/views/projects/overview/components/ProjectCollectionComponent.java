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
import life.qbic.datamanager.views.projects.project.info.ProjectInformationMain;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.ProjectPreview;
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
  final Button createProjectButton = new Button("Add");
  private final Div header = new Div();
  private final ClientDetailsProvider clientDetailsProvider;
  private final transient ProjectInformationService projectInformationService;
  private String projectPreviewFilter = "";
  private GridLazyDataView<ProjectPreview> projectPreviewGridLazyDataView;

  public ProjectCollectionComponent(ClientDetailsProvider clientDetailsProvider,
      ProjectInformationService projectInformationService) {
    this.clientDetailsProvider = clientDetailsProvider;
    this.projectInformationService = projectInformationService;
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
      String lastModified = asClientLocalDateTime(projectPreview.lastModified()).format(
          DateTimeFormatter.ISO_LOCAL_DATE);
      ProjectPreviewItem projectPreviewItem = new ProjectPreviewItem(
          projectPreview.projectId().value(), projectPreview.projectCode(),
          projectPreview.projectTitle(), lastModified);
      projectPreviewItem.setCollaborators(List.of("Frank Tank", "Awesome Guy", "Guso Goon"));
      projectPreviewItem.setProjectDetails("Mrs Principles", "Mr Responsible");
      projectPreviewItem.setMeasurementTypes(List.of("Proteomics", "Genomics"));
      return projectPreviewItem;
    });
    projectGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS);
    projectGrid.addClassName("project-grid");
    add(projectGrid);
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
   * ProjectPreviewItem
   * <p>
   * The Project Preview Item is a Div container styled similar to the {@link Card} component,
   * hosting the project information of interest provided by a {@link ProjectPreview}
   */
  private static class ProjectPreviewItem extends Div {

    private static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
    private final Span tags = new Span();
    private final Div projectDetails = new Div();
    private final AvatarGroup usersWithAccess = new AvatarGroup();
    private static final int MAXIMUM_NUMBER_OF_SHOWN_AVATARS = 2;

    public ProjectPreviewItem(String projectId, String projectCode,
        String projectTitle, String lastModificationDate) {
      add(createHeader(projectCode, projectTitle));
      Span lastModified = new Span(String.format("Last modified on %s", lastModificationDate));
      lastModified.addClassName("secondary");
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
      Span projectResponsible = new Span(
          String.format("Project Responsible: %s", responsiblePartyName));
      projectDetails.add(principalInvestigator, projectResponsible);
    }

    public void setMeasurementTypes(Collection<String> measurementTypes) {
      tags.removeAll();
      measurementTypes.forEach(measurementType -> tags.add(new Tag(measurementType)));
    }
  }

}
