package life.qbic.datamanager.views.projects.overview.components;

import static life.qbic.datamanager.views.projects.overview.components.ProjectOverviewSortOption.CODE_ASC;
import static life.qbic.datamanager.views.projects.overview.components.ProjectOverviewSortOption.CODE_DESC;
import static life.qbic.datamanager.views.projects.overview.components.ProjectOverviewSortOption.LAST_MODIFIED_ASC;
import static life.qbic.datamanager.views.projects.overview.components.ProjectOverviewSortOption.LAST_MODIFIED_DESC;
import static life.qbic.datamanager.views.projects.overview.components.ProjectOverviewSortOption.TITLE_ASC;
import static life.qbic.datamanager.views.projects.overview.components.ProjectOverviewSortOption.TITLE_DESC;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.spring.annotation.RouteScope;
import java.io.Serial;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import life.qbic.application.commons.SortOrder;
import life.qbic.application.commons.time.DateTimeFormat;
import life.qbic.datamanager.views.AppRoutes.ProjectRoutes;
import life.qbic.datamanager.views.account.UserAvatar.UserAvatarGroupItem;
import life.qbic.datamanager.views.general.Card;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.general.Tag;
import life.qbic.datamanager.views.general.Tag.TagColor;
import life.qbic.datamanager.views.general.pagination.ListState;
import life.qbic.datamanager.views.general.pagination.ListStateCodec;
import life.qbic.datamanager.views.general.pagination.PaginationBar;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.datamanager.views.projects.overview.components.PinnedProjectsComponent.ToggleHandler;
import life.qbic.datamanager.views.projects.project.datasets.ConnectedDatasetsMain;
import life.qbic.datamanager.views.projects.project.info.ProjectInformationMain;
import life.qbic.projectmanagement.application.PinnedProjectService;
import life.qbic.projectmanagement.application.PinnedProjectService.PinOutcome;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.ProjectOverview;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.stereotype.Component;

/**
 * <b>Project Collection</b>
 * <p>
 * A component that displays paginated cards showing the content of accessible
 * {@link ProjectOverview} for the logged-in user. The cards are rendered in a responsive grid
 * without an embedded scroll container (USER-NFR-01); navigation happens through an explicit pager
 * with a visible total count (USER-R-01) and the list state is mirrored into the browser URL
 * (USER-R-03).
 * <p>
 * The component also fires {@link ProjectCreationSubmitEvent} to all registered listeners, if a
 * user has the intend to create a new project.
 *
 * @since 1.0.0
 */
@Component
@RouteScope
public class ProjectCollectionComponent extends PageArea {

  @Serial
  private static final long serialVersionUID = 8579375312838977742L;
  private static final String EMPTY_PROJECT_COLLECTION_MESSAGE =
      "You don't have any projects yet. Start by creating your first project.";
  private static final String EMPTY_SEARCH_RESULT_MESSAGE = "No projects found.";
  /**
   * Deterministic tie-break key appended to every sort order so offset/limit pagination never
   * duplicates or drops items across page boundaries (ADR-0007). Only consulted when the primary
   * sort attributes are exactly equal.
   */
  private static final SortOrder SORT_TIE_BREAKER = new SortOrder("projectCode", false);
  final TextField projectSearchField = new TextField();
  final Button sortButton = new Button();
  final ContextMenu sortMenu = new ContextMenu(sortButton);
  final Button createProjectButton = new Button("Create project");
  final Div projectCards = new Div();
  /** Full pager below the list (info + numbered window + prev/next + page size). */
  final PaginationBar paginationBar = new PaginationBar(ListStateCodec.ALLOWED_PAGE_SIZES,
      ListStateCodec.DEFAULT_PAGE_SIZE, "projects");
  private final Div header = new Div();
  private final Span emptyStateMessage = new Span();
  private final transient ProjectInformationService projectInformationService;
  /**
   * The user's quick-access shortlist, rendered above the list controls. Owned here because the
   * overview cards and the shortlist share one pin toggle handler and must stay in sync.
   */
  private final PinnedProjectsComponent pinnedProjectsComponent;
  private final transient PinnedProjectService pinnedProjectService;
  private final transient MessageSourceNotificationFactory notificationFactory;
  /**
   * The overviews rendered on the current page; reused to re-render the card toggle states after a pin
   * change without querying the project list again.
   */
  private List<ProjectOverview> currentOverviews = List.of();
  /**
   * The currently applied list state; {@code null} until the first list state has been applied
   * (initial page load), which guarantees the initial load is never skipped as "unchanged".
   */
  private ListState listState;

