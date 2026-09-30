package life.qbic.datamanager.views.projects.project.access;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.shared.Registration;
import java.io.Serial;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import life.qbic.application.commons.SortOrder;
import life.qbic.datamanager.views.account.UserAvatar;
import life.qbic.datamanager.views.projects.project.access.ProjectAccessComponent.UserInfoComponent;
import life.qbic.identity.api.UserInfo;
import life.qbic.identity.api.UserInformationService;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.ProjectCollaborator;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.ProjectRole;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.ProjectRoleRecommendationRenderer;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.SharedProjectGroup;
import life.qbic.usergroups.api.GroupInfo;
import life.qbic.usergroups.api.GroupInformationService;

/**
 * <b>Project Sharing Composer</b>
 *
 * <p>An inline, dialog-free surface to grant project access to one or several principals — people
 * and user groups — at once. Selecting a principal stages a removable chip with its own
 * {@link ProjectRole} control; {@code Grant access} applies all staged grants in a single request.
 * Staging a single principal is the per-item workflow; staging several is the batch workflow.</p>
 *
 * <p>The composer never uses a modal dialog and never requests group membership data. Already
 * granted principals are filtered out of the pickers.</p>
 *
 * @since 1.20.0
 */
public class ProjectSharingComposer extends Div {

  @Serial
  private static final long serialVersionUID = 7139122224671283411L;
  private static final List<ProjectRole> ASSIGNABLE_ROLES = List.of(ProjectRole.READ,
      ProjectRole.WRITE, ProjectRole.ADMIN);

  private final transient UserInformationService userInformationService;
  private final transient GroupInformationService groupInformationService;
  private final ComboBox<UserInfo> personPicker = new ComboBox<>();
  private final ComboBox<GroupInfo> groupPicker = new ComboBox<>();
  private final Div stagedGrants = new Div();
  private final Button grantButton = new Button("Grant access");

  private final Set<String> alreadyGrantedUserIds = new HashSet<>();
  private final Set<String> alreadyGrantedGroupIds = new HashSet<>();
  private final Set<String> stagedUserIds = new HashSet<>();
  private final Set<String> stagedGroupIds = new HashSet<>();

  public ProjectSharingComposer(UserInformationService userInformationService,
      GroupInformationService groupInformationService) {
    this.userInformationService = requireNonNull(userInformationService);
    this.groupInformationService = requireNonNull(groupInformationService);
    addClassName("project-sharing-composer");
    layout();
  }

  private static Component renderUser(UserInfo userInfo) {
    UserAvatar userAvatar = new UserAvatar();
    userAvatar.setUserId(userInfo.id());
    userAvatar.setName(userInfo.platformUserName());
    UserInfoComponent userInfoComponent = new UserInfoComponent(userAvatar,
        userInfo.platformUserName(), userInfo.fullName());
    if (userInfo.oidcId() != null && userInfo.oidcIssuer() != null) {
      userInfoComponent.setOidc(userInfo.oidcIssuer(), userInfo.oidcId());
    }
    return userInfoComponent;
  }

  private void layout() {
    Span title = new Span("Share this project");
    title.addClassName("section-title");
    Span description = new Span(
        "Add one or several people and groups. Everyone selected gains the role you choose on the "
            + "chip. Already granted people and groups are not offered again.");
    description.addClassName("secondary");

    configurePersonPicker();
    configureGroupPicker();
    stagedGrants.addClassName("staged-grants");

    grantButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    grantButton.addClassName("grant-access-button");
    grantButton.setEnabled(false);
    grantButton.addClickListener(event -> fireGrantRequest());

    Div pickers = new Div(personPicker, groupPicker);
    pickers.addClassName("sharing-pickers");

    add(title, description, pickers, stagedGrants, grantButton);
  }

