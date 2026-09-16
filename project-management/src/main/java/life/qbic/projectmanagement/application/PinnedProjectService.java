package life.qbic.projectmanagement.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import life.qbic.application.commons.SortOrder;
import life.qbic.identity.api.AuthenticationToUserIdTranslator;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.api.PinnedProjectStore;
import life.qbic.projectmanagement.application.api.ProjectOverviewLookup;
import life.qbic.projectmanagement.application.pinned.PinnedProject;
import life.qbic.projectmanagement.application.pinned.PinnedProjectView;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for the user-curated pinned projects of {@code USER-R-04}.
 * <p>
 * A pin is an application-layer user preference, not project state (ADR-0008). Consequently:
 * <ul>
 *   <li>pinning and unpinning never load or write the {@code Project} aggregate, so
 *   {@code Project#lastModified} is untouched;</li>
 *   <li>creating a pin requires read access to the project, removing one does not;</li>
 *   <li>every read path resolves the user's accessible projects first and intersects, exactly like
 *   {@link ProjectInformationService#queryOverview(String, int, int, List)} does, so a pin can never
 *   become a channel for project data the user is not entitled to see.</li>
 * </ul>
 *
 * @since 1.12.0
 */
@Service
public class PinnedProjectService {

  /**
   * Upper bound of pins one user may hold. The bound is a product parameter, not a requirement
   * detail: it keeps the curated set a shortlist. Pins of projects the user can no longer read count
   * towards the bound, because they stay visible and therefore stay removable.
   *
   * <p>Six allows a balanced 3×2 (or 2×3) grid on desktop while staying a shortlist; five would
   * force an uneven wrap on the row's default layout (FEAT-PINNED-01).
   */
  public static final int MAX_PINNED_PROJECTS = 6;

  private static final Logger log = LoggerFactory.logger(PinnedProjectService.class);
  /** Deterministic ordering for the overview lookup of the intersected pinned projects. */
  private static final List<SortOrder> OVERVIEW_SORT = List.of(new SortOrder("projectCode", false));

  private final PinnedProjectStore pinnedProjectStore;
  private final ProjectOverviewLookup projectOverviewLookup;
  private final ProjectInformationService projectInformationService;
  private final AuthenticationToUserIdTranslator userIdTranslator;

  public PinnedProjectService(@Autowired PinnedProjectStore pinnedProjectStore,
      @Autowired ProjectOverviewLookup projectOverviewLookup,
      @Autowired ProjectInformationService projectInformationService,
      @Autowired AuthenticationToUserIdTranslator userIdTranslator) {
    this.pinnedProjectStore = Objects.requireNonNull(pinnedProjectStore,
        "pinnedProjectStore cannot be null");
    this.projectOverviewLookup = Objects.requireNonNull(projectOverviewLookup,
        "projectOverviewLookup cannot be null");
    this.projectInformationService = Objects.requireNonNull(projectInformationService,
        "projectInformationService cannot be null");
    this.userIdTranslator = Objects.requireNonNull(userIdTranslator,
        "userIdTranslator cannot be null");
  }

  /**
   * Returns the pinned projects of the currently authenticated user, most recently pinned first.
   *
   * <p>Pins of projects the user can still read carry live overview data; all other pins are
   * returned as {@link PinnedProjectView.AccessState#REVOKED} entries carrying only the label that was
   * captured when the user created them. The returned list contains <b>every</b> stored pin of the
   * user, so an unreadable pin never silently shrinks the result.
   *
   * @return the user's pins in pinning order; empty if the user has no pins or is not authenticated
   */
  public List<PinnedProjectView> findPinnedProjects() {
    Optional<String> userId = currentUserId();
    if (userId.isEmpty()) {
      return List.of();
    }
    List<PinnedProject> pins = pinnedProjectStore.findByUserId(userId.get());
    if (pins.isEmpty()) {
      return List.of();
    }
    Map<ProjectId, ProjectOverview> readableOverviews = findReadableOverviews(pins);
    List<PinnedProjectView> views = new ArrayList<>(pins.size());
    for (PinnedProject pin : pins) {
      ProjectOverview overview = readableOverviews.get(pin.projectId());
      views.add(
          overview != null ? PinnedProjectView.liveOf(pin, overview) : PinnedProjectView.placeholderOf(
              pin));
    }
    // Defensive re-sort: the store already returns newest-first via `ORDER BY
    // pinnedAt DESC`, but sorting here again with full Instant precision keeps the
    // contract ("most recently pinned first") correct even if a deployed database
    // column or migration stores coarser precision (e.g. a plain `datetime` table
    // instead of `datetime(6)`), where several pins could share one timestamp and
    // the database tie-break alone would decide the order. The sort is stable, so
    // equal timestamps keep the database order.
    views.sort(
        java.util.Comparator.comparing(PinnedProjectView::pinnedAt).reversed());
    return views;
  }

  /**
   * Pins a project for the currently authenticated user.
   *
   * <p>The pin-time label is taken from the access-restricted project overview, so no unchecked
   * project read is introduced.
   *
   * @param projectId the project to pin
   * @return the outcome; never throws for the expected failure paths (see {@link PinOutcome})
   */
  @Transactional(propagation = Propagation.REQUIRED, timeout = 15)
  public PinOutcome pin(ProjectId projectId) {
    Objects.requireNonNull(projectId, "projectId cannot be null");
    Optional<String> userId = currentUserId();
    if (userId.isEmpty()) {
      return PinOutcome.NO_ACTIVE_USER;
    }
    if (pinnedProjectStore.find(userId.get(), projectId).isPresent()) {
      return PinOutcome.ALREADY_PINNED;
    }
    if (pinnedProjectStore.countByUserId(userId.get()) >= MAX_PINNED_PROJECTS) {
      return PinOutcome.LIMIT_REACHED;
    }
    ProjectOverview overview = findAccessibleOverview(projectId).orElse(null);
    if (overview == null) {
      return PinOutcome.NOT_PERMITTED;
    }
    pinnedProjectStore.add(
        PinnedProject.create(userId.get(), projectId, Instant.now(), overview.projectCode(),
            overview.projectTitle()));
    log.debug(String.format("User '%s' pinned project '%s'", userId.get(), projectId.value()));
    return PinOutcome.PINNED;
  }

  /**
   * Removes a project from the pinned projects of the currently authenticated user.
   *
   * <p>Intentionally performs <b>no</b> project access check: an unreadable project must stay
   * unpinnable by its owner, otherwise a pin would occupy a place of the bounded set forever
   * (ADR-0008). Only the user's own pin row is touched.
   *
   * @param projectId the project to unpin
   * @return the outcome; {@link PinOutcome#NOT_PINNED} if the user holds no such pin
   */
  @Transactional(propagation = Propagation.REQUIRED, timeout = 15)
  public PinOutcome unpin(ProjectId projectId) {
    Objects.requireNonNull(projectId, "projectId cannot be null");
    Optional<String> userId = currentUserId();
    if (userId.isEmpty()) {
      return PinOutcome.NO_ACTIVE_USER;
    }
    boolean removed = pinnedProjectStore.remove(userId.get(), projectId);
    if (!removed) {
      return PinOutcome.NOT_PINNED;
    }
    log.debug(String.format("User '%s' unpinned project '%s'", userId.get(), projectId.value()));
    return PinOutcome.UNPINNED;
  }

  /**
   * @return {@code true} if the current user has at least one pinned project
   */
  public boolean hasPinnedProjects() {
    Optional<String> userId = currentUserId();
    return userId.filter(s -> pinnedProjectStore.countByUserId(s) > 0).isPresent();
  }

  /**
   * Resolves the live overviews of all pins the current user may still read.
   *
   * <p>Runs one access-restricted overview query for the intersection of the user's pinned project
   * ids and the projects they can access. Ids without a resulting row are unreadable and become
   * revoked views.
   */
  private Map<ProjectId, ProjectOverview> findReadableOverviews(List<PinnedProject> pins) {
    List<ProjectId> pinnedIds = pins.stream().map(PinnedProject::projectId).toList();
    List<ProjectId> readableIds = intersectWithAccessibleProjects(pinnedIds);
    if (readableIds.isEmpty()) {
      return Map.of();
    }
    return projectOverviewLookup.query("", 0, readableIds.size(), OVERVIEW_SORT, readableIds)
        .stream()
        .collect(Collectors.toMap(ProjectOverview::projectId, Function.identity(), (one, two) -> one));
  }

  /**
   * Looks up the live overview of one project, but only if the current user can access it.
   *
   * @return the overview, or empty when the user has no access to the project
   */
  private Optional<ProjectOverview> findAccessibleOverview(ProjectId projectId) {
    List<ProjectId> readableIds = intersectWithAccessibleProjects(List.of(projectId));
    if (readableIds.isEmpty()) {
      return Optional.empty();
    }
    return projectOverviewLookup.query("", 0, 1, OVERVIEW_SORT, readableIds).stream().findFirst();
  }

  /**
   * Restricts candidate project ids to the projects the current authentication can access.
   *
   * <p>Reuses the very same accessible-project resolution as the project overview queries, so pin
   * visibility and list visibility cannot drift apart.
   */
  private List<ProjectId> intersectWithAccessibleProjects(List<ProjectId> candidates) {
    var accessibleProjectIds = new HashSet<>(
        projectInformationService.findAccessibleProjectIds());
    return candidates.stream().filter(accessibleProjectIds::contains).toList();
  }

  private Optional<String> currentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      return Optional.empty();
    }
    return userIdTranslator.translateToUserId(authentication);
  }

  /**
   * Outcome of a pinning or unpinning request. Expected failure paths are return values, not
   * exceptions (see {@code ExceptionHandling.md}).
   *
   * @since 1.12.0
   */
  public enum PinOutcome {
    /** The pin was created. */
    PINNED,
    /** The pin was removed. */
    UNPINNED,
    /** The user already pinned this project; nothing changed. */
    ALREADY_PINNED,
    /** The user holds no pin for this project; nothing changed. */
    NOT_PINNED,
    /** The user already holds {@link #MAX_PINNED_PROJECTS} pins. */
    LIMIT_REACHED,
    /** The user cannot read the project, so it cannot be pinned. */
    NOT_PERMITTED,
    /** No authenticated user is available. */
    NO_ACTIVE_USER
  }
}
