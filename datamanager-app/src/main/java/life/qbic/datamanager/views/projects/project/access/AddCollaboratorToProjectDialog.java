package life.qbic.datamanager.views.projects.project.access;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.shared.Registration;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import life.qbic.application.commons.SortOrder;
import life.qbic.datamanager.views.account.UserAvatar;
import life.qbic.datamanager.views.general.DialogWindow;
import life.qbic.datamanager.views.projects.project.access.ProjectAccessComponent.UserInfoComponent;
import life.qbic.identity.api.UserInfo;
import life.qbic.identity.api.UserInformationService;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.ProjectCollaborator;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.ProjectRole;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.ProjectRoleRecommendationRenderer;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.SharedProjectGroup;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.usergroups.api.GroupInfo;
import life.qbic.usergroups.api.GroupInformationService;

/**
 * Add Collaborator to Project Dialog
 * <p>
 * Based on the {@link DialogWindow}, this dialog enables a user with at least edit rights to
 * specify which collaborator should be granted access to the selected {@link Project} via the
 * collaborators' username. Additionally, the user is able to specify the {@link ProjectRole} of the
 * added collaborator should fulfill in the project
 * <p>
 */
public class AddCollaboratorToProjectDialog extends DialogWindow {

  @Serial
  private static final long serialVersionUID = 6582904858073255011L;
  private final Div projectRoleSelectionSection = new Div();
  private final Div personSelectionSection = new Div();
  private final Div groupSelectionSection = new Div();
  private final RadioButtonGroup<ProjectRole> projectRoleSelection = new RadioButtonGroup<>();
  private final ComboBox<UserInfo> personSelection = new ComboBox<>();
  private final ComboBox<GroupInfo> groupSelection = new ComboBox<>();
  private final ProjectId projectId;
  private final GroupInformationService groupInformationService;

  public AddCollaboratorToProjectDialog(UserInformationService userInformationService,
      ProjectId projectId,
      List<ProjectCollaborator> projectCollaborators,
      GroupInformationService groupInformationService,
      List<SharedProjectGroup> alreadySharedGroups) {
    requireNonNull(userInformationService, "userInformationService must not be null");
    requireNonNull(groupInformationService, "groupInformationService must not be null");
    this.projectId = requireNonNull(projectId, "projectId must not be null");
    this.groupInformationService = groupInformationService;
    addClassName("add-user-to-project-dialog");
    initPersonSelection(userInformationService, projectCollaborators);
    initGroupSelection(alreadySharedGroups);
    initProjectRoleSelection();
    setHeaderTitle("Add people or groups");
    add(personSelectionSection, groupSelectionSection, projectRoleSelectionSection);
  }

  private static Component renderUserInfo(UserInfo userInfo) {
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

  private void initPersonSelection(UserInformationService userInformationService,
      List<ProjectCollaborator> projectCollaborators) {
    Span title = new Span("Select the person");
    title.addClassNames("section-title");
    Span description = new Span(
        "Please select the person you want to grant access to");
    description.addClassName("secondary");
    personSelection.setItems(query -> {
      List<SortOrder> sortOrders = query.getSortOrders().stream().map(
              it -> new SortOrder(it.getSorted(), it.getDirection().equals(SortDirection.DESCENDING)))
          .collect(Collectors.toCollection(ArrayList::new));
      // if no order is provided by the grid order by username
      sortOrders.add(SortOrder.of("userName").descending());
      List<UserInfo> activeUsersWithFilter = userInformationService.queryActiveUsersWithFilter(
          query.getFilter().orElse(null), query.getOffset(),
          query.getLimit(), List.copyOf(sortOrders));
      // filter for not already
      return activeUsersWithFilter.stream()
          .filter(userInfo -> projectCollaborators.stream()
              .noneMatch(
                  projectCollaborator -> projectCollaborator.userId().equals(userInfo.id())));
    });
    personSelection.setItemLabelGenerator(UserInfo::platformUserName);
    personSelection.setRenderer(
        new ComponentRenderer<>(AddCollaboratorToProjectDialog::renderUserInfo));
    personSelection.setRequired(true);
    personSelection.setErrorMessage("Please specify the collaborator to be added to the project");
    personSelection.setPlaceholder("Please search for username or full name or ORCID identifier");
    personSelection.setRenderer(new ComponentRenderer<>(
        AddCollaboratorToProjectDialog::renderUserInfo
    ));
    personSelection.addClassName("person-selection");
    personSelectionSection.addClassName("person-selection-section");
    personSelectionSection.add(title, description, personSelection);

  }

  private void initGroupSelection(List<SharedProjectGroup> alreadySharedGroups) {
    Span title = new Span("Select the group");
    title.addClassNames("section-title");
    Span comment = new Span("Choose one: a person or a group");
    comment.addClassName("secondary");
    Span description = new Span(
        "Please select a user group you want to grant access to. Every member of the group gains the selected project role.");
    description.addClassName("secondary");
    groupSelection.setItems(query ->
        groupInformationService.listPublicDirectory().stream()
            .filter(groupInfo -> alreadySharedGroups.stream()
                .noneMatch(sharedGroup -> sharedGroup.groupId().equals(groupInfo.id())))
            .filter(groupInfo -> query.getFilter().map(filter -> groupInfo.name().toLowerCase()
                    .contains(filter.toLowerCase()) || (groupInfo.description() != null
                    && groupInfo.description().toLowerCase().contains(filter.toLowerCase())))
                .orElse(true))
            .skip(query.getOffset())
            .limit(query.getLimit()));
    groupSelection.setItemLabelGenerator(GroupInfo::name);
    groupSelection.setRequired(true);
    groupSelection.setErrorMessage(
        "Please specify the group to be added to the project");
    groupSelection.setPlaceholder("Search groups…");
    groupSelection.addClassName("group-selection");
    groupSelectionSection.addClassName("group-selection-section");
    groupSelectionSection.add(title, comment, description, groupSelection);
  }

  private void initProjectRoleSelection() {
    Span title = new Span("Assign a Role");
    title.addClassNames("section-title");
    Span description = new Span("Please select the role for the person or group within the project");
    description.addClassName("secondary");
    projectRoleSelection.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL,
        RadioGroupVariant.LUMO_HELPER_ABOVE_FIELD);
    projectRoleSelection.setItems(List.of(
        ProjectRole.READ,
        ProjectRole.WRITE,
        ProjectRole.ADMIN
    ));
    projectRoleSelection.setRequired(true);
    projectRoleSelection.setErrorMessage(
        "Please specify the role for the to be added collaborator");
    projectRoleSelection.setValue(ProjectRole.READ);
    projectRoleSelection.setRenderer(new ComponentRenderer<>(
        projectRole -> {
          Span roleLabel = new Span(projectRole.label());
          roleLabel.addClassName("project-role-label");

          Span roleDescription = new Span(ProjectRoleRecommendationRenderer.render(projectRole));
          roleDescription.addClassName("project-role-description");

          Div projectRoleDiv = new Div();
          projectRoleDiv.addClassName("project-role-item");
          projectRoleDiv.add(roleLabel, roleDescription);
          return projectRoleDiv;
        })
    );
    projectRoleSelection.addClassName("role-selection");
    projectRoleSelectionSection.addClassName("role-selection-section");
    projectRoleSelectionSection.add(title, description, projectRoleSelection);
  }

