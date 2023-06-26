package life.qbic.datamanager.views.projects.overview.components;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridLazyDataView;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LocalDateTimeRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import java.io.Serial;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import life.qbic.datamanager.views.AppRoutes.Projects;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.ProjectPreview;
import life.qbic.projectmanagement.application.SortOrder;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class ProjectsCollection extends PageArea {

  private String projectPreviewFilter = "";
  private GridLazyDataView<ProjectPreview> projectPreviewGridLazyDataView;
  @Serial
  private static final long serialVersionUID = 8579375312838977742L;
  final TextField projectSearchField = new TextField();
  final Grid<ProjectPreview> projectGrid = new Grid<>(ProjectPreview.class, false);
  final Button createProjectButton = new Button("Add");
  private final String title;
  private final ZoneId clientZoneId;
  private final ProjectInformationService projectInformationService;
  private final List<ComponentEventListener<ProjectCreationClickedEvent>> projectCreationClickedListeners = new ArrayList<>();

  private Div controlSection = new Div();

  private Div gridSection = new Div();

  private Div titleSection = new Div();

  private ProjectsCollection(String title, ZoneId clientZoneId,
      ProjectInformationService projectInformationService) {
    this.title = title;
    this.clientZoneId = clientZoneId;
    this.projectInformationService = projectInformationService;
    layoutComponent();
    createLazyProjectView();
    configureSearch();
    configureProjectCreationButton();
  }

  private void layoutComponent() {
    addClassNames("projects-collection");
    layoutSearchField();
    layoutProjectCreationButton();
    layoutGrid();
    layoutTitleSection();
    layoutControlSection();
    layoutGridSection();
    controlSection.add(new Div(projectSearchField));
    controlSection.add(new Div(createProjectButton));

    gridSection.add(projectGrid);
    add(titleSection);
    add(controlSection);
    add(gridSection);
  }

  private void layoutTitleSection() {
    titleSection.addClassName("title");
    titleSection.setText(title);
  }

  private void layoutGridSection() {
    gridSection.addClassName("projects-grid");
  }

  private void layoutControlSection() {
    controlSection.addClassName("controls");
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
    createProjectButton.addClickListener(listener -> {
      fireClickEvent();
    });
  }

  private void fireClickEvent() {
    var clickedEvent = new ProjectCreationClickedEvent(this, true);
    projectCreationClickedListeners.forEach(listener -> listener.onComponentEvent(clickedEvent));
  }

  private void layoutSearchField() {
    projectSearchField.setPlaceholder("Search");
    projectSearchField.setClearButtonVisible(true);
    projectSearchField.setSuffixComponent(VaadinIcon.SEARCH.create());
    projectSearchField.addClassNames("search-field");
  }

  private void layoutProjectCreationButton() {
    createProjectButton.addClassName("add-project-button");
  }

  private void layoutGrid() {
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
  }

  private LocalDateTime asClientLocalDateTime(Instant instant) {
    ZonedDateTime zonedDateTime = instant.atZone(this.clientZoneId);
    return zonedDateTime.toLocalDateTime();
  }

  public static ProjectsCollection create(String title, ZoneId clientZoneId,
      ProjectInformationService projectInformationService) {
    Objects.requireNonNull(projectInformationService);
    Objects.requireNonNull(clientZoneId);
    return new ProjectsCollection(title, clientZoneId, projectInformationService);
  }

  public void addListener(ComponentEventListener<ProjectCreationClickedEvent> listener) {
    Objects.requireNonNull(listener);
    projectCreationClickedListeners.add(listener);
  }


}
