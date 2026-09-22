package life.qbic.datamanager.views.settings;

import static java.util.Objects.nonNull;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.util.Objects;
import life.qbic.datamanager.views.AppRoutes;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.projectmanagement.application.AuthenticationToUserIdTranslationService;
import life.qbic.usergroups.api.GroupInformationService;
import life.qbic.usergroups.application.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * My Groups Main
 * <p>
 * This component hosts the "My Groups" settings section of the current logged-in user. It lists
 * the groups the user belongs to together with their internal role and provides access to group
 * management actions.
 *
 * @since 1.19.0
 */
@Route(value = AppRoutes.GroupsRoutes.MY_GROUPS, layout = SettingsMainLayout.class)
@SpringComponent
@UIScope
@PermitAll
@PageTitle("Settings · My Groups")
public class MyGroupsMain extends Main implements BeforeEnterObserver {

  @Serial
  private static final long serialVersionUID = 6902406260794581649L;

  private final transient GroupInformationService groupInformationService;
  private final transient GroupService groupService;
  private final transient AuthenticationToUserIdTranslationService userIdTranslator;
  private final transient MessageSourceNotificationFactory messageFactory;

  private SettingsSection section;
  private MyGroupsComponent myGroupsComponent;

  public MyGroupsMain(
      @Autowired GroupInformationService groupInformationService,
      @Autowired GroupService groupService,
      @Autowired AuthenticationToUserIdTranslationService userIdTranslator,
      @Autowired MessageSourceNotificationFactory messageFactory) {
    this.groupInformationService = Objects.requireNonNull(groupInformationService,
        "groupInformationService must not be null");
    this.groupService = Objects.requireNonNull(groupService, "groupService must not be null");
    this.userIdTranslator = Objects.requireNonNull(userIdTranslator,
        "userIdTranslator must not be null");
    this.messageFactory = Objects.requireNonNull(messageFactory,
        "messageFactory must not be null");
    addClassName("my-groups");
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    var userId = userIdTranslator.translateToUserId(authentication).orElseThrow();

    if (nonNull(section)) {
      remove(section);
    }
    section = new SettingsSection("My Groups",
        "Groups you belong to and manage.");
    myGroupsComponent = new MyGroupsComponent(
        () -> groupInformationService.listMyGroups(userId),
        this::refreshList,
        this::onLeaveGroupRequested);
    myGroupsComponent.refresh();
    section.addContent(myGroupsComponent);
    Button newGroupButton = new Button("New group");
    newGroupButton.setClassName("my-groups__new-group");
    newGroupButton.addClickListener(click ->
        com.vaadin.flow.component.UI.getCurrent().navigate(NewGroupMain.class));
    section.addAction(newGroupButton);
    add(section);
  }

  private void refreshList() {
    myGroupsComponent.refresh();
  }

  /**
   * Placeholder for the leave-group flow.
   * <p>
   * The list component emits {@link MyGroupsComponent.LeaveGroupEvent}s; the actual confirmation
   * dialog and {@link GroupService#removeMembership} call are connected in a later increment of
   * this story (self-remove stage).
   */
  private void onLeaveGroupRequested(String groupId) {
    // left empty on purpose: connected in the self-remove increment
  }
}