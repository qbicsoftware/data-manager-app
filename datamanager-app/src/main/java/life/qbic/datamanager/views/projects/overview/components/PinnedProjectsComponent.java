package life.qbic.datamanager.views.projects.overview.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.theme.lumo.LumoUtility.IconSize;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;
import java.time.Instant;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import life.qbic.application.commons.time.DateTimeFormat;
import life.qbic.datamanager.views.general.ProjectCodeBadge;
import life.qbic.datamanager.views.general.Tag;
import life.qbic.datamanager.views.general.Tag.TagColor;
import life.qbic.datamanager.views.projects.overview.components.ProjectCollectionComponent.MeasurementType;
import life.qbic.datamanager.views.projects.project.info.ProjectInformationMain;
import life.qbic.projectmanagement.application.PinnedProjectService;
import life.qbic.projectmanagement.application.pinned.PinnedProjectView;
import life.qbic.projectmanagement.domain.model.project.ProjectId;

/**
 * <b>Pinned projects</b>
 * <p>
 * The user-curated quick-access shortlist ({@code USER-R-04}) rendered above the project overview
 * list controls, so it behaves as a personal toolbar rather than as part of the result set. The row
 * is independent of search, sorting and paging: it shows the same projects while the list below is
 * filtered (FEAT-PINNED-01).
 * <p>
 * Two card states exist:
 * <ul>
 *   <li><b>accessible</b> — code, title and measurement types read from the live, access-restricted
 *   project overview; clicking opens the project;</li>
 *   <li><b>revoked</b> — the project is no longer readable, so the card carries only the label that
 *   was captured when the pin was created plus an explicit no-access indication. It is not a link and
 *   exposes no other project data.</li>
 * </ul>
 * Both states expose an unpin control. The control is a <em>sibling</em> of the card's
 * {@link RouterLink}, never a descendant, so a toggle click cannot also trigger navigation — the same
 * topology the project cards use for their dataset footer.
 *
 * @since 1.19.0
 */
public class PinnedProjectsComponent extends Div {

  private static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";

  private final Div cards = new Div();
  private final transient Supplier<List<PinnedProjectView>> pinnedProjectsSupplier;
  private final transient ToggleHandler toggleHandler;
  private final transient PinnedProjectActionHandler actionHandler;
  /** The unpin action per pinned project, rebuilt on every refresh. Kept as {@link Runnable}s so
   * unit tests (and future programmatic triggers) can invoke the unpin action without simulating
   * browser menu interaction. */
  private final transient Map<ProjectId, Runnable> unpinActions = new LinkedHashMap<>();
  private List<PinnedProjectView> pinnedProjects = List.of();

  /**
   * @param pinnedProjectsSupplier supplies the current user's pins; called on every {@link #refresh()},
   *                               so the row never owns access-resolution logic
   * @param toggleHandler          receives pin and unpin requests raised from this row
   * @param actionHandler          exposes quick sharing / access management for manageable pins, so
   *                               the shortlist offers the same project actions as the overview cards
   */
  public PinnedProjectsComponent(Supplier<List<PinnedProjectView>> pinnedProjectsSupplier,
      ToggleHandler toggleHandler, PinnedProjectActionHandler actionHandler) {
    this.pinnedProjectsSupplier = Objects.requireNonNull(pinnedProjectsSupplier,
        "pinnedProjectsSupplier cannot be null");
    this.toggleHandler = Objects.requireNonNull(toggleHandler, "toggleHandler cannot be null");
    this.actionHandler = Objects.requireNonNull(actionHandler, "actionHandler cannot be null");
    addClassName("pinned-projects");
    setVisible(false);
    var title = new Span("Pinned projects");
    title.addClassName("pinned-projects-title");
    cards.addClassName("pinned-projects-cards");
    add(title, cards);
    refresh();
  }

  /**
   * Reloads the current user's pins and re-renders the row. Hidden when the user has no pins, so a
   * user who never pinned anything sees the project overview exactly as before this feature.
   */
  public final void refresh() {
    pinnedProjects = pinnedProjectsSupplier.get();
    cards.removeAll();
    unpinActions.clear();
    pinnedProjects.forEach(this::addCard);
    setVisible(!pinnedProjects.isEmpty());
    updateCountIndicator();
  }

