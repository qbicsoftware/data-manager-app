package life.qbic.datamanager.views.groups;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
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

  private static final DateTimeFormatter TIMESTAMP =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

  private final transient ProfilePictureService profilePictureService;
  private final transient GroupAdministrationPermission groupAdministrationPermission;
  private final transient AuthenticationToUserIdTranslationService userIdTranslator;
  private final transient UserInformationService userInformationService;
  private final transient GroupInformationService groupInformationService;

  public AdminProfilePictureAuditMain(
      @Autowired ProfilePictureService profilePictureService,
      @Autowired GroupAdministrationPermission groupAdministrationPermission,
      @Autowired AuthenticationToUserIdTranslationService userIdTranslator,
      @Autowired UserInformationService userInformationService,
      @Autowired GroupInformationService groupInformationService) {
    this.profilePictureService = requireNonNull(profilePictureService,
        "profilePictureService must not be null");
    this.groupAdministrationPermission = requireNonNull(groupAdministrationPermission,
        "groupAdministrationPermission must not be null");
    this.userIdTranslator = requireNonNull(userIdTranslator, "userIdTranslator must not be null");
    this.userInformationService = requireNonNull(userInformationService,
        "userInformationService must not be null");
    this.groupInformationService = requireNonNull(groupInformationService,
        "groupInformationService must not be null");
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

  private void build(String adminUserId) {
    var section = new SettingsSection("Profile picture audit",
        "Set and replace events for user and group profile pictures. Administrators may "
            + "force-remove a picture.");

    Grid<ProfilePictureAuditEntry> grid = new Grid<>();
    grid.addComponentColumn(this::pictureCell)
        .setHeader("Picture").setAutoWidth(true);
    grid.addColumn(this::ownerDisplay).setHeader("Owner").setAutoWidth(true).setSortable(false);
    grid.addColumn(this::ownerTypeDisplay).setHeader("Type").setAutoWidth(true).setSortable(false);
    grid.addColumn(ProfilePictureAuditEntry::action).setHeader("Action").setAutoWidth(true)
        .setSortable(false);
    grid.addColumn(this::actorDisplay).setHeader("Actor").setAutoWidth(true).setSortable(false);
    grid.addColumn(this::timestampDisplay).setHeader("When").setAutoWidth(true).setSortable(false);
    grid.addComponentColumn(entry -> forceRemoveButton(adminUserId, entry))
        .setHeader("").setAutoWidth(true);
    grid.setHeight("34rem");

    PaginatedGrid<ProfilePictureAuditEntry> paginatedGrid = new PaginatedGrid<>(
        grid,
        this::loadAuditPage,
        entry -> entry.ownerType().name() + ":" + entry.ownerId() + ":" + entry.createdAt(),
        "record",
        SortOrder.of("createdAt"));

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

  private Button forceRemoveButton(String adminUserId, ProfilePictureAuditEntry entry) {
    var button = new Button("Force remove");
    button.addClassName("tertiary");
    button.addClickListener(click -> {
      var result = profilePictureService.forceRemove(adminUserId, entry.ownerType(),
          entry.ownerId());
      if (result.isError()) {
        Notification.show("Could not remove the picture.");
        return;
      }
      button.setEnabled(false);
      button.setText("Removed");
      Notification.show("Picture removed.");
    });
    return button;
  }

  // ── paging ───────────────────────────────────────────────────────────────

  private PaginatedGrid.Page<ProfilePictureAuditEntry> loadAuditPage(ListState state) {
    String adminUserId = currentUserId();
    var result = profilePictureService.auditPage(adminUserId, state.filter(),
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