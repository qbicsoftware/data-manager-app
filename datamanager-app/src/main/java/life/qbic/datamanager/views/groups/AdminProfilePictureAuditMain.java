package life.qbic.datamanager.views.groups;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
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
import life.qbic.datamanager.profilepicture.ProfilePictureAuditEntry;
import life.qbic.datamanager.profilepicture.ProfilePictureService;
import life.qbic.datamanager.views.AppRoutes;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.settings.SettingsSection;
import life.qbic.projectmanagement.application.AuthenticationToUserIdTranslationService;
import life.qbic.usergroups.api.GroupAdministrationPermission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * System-administrator view of the profile-picture audit trail.
 *
 * <p>Lists set/replace events (newest first) and allows an administrator to force-remove any
 * listed owner's picture. Admin-only: non-administrators are routed to the not-found page, and the
 * service gate remains authoritative.</p>
 *
 * @since 1.22.0
 */
@Route(value = AppRoutes.GroupsRoutes.ADMIN_PROFILE_PICTURES, layout = GroupsMainLayout.class)
@SpringComponent
@UIScope
@PermitAll
@PageTitle("Groups · Admin · Profile pictures")
public class AdminProfilePictureAuditMain extends Main implements BeforeEnterObserver {

  @Serial
  private static final long serialVersionUID = 8822664477811220001L;

  private static final int MAX_ENTRIES = 200;
  private static final DateTimeFormatter TIMESTAMP =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

  private final transient ProfilePictureService profilePictureService;
  private final transient GroupAdministrationPermission groupAdministrationPermission;
  private final transient AuthenticationToUserIdTranslationService userIdTranslator;
  private final Grid<ProfilePictureAuditEntry> grid = new Grid<>();

  public AdminProfilePictureAuditMain(
      @Autowired ProfilePictureService profilePictureService,
      @Autowired GroupAdministrationPermission groupAdministrationPermission,
      @Autowired AuthenticationToUserIdTranslationService userIdTranslator) {
    this.profilePictureService = requireNonNull(profilePictureService,
        "profilePictureService must not be null");
    this.groupAdministrationPermission = requireNonNull(groupAdministrationPermission,
        "groupAdministrationPermission must not be null");
    this.userIdTranslator = requireNonNull(userIdTranslator, "userIdTranslator must not be null");
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
    var section = new SettingsSection("Admin · Profile picture audit",
        "Set and replace events for user and group profile pictures. Administrators may "
            + "force-remove a picture.");
    grid.addColumn(entry -> entry.ownerType().name()).setHeader("Owner type").setAutoWidth(true);
    grid.addColumn(ProfilePictureAuditEntry::ownerId).setHeader("Owner id").setAutoWidth(true);
    grid.addColumn(ProfilePictureAuditEntry::action).setHeader("Action").setAutoWidth(true);
    grid.addColumn(ProfilePictureAuditEntry::actorId).setHeader("Actor").setAutoWidth(true);
    grid.addColumn(entry -> TIMESTAMP.format(entry.createdAt())).setHeader("When")
        .setAutoWidth(true);
    grid.addColumn(entry -> shortHash(entry.previousContentHash())).setHeader("Previous")
        .setAutoWidth(true);
    grid.addColumn(entry -> shortHash(entry.newContentHash())).setHeader("New").setAutoWidth(true);
    grid.addComponentColumn(entry -> forceRemoveButton(adminUserId, entry))
        .setHeader("").setAutoWidth(true);
    grid.setItems(reload(adminUserId));
    grid.setAllRowsVisible(false);
    grid.setHeight("32rem");
    section.addContent(grid);
    add(section);
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
      // Refresh in place: the audit row stays (removal is not audited), but the button is spent.
      button.setEnabled(false);
      button.setText("Removed");
      Notification.show("Picture removed.");
    });
    return button;
  }

  private java.util.List<ProfilePictureAuditEntry> reload(String adminUserId) {
    var result = profilePictureService.listAudit(adminUserId,
        PageRequest.of(0, MAX_ENTRIES));
    if (result.isError()) {
      add(new Span("Could not load the audit trail."));
      return java.util.List.of();
    }
    return result.getValue();
  }

  private static String shortHash(String hash) {
    return hash == null ? "—" : hash.substring(0, Math.min(8, hash.length()));
  }

  private String currentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return userIdTranslator.translateToUserId(authentication).orElse(null);
  }
}