  /**
   * Updates the title to show the current pin count and remaining slots, and applies a count-based
   * class to the cards container so the grid uses the matching column layout.
   */
  private void updateCountIndicator() {
    int count = pinnedProjects.size();
    int remaining = PinnedProjectService.MAX_PINNED_PROJECTS - count;
    getChildren().forEach(child -> {
      if (child instanceof Span span && span.getClassNames().contains("pinned-projects-title")) {
        if (remaining > 0) {
          span.setText("Pinned projects (%d/%d)".formatted(count, PinnedProjectService.MAX_PINNED_PROJECTS));
        } else {
          span.setText("Pinned projects (%d/%d — full)".formatted(count, PinnedProjectService.MAX_PINNED_PROJECTS));
        }
      }
    });
    cards.getClassNames().removeIf(c -> c.startsWith("pinned-count-"));
    cards.addClassName("pinned-count-%d".formatted(count));
  }

  /**
   * @return the project ids the current user has pinned; drives the toggle state of the overview cards
   */
  public Set<ProjectId> pinnedProjectIds() {
    return pinnedProjects.stream().map(PinnedProjectView::projectId)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  public boolean isPinned(ProjectId projectId) {
    return pinnedProjectIds().contains(projectId);
  }

  /**
   * Handles a pin toggle coming from this row.
   *
   * @since 1.19.0
   */
  @FunctionalInterface
  public interface ToggleHandler {

    /**
     * @param projectId the project the user acted on
     * @param pin       {@code true} to pin, {@code false} to unpin
     */
    void onToggle(ProjectId projectId, boolean pin);
  }

  /**
   * Builds one card of the shortlist. Accessible pins render as a card body link with the live
   * project label; revoked pins render as a non-clickable placeholder with the pin-time label.
   */
  private void addCard(PinnedProjectView pinnedProject) {
    var wrapper = new Div();
    wrapper.addClassName("pinned-project-card");
    if (pinnedProject.isAccessible()) {
      wrapper.add(buildCardBody(pinnedProject));
    } else {
      wrapper.addClassName("no-access");
      wrapper.add(buildRevokedBody(pinnedProject));
    }
    wrapper.add(buildToggleButton(pinnedProject));
    cards.add(wrapper);
  }

  private RouterLink buildCardBody(PinnedProjectView pinnedProject) {
    var link = new RouterLink("", ProjectInformationMain.class,
        new RouteParameters(PROJECT_ID_ROUTE_PARAMETER, pinnedProject.projectId().value()));
    link.addClassName("pinned-project-card-body");
    link.add(buildTitleLine(pinnedProject));
    link.add(buildMeasurementTags(pinnedProject));
    link.add(buildLastModifiedLine(pinnedProject));
    return link;
  }

  private Div buildRevokedBody(PinnedProjectView pinnedProject) {
    var body = new Div();
    body.addClassName("pinned-project-card-body");
    body.add(buildTitleLine(pinnedProject));
    var noAccess = new Tag("No access");
    noAccess.setTagColor(TagColor.WARNING);
    var tags = new Span(noAccess);
    tags.addClassName("tag-collection");
    body.add(tags);
    body.add(buildPinnedAtLine(pinnedProject.pinnedAt()));
    var hint = new Span("You no longer have access to this project.");
    hint.addClassName("pinned-project-hint");
    body.add(hint);
    return body;
  }

  private Span buildTitleLine(PinnedProjectView pinnedProject) {
    var title = new Span();
    title.add(new ProjectCodeBadge(pinnedProject.projectCode()));
    title.add(new Span(" " + pinnedProject.projectTitle()));
    title.addClassName("pinned-project-title");
    // The visible text is truncated by CSS; the full label stays available on hover and to
    // assistive technology.
    title.setTitle("%s - %s".formatted(pinnedProject.projectCode(), pinnedProject.projectTitle()));
    return title;
  }

  private Span buildMeasurementTags(PinnedProjectView pinnedProject) {
    var tags = new Span();
    tags.addClassName("tag-collection");
    if (pinnedProject.pxpMeasurementCount() > 0) {
      tags.add(buildMeasurementTag(MeasurementType.PROTEOMICS));
    }
    if (pinnedProject.ngsMeasurementCount() > 0) {
      tags.add(buildMeasurementTag(MeasurementType.GENOMICS));
    }
    if (pinnedProject.ipMeasurementCount() > 0) {
      tags.add(buildMeasurementTag(MeasurementType.IMMUNOPEPTIDOMICS));
    }
    return tags;
  }

  private Tag buildMeasurementTag(MeasurementType measurementType) {
    var tag = new Tag(measurementType.getType());
    tag.setTagColor(switch (measurementType) {
      case PROTEOMICS -> TagColor.VIOLET;
      case GENOMICS -> TagColor.PINK;
      case IMMUNOPEPTIDOMICS -> TagColor.GOLD;
    });
    return tag;
  }

  /**
   * The last-modified line for an accessible pin. This is more actionable than the pin date: it
   * tells the user whether anything has changed in the project since they last looked, which is the
   * primary reason to revisit a pinned project.
   */
  private Span buildLastModifiedLine(PinnedProjectView pinnedProject) {
    Instant lastModified = pinnedProject.lastModified();
    if (lastModified == null) {
      return new Span();
    }
    var formatted = DateTimeFormat.asJavaFormatter(DateTimeFormat.SIMPLE_DATE_SHORT,
        ZoneId.systemDefault()).format(lastModified);
    var label = new Span("Modified %s".formatted(formatted));
    label.addClassName("pinned-project-secondary");
    return label;
  }

  /**
   * The pin-date line, shown only for revoked pins where no live project data is available. For
   * accessible pins the last-modified date replaces this — see {@link #buildLastModifiedLine}.
   */
  private Span buildPinnedAtLine(Instant pinnedAt) {
    var formatted = DateTimeFormat.asJavaFormatter(DateTimeFormat.SIMPLE_DATE_SHORT,
        ZoneId.systemDefault()).format(pinnedAt);
    var label = new Span("Pinned on %s".formatted(formatted));
    label.addClassName("pinned-project-secondary");
    return label;
  }

  /**
   * The unpin control. Rendered as a kebab menu for every pin, including revoked ones, because a
   * pin its owner cannot remove would hold one of the bounded places forever (ADR-0008).
   */
  private Button buildToggleButton(PinnedProjectView pinnedProject) {
    var button = new Button(VaadinIcon.ELLIPSIS_DOTS_H.create());
    button.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
    button.getElement().setAttribute("aria-label", "Options for pinned project %s".formatted(pinnedProject.projectCode()));
    button.getElement().setAttribute("title", "Options");
    button.addClassName("pinned-project-toggle");

    var menu = new ContextMenu(button);
    menu.setOpenOnClick(true);
    if (pinnedProject.isAccessible()
        && actionHandler.canManageAccess(pinnedProject.projectId())) {
      // Same project actions as the overview card menu, so a pinned project is not a dead end.
      menu.addItem(menuItemWithIcon("Share project…", VaadinIcon.SHARE),
          event -> actionHandler.share(pinnedProject.projectId(),
              "%s — %s".formatted(pinnedProject.projectCode(), pinnedProject.projectTitle())));
      menu.addItem(menuItemWithIcon("Manage access", VaadinIcon.GROUP),
          event -> actionHandler.manageAccess(pinnedProject.projectId()));
    }
    menu.addItem(menuItemWithIcon("Unpin project", VaadinIcon.PIN, true),
        event -> toggleHandler.onToggle(pinnedProject.projectId(), false));
    unpinActions.put(pinnedProject.projectId(),
        () -> toggleHandler.onToggle(pinnedProject.projectId(), false));

    return button;
  }

  private static Span menuItemWithIcon(String label, VaadinIcon icon) {
    return menuItemWithIcon(label, icon, false);
  }

  /**
   * Builds a menu item with a small icon. {@code rotated} tilts the glyph counter-clockwise,
   * used for the unpin action: there is no dedicated unpin icon, so a rotated pin reads as
   * "remove the pin".
   */
  private static Span menuItemWithIcon(String label, VaadinIcon icon, boolean rotated) {
    Icon iconComponent = icon.create();
    iconComponent.addClassName(IconSize.SMALL);
    if (rotated) {
      iconComponent.addClassName("menu-icon-unpin");
    }
    Span item = new Span(iconComponent, new Span(label));
    item.addClassName("user-menu-item");
    item.getStyle().set("display", "inline-flex");
    item.getStyle().set("align-items", "center");
    item.getStyle().set("gap", "var(--spacing-03)");
    return item;
  }

  /**
   * Project actions offered for an accessible pinned project. The shortlist does not own the access
   * services; the owning collection component implements these callbacks.
   *
   * @since 1.20.0
   */
  public interface PinnedProjectActionHandler {

    boolean canManageAccess(ProjectId projectId);

    void share(ProjectId projectId, String projectLabel);

    void manageAccess(ProjectId projectId);
  }
}
