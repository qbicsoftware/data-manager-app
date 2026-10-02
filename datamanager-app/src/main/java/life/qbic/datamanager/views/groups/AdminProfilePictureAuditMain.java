package life.qbic.datamanager.views.groups;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import life.qbic.application.commons.SortOrder;
import life.qbic.datamanager.profilepicture.ProfilePictureAuditEntry;
import life.qbic.datamanager.profilepicture.ProfilePictureOwnerType;
import life.qbic.datamanager.profilepicture.ProfilePictureService;
import life.qbic.datamanager.views.AppRoutes;
import life.qbic.datamanager.views.account.UserAvatar;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.general.pagination.ListState;
import life.qbic.datamanager.views.general.pagination.PaginatedGrid;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.datamanager.views.settings.SettingsSection;
import life.qbic.identity.api.UserInformationService;
import life.qbic.projectmanagement.application.AuthenticationToUserIdTranslationService;
import life.qbic.usergroups.api.GroupAdministrationPermission;
import life.qbic.usergroups.api.GroupInfo;
import life.qbic.usergroups.api.GroupInformationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * System-administrator view of the profile-picture audit trail.
 *
 * <p>Lists set/replace events (newest first) in a reusable {@link PaginatedGrid} and allows an
 * administrator to force-remove any listed owner's picture. Admin-only: non-administrators are
 * routed to the not-found page, and the service gate remains authoritative.</p>
 *
 * <p>Rows are human-readable: owners and actors resolve to display names and each row shows the
 * owner's current picture (historical images are not retained, so this is the picture as of
 * today, not the audited revision).</p>
 *
 * @since 1.22.0
 */
@Route(value = AppRoutes.GroupsRoutes.ADMIN_PROFILE_PICTURES, layout = GroupsMainLayout.class)
@SpringComponent
@UIScope
@PermitAll
@PageTitle("Groups · Profile pictures")
public class AdminProfilePictureAuditMain extends Main implements BeforeEnterObserver {

  @Serial
  private static final long serialVersionUID = 8822664477811220001L;

  /** Larger default page size for the audit list (matches the measurements view). */
  private static final int AUDIT_PAGE_SIZE = 24;

  private static final DateTimeFormatter TIMESTAMP =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

  private final transient ProfilePictureService profilePictureService;
  private final transient GroupAdministrationPermission groupAdministrationPermission;
  private final transient AuthenticationToUserIdTranslationService userIdTranslator;
  private final transient UserInformationService userInformationService;
  private final transient GroupInformationService groupInformationService;
  private final transient MessageSourceNotificationFactory messageFactory;

  public AdminProfilePictureAuditMain(
      @Autowired ProfilePictureService profilePictureService,
      @Autowired GroupAdministrationPermission groupAdministrationPermission,
      @Autowired AuthenticationToUserIdTranslationService userIdTranslator,
      @Autowired UserInformationService userInformationService,
      @Autowired GroupInformationService groupInformationService,
      @Autowired MessageSourceNotificationFactory messageFactory) {
    this.profilePictureService = requireNonNull(profilePictureService,
        "profilePictureService must not be null");
    this.groupAdministrationPermission = requireNonNull(groupAdministrationPermission,
        "groupAdministrationPermission must not be null");
    this.userIdTranslator = requireNonNull(userIdTranslator, "userIdTranslator must not be null");
    this.userInformationService = requireNonNull(userInformationService,
        "userInformationService must not be null");
    this.groupInformationService = requireNonNull(groupInformationService,
        "groupInformationService must not be null");
    this.messageFactory = requireNonNull(messageFactory, "messageFactory must not be null");
    addClassName("admin-profile-pictures");
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    String adminUserId = currentUserId();
    if (adminUserId == null || !groupAdministrationPermission.isAdmin(adminUserId)) {
      event.rerouteToError(NotFoundException.class);
      return;
    }
    removeAll();
    build(adminUserId);
  }

  private boolean showReviewed = false;