  private void configurePersonPicker() {
    personPicker.setLabel("Add a person");
    personPicker.setPlaceholder("Search by username, name or ORCID…");
    personPicker.setItemLabelGenerator(UserInfo::platformUserName);
    personPicker.setRenderer(new ComponentRenderer<>(ProjectSharingComposer::renderUser));
    personPicker.addClassName("person-selection");
    personPicker.setItems(query -> {
      List<SortOrder> sortOrders = query.getSortOrders().stream()
          .map(it -> new SortOrder(it.getSorted(),
              it.getDirection().equals(SortDirection.DESCENDING)))
          .collect(Collectors.toCollection(ArrayList::new));
      sortOrders.add(SortOrder.of("userName").descending());
      return userInformationService.queryActiveUsersWithFilter(query.getFilter().orElse(null),
              query.getOffset(), query.getLimit(), List.copyOf(sortOrders)).stream()
          .filter(userInfo -> !alreadyGrantedUserIds.contains(userInfo.id()))
          .filter(userInfo -> !stagedUserIds.contains(userInfo.id()));
    });
    personPicker.addValueChangeListener(event -> {
      if (event.getValue() != null) {
        stageUser(event.getValue());
        personPicker.setValue(null);
      }
    });
  }

  private void configureGroupPicker() {
    groupPicker.setLabel("Add a group");
    groupPicker.setPlaceholder("Search groups…");
    groupPicker.setItemLabelGenerator(GroupInfo::name);
    groupPicker.addClassName("group-selection");
    groupPicker.setItems(query -> {
      String filter = query.getFilter().orElse("").toLowerCase();
      return groupInformationService.listPublicDirectory().stream()
          .filter(groupInfo -> !alreadyGrantedGroupIds.contains(groupInfo.id()))
          .filter(groupInfo -> !stagedGroupIds.contains(groupInfo.id()))
          .filter(groupInfo -> filter.isEmpty()
              || groupInfo.name().toLowerCase().contains(filter)
              || (groupInfo.description() != null
              && groupInfo.description().toLowerCase().contains(filter)))
          .skip(query.getOffset())
          .limit(query.getLimit());
    });
    groupPicker.addValueChangeListener(event -> {
      if (event.getValue() != null) {
        stageGroup(event.getValue());
        groupPicker.setValue(null);
      }
    });
  }

  private void stageUser(UserInfo userInfo) {
    if (!stagedUserIds.add(userInfo.id())) {
      return;
    }
    addStagedGrant(PrincipalType.USER, userInfo.id(), renderUser(userInfo));
    personPicker.getDataProvider().refreshAll();
    updateGrantButtonState();
  }

  private void stageGroup(GroupInfo groupInfo) {
    if (!stagedGroupIds.add(groupInfo.id())) {
      return;
    }
    addStagedGrant(PrincipalType.GROUP, groupInfo.id(), renderGroup(groupInfo));
    groupPicker.getDataProvider().refreshAll();
    updateGrantButtonState();
  }

  private static Component renderGroup(GroupInfo groupInfo) {
    Span name = new Span(groupInfo.name());
    name.addClassName("bold");
    if (groupInfo.description() == null || groupInfo.description().isBlank()) {
      return name;
    }
    Span description = new Span(groupInfo.description());
    description.addClassName("tertiary");
    Div identity = new Div(name, description);
    identity.addClassName("group-identity");
    return identity;
  }

  private void addStagedGrant(PrincipalType type, String id, Component identity) {
    stagedGrants.add(new StagedGrant(type, id, identity));
  }

  private void removeStagedGrant(StagedGrant stagedGrant) {
    if (stagedGrant.type() == PrincipalType.USER) {
      stagedUserIds.remove(stagedGrant.id());
      personPicker.getDataProvider().refreshAll();
    } else {
      stagedGroupIds.remove(stagedGrant.id());
      groupPicker.getDataProvider().refreshAll();
    }
    stagedGrants.remove(stagedGrant);
    updateGrantButtonState();
  }

  private void updateGrantButtonState() {
    grantButton.setEnabled(!stagedGrants.getChildren().findAny().isEmpty());
  }

  private void fireGrantRequest() {
    List<GrantRequest> requests = stagedGrants.getChildren()
        .filter(StagedGrant.class::isInstance)
        .map(StagedGrant.class::cast)
        .map(staged -> new GrantRequest(staged.type(), staged.id(), staged.role()))
        .toList();
    if (requests.isEmpty()) {
      return;
    }
    fireEvent(new GrantRequestedEvent(this, true, requests));
  }