  public ProjectCollectionComponent(ProjectInformationService projectInformationService,
      PinnedProjectService pinnedProjectService,
      MessageSourceNotificationFactory notificationFactory) {
    this.projectInformationService = Objects.requireNonNull(projectInformationService,
        "Project information service cannot be null");
    this.pinnedProjectService = Objects.requireNonNull(pinnedProjectService,
        "pinnedProjectService cannot be null");
    this.notificationFactory = Objects.requireNonNull(notificationFactory,
        "notificationFactory cannot be null");
    this.pinnedProjectsComponent = new PinnedProjectsComponent(
        pinnedProjectService::findPinnedProjects, this::handlePinToggle);
    layoutComponent();
    configureSearch();
    configureSortButton();
    configureProjectCreationButton();
    configurePagination();
  }

  private void initHeader() {
    header.addClassName("header");
    // Title row: title on the left, Create button on the far right
    Div titleRow = new Div();
    titleRow.addClassName("title-row");
    Span title = new Span("My Research Projects");
    title.addClassName("title");
    createProjectButton.addClassName("primary");
    titleRow.add(title, createProjectButton);
    // Controls row: search + sort dropdown
    projectSearchField.setPlaceholder("Search");
    projectSearchField.setClearButtonVisible(true);
    projectSearchField.setSuffixComponent(VaadinIcon.SEARCH.create());
    projectSearchField.addClassNames("search-field");
    configureSortButton();
    Span controls = new Span(projectSearchField, sortButton);
    controls.addClassName("controls");
    // The shortlist is the first child of the header, i.e. above the title and the search controls:
    // it is a personal toolbar, not part of the filtered result set (FEAT-PINNED-01).
    header.add(pinnedProjectsComponent, titleRow, controls);
    add(header);
  }

  private void layoutComponent() {
    addClassNames("project-collection-component");
    initHeader();
    layoutCards();
    layoutEmptyState();
    add(paginationBar);
  }

  private void layoutCards() {
    projectCards.addClassName("project-card-grid");
    add(projectCards);
  }

  private void layoutEmptyState() {
    emptyStateMessage.addClassName("empty-state");
    emptyStateMessage.setVisible(false);
    add(emptyStateMessage);
  }

  private void configureSearch() {
    projectSearchField.setValueChangeMode(ValueChangeMode.LAZY);
    projectSearchField.addValueChangeListener(event -> {
      String filter = event.getValue().trim();
      if (filter.equals(listState.filter())) {
        return;
      }
      // A search always resets paging to the first page so the result set is shown from its start.
      applyStateAtUrl(listState.withFilter(filter).withPage(1), HistoryMode.REPLACE);
    });
  }

  private void configureSortButton() {
    sortButton.addClassName("sort-button");
    sortButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    sortButton.setIcon(VaadinIcon.SORT.create());
    sortButton.setText("Sort");
    sortButton.setAriaLabel("Sort options");
    sortMenu.setOpenOnClick(true);
    sortMenu.removeAll();

    // Group: Last modified
    MenuItem lastModifiedDesc = sortMenu.addItem("Last modified (newest first)");
    lastModifiedDesc.setCheckable(true);
    lastModifiedDesc.addClickListener(event -> {
      if (!LAST_MODIFIED_DESC.toSortOrder().equals(listState.sort())) {
        applyStateAtUrl(listState.withSort(LAST_MODIFIED_DESC.toSortOrder()).withPage(1), HistoryMode.PUSH);
      }
    });

    MenuItem lastModifiedAsc = sortMenu.addItem("Last modified (oldest first)");
    lastModifiedAsc.setCheckable(true);
    lastModifiedAsc.addClickListener(event -> {
      if (!LAST_MODIFIED_ASC.toSortOrder().equals(listState.sort())) {
        applyStateAtUrl(listState.withSort(LAST_MODIFIED_ASC.toSortOrder()).withPage(1), HistoryMode.PUSH);
      }
    });

    // Divider
    sortMenu.addSeparator();

    // Group: Project title
    MenuItem titleAsc = sortMenu.addItem("Project title (A–Z)");
    titleAsc.setCheckable(true);
    titleAsc.addClickListener(event -> {
      if (!TITLE_ASC.toSortOrder().equals(listState.sort())) {
        applyStateAtUrl(listState.withSort(TITLE_ASC.toSortOrder()).withPage(1), HistoryMode.PUSH);
      }
    });

    MenuItem titleDesc = sortMenu.addItem("Project title (Z–A)");
    titleDesc.setCheckable(true);
    titleDesc.addClickListener(event -> {
      if (!TITLE_DESC.toSortOrder().equals(listState.sort())) {
        applyStateAtUrl(listState.withSort(TITLE_DESC.toSortOrder()).withPage(1), HistoryMode.PUSH);
      }
    });

    // Divider
    sortMenu.addSeparator();

    // Group: Project code
    MenuItem codeAsc = sortMenu.addItem("Project code (A–Z)");
    codeAsc.setCheckable(true);
    codeAsc.addClickListener(event -> {
      if (!CODE_ASC.toSortOrder().equals(listState.sort())) {
        applyStateAtUrl(listState.withSort(CODE_ASC.toSortOrder()).withPage(1), HistoryMode.PUSH);
      }
    });

    MenuItem codeDesc = sortMenu.addItem("Project code (Z–A)");
    codeDesc.setCheckable(true);
    codeDesc.addClickListener(event -> {
      if (!CODE_DESC.toSortOrder().equals(listState.sort())) {
        applyStateAtUrl(listState.withSort(CODE_DESC.toSortOrder()).withPage(1), HistoryMode.PUSH);
      }
    });

    updateSortCheckmarks();
  }