  @Override
  protected void onConfirmClicked(ClickEvent<Button> clickEvent) {
    personSelection.setInvalid(personSelection.isEmpty());
    groupSelection.setInvalid(groupSelection.isEmpty());
    projectRoleSelection.setInvalid(projectRoleSelection.isEmpty());
    boolean personSelected = !personSelection.isEmpty();
    boolean groupSelected = !groupSelection.isEmpty();
    if (!(personSelected ^ groupSelected)) {
      // exactly one of person or group must be selected
      personSelection.setInvalid(!personSelected);
      groupSelection.setInvalid(!groupSelected);
      if (!personSelected && !groupSelected) {
        groupSelection.setErrorMessage(
            "Choose one: grant a person or a group access (not both, not neither).");
        projectRoleSelection.setInvalid(projectRoleSelection.isEmpty());
      } else {
        groupSelection.setErrorMessage("Please specify the group to be added to the project");
      }
      return;
    }
    if (personSelected && !projectRoleSelection.isInvalid()) {
      String userId = personSelection.getValue().id();
      ProjectRole projectRole = projectRoleSelection.getValue();
      ProjectCollaborator projectCollaborator = new ProjectCollaborator(userId, projectId,
          projectRole);
      fireEvent(new ConfirmEvent(this, clickEvent.isFromClient(), projectCollaborator));
      return;
    }
    if (groupSelected && !projectRoleSelection.isInvalid()) {
      GroupInfo groupInfo = groupSelection.getValue();
      ProjectRole projectRole = projectRoleSelection.getValue();
      fireEvent(
          new GroupConfirmEvent(this, clickEvent.isFromClient(), groupInfo.id(), projectRole));
    }
  }

  @Override
  protected void onCancelClicked(ClickEvent<Button> clickEvent) {
    fireEvent(new CancelEvent(this, clickEvent.isFromClient()));
  }

  public Registration addCancelListener(ComponentEventListener<CancelEvent> listener) {
    return addListener(CancelEvent.class, listener);
  }

  public Registration addConfirmListener(ComponentEventListener<ConfirmEvent> listener) {
    return addListener(ConfirmEvent.class, listener);
  }

  public Registration addGroupConfirmListener(
      ComponentEventListener<GroupConfirmEvent> listener) {
    return addListener(GroupConfirmEvent.class, listener);
  }

  public static class CancelEvent extends ComponentEvent<AddCollaboratorToProjectDialog> {

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public CancelEvent(AddCollaboratorToProjectDialog source, boolean fromClient) {
      super(source, fromClient);
    }
  }

  public static class ConfirmEvent extends ComponentEvent<AddCollaboratorToProjectDialog> {

    private final transient ProjectCollaborator projectCollaborator;

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public ConfirmEvent(AddCollaboratorToProjectDialog source, boolean fromClient,
        ProjectCollaborator projectCollaborator) {
      super(source, fromClient);
      this.projectCollaborator = projectCollaborator;
    }

    public ProjectCollaborator projectCollaborator() {
      return projectCollaborator;
    }
  }

  public static class GroupConfirmEvent extends ComponentEvent<AddCollaboratorToProjectDialog> {

    private final transient String groupId;
    private final transient ProjectRole projectRole;

    /**
     * Fired when the user confirms sharing a user group onto the project.
     *
     * @param source      the dialog that fired the event
     * @param fromClient  {@code true} if the event originated from the client
     * @param groupId     the stable user group id to share
     * @param projectRole the project role to grant the group
     */
    public GroupConfirmEvent(AddCollaboratorToProjectDialog source, boolean fromClient,
        String groupId, ProjectRole projectRole) {
      super(source, fromClient);
      this.groupId = groupId;
      this.projectRole = projectRole;
    }

    public String groupId() {
      return groupId;
    }

    public ProjectRole projectRole() {
      return projectRole;
    }
  }
}