  private void build(String adminUserId) {
    var section = new SettingsSection("Profile picture audit",
        "Set and replace events for user and group profile pictures. Administrators may mark "
            + "entries as reviewed or retract a violating picture.");

    Grid<ProfilePictureAuditEntry> grid = new Grid<>();
    grid.addComponentColumn(this::pictureCell)
        .setHeader("Picture").setAutoWidth(true);
    grid.addColumn(this::ownerDisplay).setHeader("Owner").setAutoWidth(true).setSortable(false);
    grid.addColumn(this::ownerTypeDisplay).setHeader("Type").setAutoWidth(true).setSortable(false);
    grid.addColumn(ProfilePictureAuditEntry::action).setHeader("Action").setAutoWidth(true)
        .setSortable(false);
    grid.addColumn(this::actorDisplay).setHeader("Actor").setAutoWidth(true).setSortable(false);
    grid.addColumn(this::timestampDisplay).setHeader("When").setAutoWidth(true).setSortable(false);
    grid.addColumn(entry -> entry.reviewed() ? "Reviewed" : "—").setHeader("Review")
        .setAutoWidth(true).setSortable(false);

    // The PaginatedGrid's built-in selection toolbar provides the row checkboxes, the
    // "N records selected" text and the clear action — the same selection pattern the measurement
    // lists use. showSearch=false keeps that toolbar but drops the free-text search field, and the
    // grid is not height-constrained so the page scrolls naturally.
    PaginatedGrid<ProfilePictureAuditEntry> paginatedGrid = new PaginatedGrid<>(
        grid,
        this::loadAuditPage,
        entry -> String.valueOf(entry.id()),
        "record",
        SortOrder.of("createdAt"),
        true,
        false,
        true,
        false);
    // Default to 24 rows per page (the PaginatedGrid constructor would default to 12).
    paginatedGrid.setListState(
        new ListState(1, AUDIT_PAGE_SIZE, "", SortOrder.of("createdAt")));

    // Bulk actions on the current selection (cross-page), disabled while nothing is selected.
    Button markReviewedButton = new Button("Mark reviewed");
    markReviewedButton.addClassName("primary");
    markReviewedButton.setEnabled(false);
    markReviewedButton.addClickListener(click -> markReviewedSelected(paginatedGrid, adminUserId));

    Button forceRemoveButton = new Button("Force remove");
    forceRemoveButton.addClassName("button-danger");
    forceRemoveButton.setEnabled(false);
    forceRemoveButton.addClickListener(click -> forceRemoveSelected(paginatedGrid, adminUserId));

    paginatedGrid.addSelectionChangeListener(event -> {
      boolean hasSelection = !event.getSelectedIds().isEmpty();
      markReviewedButton.setEnabled(hasSelection);
      forceRemoveButton.setEnabled(hasSelection);
    });

    Checkbox showReviewedCheckbox = new Checkbox("Show reviewed");
    showReviewedCheckbox.setValue(showReviewed);
    showReviewedCheckbox.addValueChangeListener(event -> {
      showReviewed = event.getValue();
      paginatedGrid.refresh();
    });

    section.addAction(markReviewedButton);
    section.addAction(forceRemoveButton);
    section.addContent(showReviewedCheckbox);
    section.addContent(paginatedGrid);
    add(section);
  }

  // ── columns ──────────────────────────────────────────────────────────────

  private UserAvatar pictureCell(ProfilePictureAuditEntry entry) {
    UserAvatar avatar = new UserAvatar();
    avatar.addClassName("audit-table-avatar");
    if (entry.ownerType() == ProfilePictureOwnerType.USER) {
      avatar.setUserId(entry.ownerId());
    } else {
      avatar.setGroupId(entry.ownerId());
    }
    return avatar;
  }

  private String ownerDisplay(ProfilePictureAuditEntry entry) {
    if (entry.ownerType() == ProfilePictureOwnerType.USER) {
      return displayNameForUser(entry.ownerId());
    }
    return displayNameForGroup(entry.ownerId());
  }

  private String ownerTypeDisplay(ProfilePictureAuditEntry entry) {
    return entry.ownerType() == ProfilePictureOwnerType.USER ? "User" : "Group";
  }

  private String actorDisplay(ProfilePictureAuditEntry entry) {
    return displayNameForUser(entry.actorId());
  }

  private String timestampDisplay(ProfilePictureAuditEntry entry) {
    return TIMESTAMP.format(entry.createdAt());
  }

  /**
   * Force-removes the pictures of every distinct owner in the current selection (across pages).
   * The audit rows are kept; only the stored pictures are retracted, so the affected avatars
   * become placeholders.
   */
  private void forceRemoveSelected(PaginatedGrid<ProfilePictureAuditEntry> grid,
      String adminUserId) {
    var ids = grid.selectedIds().stream().map(Long::valueOf).toList();
    var result = profilePictureService.forceRemoveAuditEntries(adminUserId, ids);
    grid.deselect(grid.selectedIds());
    grid.refresh();
    if (result.isError() || result.getValue() == 0) {
      messageFactory.toast("profile.picture.force-remove.error", new Object[]{}, getLocale())
          .open();
    } else {
      messageFactory.toast("profile.picture.force-remove.success", new Object[]{result.getValue()},
          getLocale()).open();
    }
  }

  /**
   * Marks the selected audit entries as reviewed. This only cleans up the working list; stored
   * pictures are untouched.
   */
  private void markReviewedSelected(PaginatedGrid<ProfilePictureAuditEntry> grid,
      String adminUserId) {
    var ids = grid.selectedIds().stream().map(Long::valueOf).toList();
    var result = profilePictureService.markReviewed(adminUserId, ids);
    grid.deselect(grid.selectedIds());
    grid.refresh();
    if (result.isError()) {
      messageFactory.toast("profile.picture.mark-reviewed.error", new Object[]{}, getLocale())
          .open();
    } else {
      messageFactory.toast("profile.picture.mark-reviewed.success", new Object[]{ids.size()},
          getLocale()).open();
    }
  }

  // ── paging ───────────────────────────────────────────────────────────────

  private PaginatedGrid.Page<ProfilePictureAuditEntry> loadAuditPage(ListState state) {
    String adminUserId = currentUserId();
    var result = profilePictureService.auditPage(adminUserId, showReviewed,
        PageRequest.of(state.page() - 1, state.pageSize()));
    if (result.isError()) {
      return new PaginatedGrid.Page<>(java.util.List.of(), 0);
    }
    var page = result.getValue();
    return new PaginatedGrid.Page<>(page.getContent(), page.getTotalElements());
  }

  // ── name resolution ──────────────────────────────────────────────────────

  private String displayNameForUser(String userId) {
    return userInformationService.findById(userId)
        .map(user -> user.fullName() + " (" + user.platformUserName() + ")")
        .orElse(userId);
  }

  private String displayNameForGroup(String groupId) {
    return groupInformationService.findGroupById(groupId)
        .map(GroupInfo::name)
        .orElse(groupId);
  }

  private String currentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return userIdTranslator.translateToUserId(authentication).orElse(null);
  }
}