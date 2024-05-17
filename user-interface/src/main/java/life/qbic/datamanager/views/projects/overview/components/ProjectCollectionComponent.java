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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import life.qbic.application.commons.SortOrder;
import life.qbic.datamanager.ClientDetailsProvider;
import life.qbic.datamanager.views.general.Card;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.general.Tag;
import life.qbic.datamanager.views.general.Tag.TagColor;
import life.qbic.datamanager.views.projects.project.info.ProjectInformationMain;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.ProjectOverview;
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
  final Grid<ProjectOverview> projectGrid = new Grid<>(ProjectOverview.class, false);
  final Button createProjectButton = new Button("Create");
  private final Div header = new Div();
  private final transient ProjectInformationService projectInformationService;
  private String projectPreviewFilter = "";
  private GridLazyDataView<ProjectOverview> projectPreviewGridLazyDataView;

  public ProjectCollectionComponent(ClientDetailsProvider clientDetailsProvider,
      ProjectInformationService projectInformationService) {
    this.projectInformationService = Objects.requireNonNull(projectInformationService,
        "Project information service cannot be null");
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
      return projectInformationService.queryOverview(projectPreviewFilter, query.getOffset(),
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
    projectGrid.addComponentColumn(ProjectPreviewItem::new);
    projectGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS);
    projectGrid.addClassName("project-grid");
    add(projectGrid);
  }

  private void fireCreateClickedEvent() {
    var clickedEvent = new ProjectCreationSubmitEvent(this, true);
    fireEvent(clickedEvent);
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
   * hosting the project information of interest provided by a {@link ProjectOverview}
   */
  private static class ProjectPreviewItem extends Div {

    private static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
    private static final int MAXIMUM_NUMBER_OF_SHOWN_AVATARS = 3;
    private final Span tags = new Span();
    private final Div projectDetails = new Div();
    private final AvatarGroup usersWithAccess = new AvatarGroup();
    private final ProjectOverview projectOverview;

    public ProjectPreviewItem(ProjectOverview projectOverview) {
      this.projectOverview = Objects.requireNonNull(projectOverview);
      Span header = createHeader(projectOverview.projectCode(), projectOverview.projectTitle());
      add(header);
      Span lastModified = new Span(
          String.format("Last modified on %s", projectOverview.lastModified()));
      lastModified.addClassName("tertiary");
      add(lastModified);
      projectDetails.addClassName("details");
      Span principalInvestigator = new Span(
          String.format("Principal Investigator: %s", projectOverview.principalInvestigatorName()));
      Span projectResponsible = new Span();
      if (projectOverview.projectResponsibleName() != null) {
        projectResponsible.setText(
            String.format("Project Responsible: %s", projectOverview.projectResponsibleName()));
      }
      projectDetails.add(principalInvestigator, projectResponsible);
      add(projectDetails);
      usersWithAccess.setMaxItemsVisible(MAXIMUM_NUMBER_OF_SHOWN_AVATARS);
      add(usersWithAccess);
      setMeasurementDependentTags();
      projectOverview.collaboratorUserNames().stream().map(AvatarGroupItem::new)
          .forEach(usersWithAccess::add);
      addClassNames("project-preview-item");
      addClickListener(event -> getUI().ifPresent(ui -> ui.navigate(ProjectInformationMain.class,
          new RouteParam(PROJECT_ID_ROUTE_PARAMETER, projectOverview.projectId().value()))));
    }

    private Span createHeader(String projectCode, String projectTitle) {
      Span title = new Span(String.format("%s - %s", projectCode, projectTitle));
      title.addClassName("project-preview-item-title");
      tags.addClassNames("tag-collection");
      Span header = new Span(title, tags);
      header.addClassName("header");
      return header;
    }

    public void setMeasurementDependentTags() {
      tags.removeAll();
      Collection<MeasurementType> measurementTypes = new ArrayList<>();
      if (projectOverview.pxpMeasurementCount() != null) {
        measurementTypes.add(MeasurementType.PROTEOMICS);
      }
      if (projectOverview.ngsMeasurementCount() != null) {
        measurementTypes.add(MeasurementType.GENOMICS);
      }
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