  /**
   * Provides the currently granted principals so the pickers can filter them out.
   *
   * @param collaborators the direct collaborators of the project
   * @param sharedGroups  the groups currently shared onto the project
   */
  public void setAlreadyGranted(List<ProjectCollaborator> collaborators,
      List<SharedProjectGroup> sharedGroups) {
    alreadyGrantedUserIds.clear();
    collaborators.stream().map(ProjectCollaborator::userId).forEach(alreadyGrantedUserIds::add);
    alreadyGrantedGroupIds.clear();
    sharedGroups.stream().map(SharedProjectGroup::groupId)
        .forEach(alreadyGrantedGroupIds::add);
    clearStagedGrants();
    personPicker.getDataProvider().refreshAll();
    groupPicker.getDataProvider().refreshAll();
  }

  /**
   * Clears the staging area. Called after a successful grant or when the composer is dismissed.
   */
  public void reset() {
    clearStagedGrants();
  }

  private void clearStagedGrants() {
    stagedGrants.removeAll();
    stagedUserIds.clear();
    stagedGroupIds.clear();
    updateGrantButtonState();
    personPicker.getDataProvider().refreshAll();
    groupPicker.getDataProvider().refreshAll();
  }

  public Registration addGrantListener(
      ComponentEventListener<GrantRequestedEvent> listener) {
    return addListener(GrantRequestedEvent.class, listener);
  }

  /**
   * The kind of principal a grant refers to.
   */
  public enum PrincipalType {
    USER,
    GROUP
  }

  /**
   * A single grant staged in the composer: a principal and the project role it should receive.
   */
  public record GrantRequest(PrincipalType type, String id, ProjectRole role) {

  }

  /**
   * A staged principal chip with its own role selection and a remove control.
   */
  private final class StagedGrant extends Div {

    @Serial
    private static final long serialVersionUID = 8907312431231123345L;
    private final PrincipalType type;
    private final String id;
    private final Select<ProjectRole> roleSelect = new Select<>();

    private StagedGrant(PrincipalType type, String id, Component identity) {
      this.type = type;
      this.id = id;
      addClassName("staged-grant");
      configureRoleSelect();
      Button remove = new Button(VaadinIcon.CLOSE_SMALL.create());
      remove.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
      remove.addClassName("remove-staged-grant");
      remove.getElement().setAttribute("aria-label", "Remove from the share list");
      remove.addClickListener(event -> removeStagedGrant(this));
      add(identity, roleSelect, remove);
    }

    private void configureRoleSelect() {
      roleSelect.addClassName("project-role-select");
      roleSelect.setItemLabelGenerator(ProjectRole::label);
      roleSelect.setItems(ASSIGNABLE_ROLES);
      roleSelect.setRenderer(new ComponentRenderer<>(role -> {
        Span roleLabel = new Span(role.label());
        roleLabel.addClassName("project-role-label");
        Span roleDescription = new Span(ProjectRoleRecommendationRenderer.render(role));
        roleDescription.addClassName("project-role-description");
        Div item = new Div(roleLabel, roleDescription);
        item.addClassName("project-role-item");
        return item;
      }));
      roleSelect.setValue(ProjectRole.READ);
      roleSelect.getElement().setAttribute("aria-label", "Project role for the selected principal");
    }

    private PrincipalType type() {
      return type;
    }

    private String id() {
      return id;
    }

    private ProjectRole role() {
      return roleSelect.getValue();
    }
  }

  /**
   * Fired when the user asks to grant access to all staged principals.
   */
  public static class GrantRequestedEvent extends ComponentEvent<ProjectSharingComposer> {

    @Serial
    private static final long serialVersionUID = 5571239981233112391L;
    private final transient List<GrantRequest> requests;

    /**
     * Creates a new grant request event.
     *
     * @param source     the composer that fired the event
     * @param fromClient whether the event originated client-side
     * @param requests   the staged grants to apply
     */
    public GrantRequestedEvent(ProjectSharingComposer source, boolean fromClient,
        List<GrantRequest> requests) {
      super(source, fromClient);
      this.requests = List.copyOf(requests);
    }

    public List<GrantRequest> requests() {
      return requests;
    }
  }
}