  private void updateSortCheckmarks() {
    if (listState == null) {
      return;
    }
    SortOrder currentSort = listState.sort();
    sortMenu.getChildren().forEach(component -> {
      if (component instanceof MenuItem item) {
        String text = item.getText();
        ProjectOverviewSortOption option = findOptionByText(text);
        if (option != null) {
          item.setChecked(option.toSortOrder().equals(currentSort));
        }
      }
    });
  }

  private ProjectOverviewSortOption findOptionByText(String text) {
    for (ProjectOverviewSortOption option : ProjectOverviewSortOption.values()) {
      if (option.label().equals(text)) {
        return option;
      }
    }
    return null;
  }

  private void configurePagination() {
    paginationBar.addChangeListener(this::applyPaginationChange);
  }

  /**
   * Applies a page or page-size change requested from the pagination bar; the bar is kept in sync
   * by {@link #loadPage(ListState)}, which reports the applied state back to it.
   */
  private void applyPaginationChange(PaginationBar.ChangeEvent event) {
    if (event.getPageSize() != listState.pageSize()) {
      applyStateAtUrl(listState.withPageSize(event.getPageSize()), HistoryMode.PUSH);
    } else if (event.getPage() != listState.page()) {
      applyStateAtUrl(listState.withPage(event.getPage()), HistoryMode.PUSH);
    }
  }

  private void configureProjectCreationButton() {
    createProjectButton.addClickListener(listener -> fireCreateClickedEvent());
  }

  /**
   * Applies an externally provided list state (initial page load, reload, browser back/forward or a
   * shared link) without touching the browser URL.
   *
   * @param state the list state parsed from the current URL
   */
  public void applyExternalState(ListState state) {
    if (state.equals(listState)) {
      return;
    }
    loadPage(state);
    syncControls(listState);
  }

  /**
   * Applies a user-initiated state change and mirrors the applied state into the browser URL.
   */
  private void applyStateAtUrl(ListState state, HistoryMode mode) {
    loadPage(state);
    syncControls(listState);
    writeUrl(listState, mode);
  }

  /**
   * Loads the current page of project overviews for the given state and renders it. When the
   * requested page lies beyond the last page (e.g. after a filter reduced the result set), the page
   * is clamped to the last valid page.
   */
  private void loadPage(ListState state) {
    List<SortOrder> sortOrders = new ArrayList<>();
    sortOrders.add(state.sort());
    sortOrders.add(SORT_TIE_BREAKER);
    List<ProjectOverview> overviews = projectInformationService.queryOverview(state.filter(),
        (state.page() - 1) * state.pageSize(), state.pageSize(), sortOrders);
    long total = projectInformationService.countOverview(state.filter());
    int totalPages = Math.max(1, (int) Math.ceil((double) total / state.pageSize()));
    int page = Math.min(state.page(), totalPages);
    if (page != state.page()) {
      // bounded recursion: the clamped page is always within range, so this branch runs at most once
      ListState clampedState = state.withPage(page);
      this.listState = clampedState;
      loadPage(clampedState);
      return;
    }
    this.listState = state;
    renderCards(overviews);
    paginationBar.setListState(page, total, state.pageSize());
    // The pager stays hidden for an empty result set (the empty-state message covers that case).
    paginationBar.setVisible(total > 0);
    renderEmptyState(overviews.isEmpty(), state.filter().isBlank());
  }

  private void renderCards(List<ProjectOverview> overviews) {
    this.currentOverviews = overviews;
    var pinnedProjectIds = pinnedProjectsComponent.pinnedProjectIds();
    projectCards.removeAll();
    overviews.forEach(overview -> projectCards.add(
        new ProjectOverviewItem(overview, pinnedProjectIds.contains(overview.projectId()),
            this::handlePinToggle)));
  }

