package life.qbic.datamanager.views.projects.overview.components;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.avatar.AvatarGroup;
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
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.spring.annotation.RouteScope;
import java.io.Serial;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import life.qbic.application.commons.SortOrder;
import life.qbic.application.commons.time.DateTimeFormat;
import life.qbic.datamanager.views.account.UserAvatar.UserAvatarGroupItem;
import life.qbic.datamanager.views.general.Card;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.general.Tag;
import life.qbic.datamanager.views.general.Tag.TagColor;
import life.qbic.datamanager.views.projects.project.datasets.ConnectedDatasetsMain;
import life.qbic.datamanager.views.projects.project.info.ProjectInformationMain;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.ProjectOverview;
import org.springframework.stereotype.Component;

/**
 * <b>Project Collection</b>
 * <p>
 * A component that displays cards showing the content of accessible
 * {@link ProjectOverview for the logged-in user.
 * <p>
 * The component also fires {@link ProjectCreationSubmitEvent} to all registered listeners, if a
 * user has the intend to create a new project.
 *
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
  private final Span searchResultInfo = new Span();
  private String projectOverviewFilter = "";
  private GridLazyDataView<ProjectOverview> projectOverviewGridLazyDataView;

  public ProjectCollectionComponent(ProjectInformationService projectInformationService) {
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

  private void initSearchResultInfo() {
    searchResultInfo.addClassName("secondary");
    add(searchResultInfo);
  }

  private void layoutComponent() {
    addClassNames("project-collection-component");
    initHeader();
    initSearchResultInfo();
    layoutGrid();
  }

  private void createLazyProjectView() {
    projectOverviewGridLazyDataView = projectGrid.setItems(query -> {
      List<SortOrder> sortOrders = query.getSortOrders().stream().map(
              it -> new SortOrder(it.getSorted(), it.getDirection().equals(SortDirection.DESCENDING)))
          .collect(Collectors.toList());
      // if no order is provided by the grid order by last modified (the least priority)
      sortOrders.add(SortOrder.of("lastModified").descending());
      return projectInformationService.queryOverview(projectOverviewFilter, query.getOffset(),
          query.getLimit(), List.copyOf(sortOrders)).stream();
    });
  }

  private void configureSearch() {
    projectSearchField.setValueChangeMode(ValueChangeMode.LAZY);
    projectSearchField.addValueChangeListener(event -> {
      projectOverviewFilter = event.getValue().trim();
      projectOverviewGridLazyDataView.refreshAll();
      showSearchResult(!event.getValue().isBlank());
    });
  }

  private void configureProjectCreationButton() {
    createProjectButton.addClickListener(listener -> fireCreateClickedEvent());
  }

  private void layoutGrid() {
    projectGrid.setSelectionMode(SelectionMode.NONE);
    projectGrid.addComponentColumn(ProjectOverviewItem::new);
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

  private void showSearchResult(boolean isVisible) {
    searchResultInfo.setVisible(isVisible);
    searchResultInfo.setText(
        "%s projects found".formatted(projectOverviewGridLazyDataView.getItems().count()));
  }

  /**
   * Resets the value within the searchField, which in turn resets the grid. Additionally, hides the
   * entire section so the result span is only shown when the user is actively searching for an
   * ontology
   */
  public void resetSearch() {
    projectSearchField.setValue("");
  }

  /**
   * The Measurement Types are employed to set the Tag Color and Tag naming dependent on the
   * registered measurements within the projectCollection
   */
  public enum MeasurementType {
    PROTEOMICS("Proteomics"),
    GENOMICS("Genomics"),
    IMMUNOPEPTIDOMICS("Immunopeptidomics");

    private final String type;

    MeasurementType(String type) {
      this.type = type;
    }

    public String getType() {
      return type;
    }
  }

  /**
   * ProjectOverviewItem
   * <p>
   * The Project Overview Item is a Div container styled similar to the {@link Card} component,
   * hosting the project information of interest provided by a {@link ProjectOverview}
   */
  private static class ProjectOverviewItem extends Div {

    private static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
    private static final int MAXIMUM_NUMBER_OF_SHOWN_AVATARS = 3;
    private final Span tags = new Span();
    private final Div projectDetails = new Div();
    private final AvatarGroup usersWithAccess = new AvatarGroup();
    private final transient ProjectOverview projectOverview;

    public ProjectOverviewItem(ProjectOverview projectOverview) {
      this.projectOverview = Objects.requireNonNull(projectOverview);
      // Both RouterLinks (card body + footer) must share a single parent so they render
      // as one unified card. Using a wrapper Div prevents event propagation between
      // clicks on the footer and clicks on the card body.
      var wrapper = new Div();
      wrapper.addClassName("project-card-wrapper");
      wrapper.add(projectInfoLink());
      attachDatasetFooter(wrapper);
      add(wrapper);
    }

    /**
     * Builds the card-body RouterLink (navigates to project info).
     *
     * <p>The card body is a RouterLink, not a Div-with-addClickListener, so the
     * footer RouterLink (a sibling, not a child) cannot have its click propagate up to the card
     * body. This is the fix for the "footer click navigates to project info first, then to
     * datasets" bug.</p>
     *
     * <p>The card body carries the {@code project-overview-item} class so the
     * existing page-area.css card styles (shadow, border-radius, padding) apply to it
     * directly.</p>
     */
    private RouterLink projectInfoLink() {
      var link = new RouterLink("", ProjectInformationMain.class,
          new RouteParameters(PROJECT_ID_ROUTE_PARAMETER, projectOverview.projectId().value()));
      link.addClassName("project-overview-item");

      link.add(createHeader(projectOverview.projectCode(), projectOverview.projectTitle()));

      Instant instant = projectOverview.lastModified();
      Span lastModified = new Span(
          String.format("Last modified on %s",
              DateTimeFormat.asJavaFormatter(DateTimeFormat.SIMPLE_DATE_SHORT,
                      ZoneId.systemDefault())
                  .format(instant)));
      lastModified.addClassName("tertiary");
      link.add(lastModified);

      projectDetails.addClassName("details");
      Span principalInvestigator = new Span(
          String.format("Principal Investigator: %s", projectOverview.principalInvestigatorName()));
      Span projectResponsible = new Span();
      if (projectOverview.projectResponsibleName() != null) {
        projectResponsible.setText(
            String.format("Project Responsible: %s", projectOverview.projectResponsibleName()));
      }
      projectDetails.add(principalInvestigator, projectResponsible);
      link.add(projectDetails);

      usersWithAccess.setMaxItemsVisible(MAXIMUM_NUMBER_OF_SHOWN_AVATARS);
      link.add(usersWithAccess);

      setMeasurementDependentTags();
      projectOverview.collaboratorUserInfos().stream()
          .map(userInfo -> new UserAvatarGroupItem(userInfo.userName(), userInfo.userId()))
          .forEach(usersWithAccess::add);

      return link;
    }

    /**
     * Attaches the connected-dataset footer to the card body when the project has
     * any connected datasets. The footer is rendered as a full-width
     * {@link RouterLink} to the project's connected-datasets view; it is a
     * sibling (not a child) of the card-body RouterLink, so each click
     * target has exactly one handler and neither can fire the other's
     * navigation — no event-propagation workaround required.
     *
     * <p>When no datasets are connected, nothing is rendered — avoids
     * advertising an empty state on the listing.</p>
     */
    private void attachDatasetFooter(Div wrapper) {
      if (projectOverview.connectedDatasetCount() > 0) {
        wrapper.add(buildDatasetFooter(projectOverview));
      }
    }

    /**
     * Builds the connected-dataset footer for a project card.
     *
     * <p>Layout (left-right): database icon, count, open/restricted
     * {@link Tag} pills, last-connected date, spacer, trailing chevron. Wrapped in a
     * {@link RouterLink} so the entire footer is a large click target (44px+) — better
     * accessibility than an icon-only link.
     *
     * <p>The footer RouterLink is a sibling (not a child) of the card-body
     * RouterLink on {@code ProjectOverviewItem}, so a footer click cannot also fire the card's
     * project-info navigation — no event-propagation workaround required.</p>
     *
     * <p>All layout/presentation is driven by CSS classes defined in
     * {@code all.css} (flex utilities, spacing, typography) and in {@code page-area.css}
     * ({@code .project-dataset-footer}).</p>
     */
    private static RouterLink buildDatasetFooter(ProjectOverview projectOverview) {
      // ── Footer content (plain Div inside the RouterLink) ──────
      var content = new Div();
      content.addClassName("flex-horizontal");
      content.addClassName("flex-align-items-center");
      content.addClassName("gap-03");

      int total = projectOverview.connectedDatasetCount();
      int open = projectOverview.openDatasetCount();
      int restricted = projectOverview.restrictedDatasetCount();
      Instant lastConnected = projectOverview.lastConnectedOn();

      // Database icon — neutral grey, not linked colour
      var icon = VaadinIcon.DATABASE.create();
      icon.addClassName("flex-shrink-0");
      icon.addClassName("color-secondary");
      content.add(icon);

      // Count — bold
      var countSpan = new Span(String.valueOf(total));
      countSpan.addClassName("bold");
      countSpan.addClassName("normal-body-text");
      content.add(countSpan);

      var labelSpan = new Span(total == 1 ? "dataset" : "datasets");
      labelSpan.addClassName("extra-small-body-text");
      content.add(labelSpan);

      content.add(buildDotSeparator());

      if (open > 0) {
        var openTag = new Tag("%d Open".formatted(open));
        openTag.setTagColor(TagColor.SUCCESS);
        content.add(openTag);
      }
      if (restricted > 0) {
        var restrictedTag = new Tag("%d Restricted".formatted(restricted));
        restrictedTag.setTagColor(TagColor.WARNING);
        content.add(restrictedTag);
      }

      content.add(buildDotSeparator());

      var lastConnectedLabel = new Span("Last connected");
      lastConnectedLabel.addClassName("extra-small-body-text");
      lastConnectedLabel.addClassName("color-secondary");
      content.add(lastConnectedLabel);

      if (lastConnected != null) {
        var lastConnectedDate = new Span(formatLastConnectedDate(lastConnected));
        lastConnectedDate.addClassName("extra-small-body-text");
        content.add(lastConnectedDate);
      } else {
        var fallback = new Span("—");
        fallback.addClassName("extra-small-body-text");
        fallback.addClassName("color-secondary");
        content.add(fallback);
      }

      // Spacer pushes the chevron to the right edge
      var spacer = new Div();
      spacer.addClassName("flex-grow-1");
      content.add(spacer);

      // Trailing chevron — visual cue only; the entire footer is the
      // RouterLink, so the chevron itself is not a separate click target
      var chevron = VaadinIcon.CHEVRON_RIGHT.create();
      chevron.addClassName("flex-shrink-0");
      content.add(chevron);

      // ─ Wrap in RouterLink (native <a> semantics) ─────────────
      // Anchored to the per-project datasets route already registered
      // for ConnectedDatasetsMain.
      var link = new RouterLink("", ConnectedDatasetsMain.class,
          new RouteParameters(PROJECT_ID_ROUTE_PARAMETER, projectOverview.projectId().value()));
      link.addClassName("project-dataset-footer");
      link.add(content);

      // Accessible name — unambiguous without visual context.
      link.getElement().setAttribute("aria-label",
          buildAriaLabel(projectOverview, total, open, restricted, lastConnected));

      return link;
    }

    private static Span buildDotSeparator() {
      var dot = new Span("·");
      dot.addClassName("extra-small-body-text");
      dot.addClassName("color-secondary");
      return dot;
    }

    /**
     * Formats the {@code connected_on} instant for display in the footer.
     *
     * <p>Uses {@code dd MMM yyyy} (e.g. {@code "05 Jul 2026"}, English
     * locale) — shorter than the card's {@code lastModified} timestamp but still human-readable.
     * The instant is converted to the system default zone before formatting.</p>
     */
    private static String formatLastConnectedDate(Instant instant) {
      return DateTimeFormat.asJavaFormatter(DateTimeFormat.SIMPLE_DATE_SHORT, ZoneId.systemDefault()).format(instant);
    }

    /**
     * Builds the {@code aria-label} for the footer RouterLink.
     *
     * <p>Shape: {@code "Open datasets for Q2KX4B: 4 connected, 2 open,
     * 2 restricted, last updated 05 July 2026"}</p>
     * <p>Screen readers get a coherent sentence describing the link
     * action (open the datasets view) and the current state — far more useful than an icon-only
     * label would be.</p>
     */
    private static String buildAriaLabel(ProjectOverview projectOverview, int total,
        int open, int restricted, Instant lastConnected) {
      String base = "Open datasets for %s: %d connected, %d open, %d restricted".formatted(
          projectOverview.projectCode(), total, open, restricted);
      if (lastConnected != null) {
        String full = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH)
            .withZone(ZoneId.systemDefault())
            .format(lastConnected);
        return base + ", last updated " + full;
      }
      return base;
    }

    private Span createHeader(String projectCode, String projectTitle) {
      Span title = new Span(String.format("%s - %s", projectCode, projectTitle));
      title.addClassName("project-overview-item-title");
      tags.addClassNames("tag-collection");
      Span header = new Span(title, tags);
      header.addClassName("header");
      return header;
    }

    public void setMeasurementDependentTags() {
      tags.removeAll();
      Collection<MeasurementType> measurementTypes = new ArrayList<>();
      if (projectOverview.pxpMeasurementCount() > 0) {
        measurementTypes.add(MeasurementType.PROTEOMICS);
      }
      if (projectOverview.ngsMeasurementCount() > 0) {
        measurementTypes.add(MeasurementType.GENOMICS);
      }
      if (projectOverview.ipMeasurementCount() > 0) {
        measurementTypes.add(MeasurementType.IMMUNOPEPTIDOMICS);
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
        case IMMUNOPEPTIDOMICS -> TagColor.GOLD;
      };
    }
  }
}
