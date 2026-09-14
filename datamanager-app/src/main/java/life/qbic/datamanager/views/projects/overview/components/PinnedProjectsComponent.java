package life.qbic.datamanager.views.projects.overview.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;
import java.time.Instant;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import life.qbic.application.commons.time.DateTimeFormat;
import life.qbic.datamanager.views.general.Tag;
import life.qbic.datamanager.views.general.Tag.TagColor;
import life.qbic.datamanager.views.projects.overview.components.ProjectCollectionComponent.MeasurementType;
import life.qbic.datamanager.views.projects.project.info.ProjectInformationMain;
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
 * @since 1.12.0
 */
public class PinnedProjectsComponent extends Div {

  private static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";

  private final Div cards = new Div();
  private final transient Supplier<List<PinnedProjectView>> pinnedProjectsSupplier;
  private final transient ToggleHandler toggleHandler;
  private List<PinnedProjectView> pinnedProjects = List.of();

  /**
   * @param pinnedProjectsSupplier supplies the current user's pins; called on every {@link #refresh()},
   *                               so the row never owns access-resolution logic
   * @param toggleHandler          receives pin and unpin requests raised from this row
   */
  public PinnedProjectsComponent(Supplier<List<PinnedProjectView>> pinnedProjectsSupplier,
      ToggleHandler toggleHandler) {
    this.pinnedProjectsSupplier = Objects.requireNonNull(pinnedProjectsSupplier,
        "pinnedProjectsSupplier cannot be null");
    this.toggleHandler = Objects.requireNonNull(toggleHandler, "toggleHandler cannot be null");
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
    pinnedProjects.forEach(this::addCard);
    setVisible(!pinnedProjects.isEmpty());
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
   * @since 1.12.0
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
    link.add(buildPinnedLine(pinnedProject.pinnedAt()));
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
    body.add(buildPinnedLine(pinnedProject.pinnedAt()));
    var hint = new Span("You no longer have access to this project.");
    hint.addClassName("pinned-project-hint");
    body.add(hint);
    return body;
  }

  private Span buildTitleLine(PinnedProjectView pinnedProject) {
    var label = "%s - %s".formatted(pinnedProject.projectCode(), pinnedProject.projectTitle());
    var title = new Span(label);
    title.addClassName("pinned-project-title");
    // The visible text is truncated by CSS; the full label stays available on hover and to
    // assistive technology.
    title.setTitle(label);
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

  private Span buildPinnedLine(Instant pinnedAt) {
    var formatted = DateTimeFormat.asJavaFormatter(DateTimeFormat.SIMPLE_DATE_SHORT,
        ZoneId.systemDefault()).format(pinnedAt);
    var label = new Span("Pinned on %s".formatted(formatted));
    label.addClassName("pinned-project-secondary");
    return label;
  }

  /**
   * The unpin control. Rendered for every pin, including revoked ones, because a pin its owner cannot
   * remove would hold one of the bounded places forever (ADR-0008).
   */
  private Button buildToggleButton(PinnedProjectView pinnedProject) {
    var button = new Button(new Icon(VaadinIcon.STAR));
    button.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
    button.getElement().setAttribute("aria-label", "Unpin %s".formatted(pinnedProject.projectCode()));
    button.getElement().setAttribute("title", "Unpin this project");
    button.addClassName("pinned-project-toggle");
    button.addClickListener(event -> toggleHandler.onToggle(pinnedProject.projectId(), false));
    return button;
  }
}