  /**
   * Applies a pin toggle coming from either the shortlist or an overview card, then re-renders both.
   *
   * <p>The expected failure paths are returned outcomes, not exceptions. Only the limit needs a user
   * message: the user must learn the rule and how to work around it, because the alternative —
   * silently dropping an existing pin — would remove something the user asked for.
   */
  private void handlePinToggle(ProjectId projectId, boolean pin) {
    PinOutcome outcome =
        pin ? pinnedProjectService.pin(projectId) : pinnedProjectService.unpin(projectId);
    if (outcome == PinOutcome.LIMIT_REACHED) {
      notificationFactory.toast("project.pinned.limit",
          new Object[]{PinnedProjectService.MAX_PINNED_PROJECTS}, getLocale()).open();
    }
    pinnedProjectsComponent.refresh();
    renderCards(currentOverviews);
  }

  private void renderEmptyState(boolean isEmpty, boolean noActiveFilter) {
    if (isEmpty) {
      // The onboarding text is only correct for an empty collection; a search without matches must
      // not claim that no projects exist at all.
      emptyStateMessage.setText(
          noActiveFilter ? EMPTY_PROJECT_COLLECTION_MESSAGE : EMPTY_SEARCH_RESULT_MESSAGE);
    }
    emptyStateMessage.setVisible(isEmpty);
  }

  private void syncControls(ListState state) {
    if (!Objects.equals(projectSearchField.getValue().trim(), state.filter())) {
      projectSearchField.setValue(state.filter());
    }
    updateSortCheckmarks();
  }

  private void writeUrl(ListState state, HistoryMode mode) {
    UI ui = UI.getCurrent();
    if (ui == null) {
      return;
    }
    Location location = new Location(ProjectRoutes.PROJECTS,
        ListStateCodec.toQueryParameters(state));
    if (mode == HistoryMode.PUSH) {
      ui.getPage().getHistory().pushState(null, location);
    } else {
      ui.getPage().getHistory().replaceState(null, location);
    }
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

  /**
   * Reloads the current page with the current list state (e.g. after a project was created).
   */
  public void refresh() {
    loadPage(listState);
  }

  /**
   * Resets the search filter and returns to the first page, mirroring the applied state into the
   * URL. Used after a project was created so the new project becomes visible.
   */
  public void resetSearch() {
    applyStateAtUrl(listState.withFilter("").withPage(1), HistoryMode.REPLACE);
  }

  /**
   * Distinguishes how a list state change is mirrored into the browser history: page-level
   * navigation pushes a new history entry (back/forward steps through list states), while search
   * typing replaces the current entry to avoid history spam.
   */
  private enum HistoryMode {
    PUSH,
    REPLACE
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

    public ProjectOverviewItem(ProjectOverview projectOverview, boolean pinned,
        ToggleHandler toggleHandler) {
      this.projectOverview = Objects.requireNonNull(projectOverview);
      Objects.requireNonNull(toggleHandler);
      // Both RouterLinks (card body + footer) must share a single parent so they render
      // as one unified card. Using a wrapper Div prevents event propagation between
      // clicks on the footer and clicks on the card body.
      var wrapper = new Div();
      wrapper.addClassName("project-card-wrapper");
      wrapper.add(projectInfoLink());
      attachDatasetFooter(wrapper);
      wrapper.add(buildPinToggle(pinned, toggleHandler));
      add(wrapper);
    }

    /**
     * Builds the pin toggle for this card.
     *
     * <p>The toggle is a sibling of the card-body {@link RouterLink} inside the card wrapper, not a
     * child of it, so clicking the star cannot also fire the navigation to the project. The filled
     * star both indicates the pinned state and offers the unpin action.</p>
     */
    private Button buildPinToggle(boolean pinned, ToggleHandler toggleHandler) {
      var button = new Button(new Icon(pinned ? VaadinIcon.STAR : VaadinIcon.STAR_O));
      button.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
      button.addClassName("project-card-pin-toggle");
      if (pinned) {
        button.addClassName("is-pinned");
      }
      var action = "%s %s".formatted(pinned ? "Unpin" : "Pin", projectOverview.projectCode());
      button.getElement().setAttribute("aria-label", action);
      button.getElement().setAttribute("title", action);
      button.addClickListener(
          event -> toggleHandler.onToggle(projectOverview.projectId(), !pinned));
      return button;
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
     * Attaches the connected-dataset footer to the card body when the project has any connected
     * datasets. The footer is rendered as a full-width {@link RouterLink} to the project's
     * connected-datasets view; it is a sibling (not a child) of the card-body RouterLink, so each
     * click target has exactly one handler and neither can fire the other's navigation — no
     * event-propagation workaround required.
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

      // ── Wrap in RouterLink (native <a> semantics) ─────────────
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
      return DateTimeFormat.asJavaFormatter(DateTimeFormat.SIMPLE_DATE_SHORT,
          ZoneId.systemDefault()).format(instant);
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
        String full = DateTimeFormat.asJavaFormatter(DateTimeFormat.SIMPLE_DATE_SHORT,
            ZoneId.systemDefault()).format(lastConnected);
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