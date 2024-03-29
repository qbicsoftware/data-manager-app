package life.qbic.datamanager.views.projects.overview.components;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridLazyDataView;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LocalDateTimeRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.spring.annotation.RouteScope;
import java.io.Serial;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import life.qbic.datamanager.ClientDetailsProvider;
import life.qbic.datamanager.ClientDetailsProvider.ClientDetails;
import life.qbic.datamanager.views.AppRoutes.Projects;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.ProjectPreview;
import life.qbic.projectmanagement.application.SortOrder;
import org.springframework.stereotype.Component;

/**
 * <b>Project Collection</b>
 * <p>
 * A component that displays previews of accessible project previews.
 * <p>
 * The component also fires {@link ProjectCreationSubmitEvent} to all registered listeners, if a user has
 * the intend to add a new project.
 *
 * @since 1.0.0
 */
@Component
@RouteScope
public class ProjectCollectionComponent extends PageArea {

  private final Div header = new Div();
  private String projectPreviewFilter = "";
  private GridLazyDataView<ProjectPreview> projectPreviewGridLazyDataView;
  @Serial
  private static final long serialVersionUID = 8579375312838977742L;
  final TextField projectSearchField = new TextField();
  final Grid<ProjectPreview> projectGrid = new Grid<>(ProjectPreview.class, false);
  final Button createProjectButton = new Button("Add");
  private final ClientDetailsProvider clientDetailsProvider;
  private final transient ProjectInformationService projectInformationService;

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
    Span titleField = new Span();
    titleField.setText("Projects");
    titleField.addClassName("title");
    createProjectButton.addClassName("primary");
    projectSearchField.setPlaceholder("Search");
    projectSearchField.setClearButtonVisible(true);
    projectSearchField.setSuffixComponent(VaadinIcon.SEARCH.create());
    projectSearchField.addClassNames("search-field");
    Span controls = new Span(projectSearchField, createProjectButton);
    controls.addClassName("controls");
    header.add(titleField, controls);
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
    projectGrid.addColumn(new ComponentRenderer<>(
            item -> new Anchor(String.format(Projects.PROJECT_INFO, item.projectId().value()),
                item.projectCode()))).setHeader("Code").setWidth("7em").setFlexGrow(0)
        .setSortProperty("projectCode");

    projectGrid.addColumn(new ComponentRenderer<>(
            item -> new Anchor(String.format(Projects.PROJECT_INFO, item.projectId().value()),
                item.projectTitle()))).setHeader("Title").setKey("projectTitle").setSortable(true)
        .setSortProperty("projectTitle");

    projectGrid.addColumn(new LocalDateTimeRenderer<>(
            projectPreview -> asClientLocalDateTime(projectPreview.lastModified()),
            "yyyy-MM-dd HH:mm:ss")).setKey("lastModified").setHeader("lastModified").setSortable(true)
        .setSortProperty("lastModified");
    projectGrid.addClassName("projects-grid");
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
}
