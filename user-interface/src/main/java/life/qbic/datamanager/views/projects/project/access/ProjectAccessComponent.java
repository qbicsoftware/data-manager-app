package life.qbic.datamanager.views.projects.project.access;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.identity.api.UserInfo;
import life.qbic.identity.api.UserInformationService;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.authorization.QbicUserDetails;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.service.AccessDomainService;
import life.qbic.projectmanagement.infrastructure.project.access.SidRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * Project Access Component
 * <p>
 * The access component is a {@link PageArea} component,
 * which shows the current permissions for all users and user groups within a {@link Project}.
 * <p>
 * Additionally, it provides the possibility to add or revoke project access for individual users
 * and user groups for the selected {@link Project}.
 * <p>
 */
@SpringComponent
@UIScope
public class ProjectAccessComponent extends PageArea {

  @Serial
  private static final long serialVersionUID = 6832688939965353201L;
  private final ProjectAccessService projectAccessService;
  private final UserDetailsService userDetailsService;
  private final SidRepository sidRepository;
  private final UserInformationService userInformationService;
  private static final Logger log = logger(ProjectAccessMain.class);
  private final Grid<UserProjectAccess> userProjectAccessGrid;
  private final AccessDomainService accessDomainService;
  private Context context;

  protected ProjectAccessComponent(
      @Autowired ProjectAccessService projectAccessService,
      @Autowired UserDetailsService userDetailsService,
      @Autowired UserInformationService userInformationService,
      @Autowired SidRepository sidRepository,
      @Autowired AccessDomainService accessDomainService) {
    this.projectAccessService = projectAccessService;
    this.userDetailsService = userDetailsService;
    this.userInformationService = userInformationService;
    this.sidRepository = sidRepository;
    this.accessDomainService = accessDomainService;
    requireNonNull(projectAccessService, "projectAccessService must not be null");
    requireNonNull(userDetailsService, "userDetailsService must not be null");
    requireNonNull(userInformationService, "userRepository must not be null");
    requireNonNull(sidRepository, "sidRepository must not be null");
    requireNonNull(accessDomainService, "accessDomainService must not be null");

    userProjectAccessGrid = new Grid<>(UserProjectAccess.class);

    var header = initHeader();
    var content = initContent();
    add(header, content);
    this.addClassName("project-access-component");
    log.debug(String.format(
        "New instance for ProjectAccessMain(#%s)",
        System.identityHashCode(this)));

  }

  public void setContext(Context context) {
    ProjectId projectId = context.projectId()
        .orElseThrow(() -> new ApplicationException("no project id in context " + context));
    this.context = context;
    loadInformationForProject(projectId);
  }

  private Div initHeader() {
    Span titleField = new Span();
    Div headerDiv = new Div();
    headerDiv.addClassName("header");
    titleField.setText("Project Access Management");
    titleField.addClassName("title");
    var buttonBar = new Span();
    Button editButton = new Button("Edit");
    editButton.addClickListener(event -> openEditUserAccessToProjectDialog());
    buttonBar.add(editButton);
    headerDiv.add(titleField, buttonBar);
    return headerDiv;
  }

  private Div initContent() {
    Div contentDiv = new Div();
    contentDiv.addClassName("content");
    var projectAccess = layoutUserProjectAccessGrid();
    contentDiv.add(projectAccess);
    return contentDiv;
  }

  private Div layoutUserProjectAccessGrid() {
    Span userProjectAccessDescription = new Span("Users with access to this project");
    userProjectAccessGrid.addColumn(UserProjectAccess::fullName).setHeader("User Name");
    userProjectAccessGrid.addColumn(UserProjectAccess::emailAddress).setHeader("Email Address");
    userProjectAccessGrid.addColumn(UserProjectAccess::projectRole).setHeader("User Role");
    Div userProjectAccess = new Div(userProjectAccessDescription, userProjectAccessGrid);
    userProjectAccess.addClassName("user-access");
    return userProjectAccess;
  }

  private List<String> getProjectRoles(List<String> projectRoles, QbicUserDetails userDetails) {
    List<String> roles = new ArrayList<>();
    for (String projectRole : projectRoles) {
      if (userDetails.hasAuthority(projectRole)) {
        roles.add(projectRole);
      }
    }
    return roles.stream().sorted().toList();
  }

  private void loadInformationForProject(ProjectId projectId) {
    loadProjectAccessibleUsers(projectId);
  }

  //shows active users in the UI
  private void loadProjectAccessibleUsers(ProjectId projectId) {
    List<String> userIds = projectAccessService.listActiveUserIds(projectId);
    List<QbicUserDetails> users = new ArrayList<>();
    for(String id : userIds) {
      Optional<UserInfo> optionalInfo = userInformationService.findById(id);
      if(optionalInfo.isPresent()) {
        QbicUserDetails userDetails = (QbicUserDetails) userDetailsService.
            loadUserByUsername(optionalInfo.get().emailAddress());
        users.add(userDetails);
      }
    }
    List<String> authorities = projectAccessService.listAuthorities(projectId).stream().distinct()
        .toList();
    var entries = users.stream().map(userDetail -> {
      var roles = getProjectRoles(authorities, userDetail);
      roles = roles.stream()
          .map(this::formatAuthorityToReadableString)
          .toList();
      String fullName = userInformationService.findById(userDetail.getUserId()).get().fullName();
      return new UserProjectAccess(fullName, userDetail.getEmailAddress(), String.join(", ", roles));
    }).toList();
    List<UserProjectAccess> userProjectAccesses = new ArrayList<>(entries);
    setUserProjectAccessGridData(userProjectAccesses.stream().distinct().collect(Collectors.toList()));
  }

  private String formatAuthorityToReadableString(String authority) {
    return authority.replaceFirst("ROLE_", "")
        .replaceAll("_", " ")
        .toLowerCase();
  }

  private void setUserProjectAccessGridData(List<UserProjectAccess> userProjectAccesses) {
    userProjectAccessGrid.setItems(userProjectAccesses);
  }

  private void openEditUserAccessToProjectDialog() {
    EditUserAccessToProjectDialog editUserAccessToProjectDialog = new EditUserAccessToProjectDialog(
        projectAccessService,
        context.projectId().orElseThrow(),
        sidRepository, userInformationService);
    addEditUserAccessToProjectDialogListeners(editUserAccessToProjectDialog);
    editUserAccessToProjectDialog.open();
  }

  private void addEditUserAccessToProjectDialogListeners(
      EditUserAccessToProjectDialog editUserAccessToProjectDialog) {
    editUserAccessToProjectDialog.addCancelEventListener(
        addUserToProjectDialogCancelEvent -> editUserAccessToProjectDialog.close());
    editUserAccessToProjectDialog.addConfirmEventListener(addUserToProjectDialogConfirmEvent -> {
      List<UserInfo> addedUsers = editUserAccessToProjectDialog.getUserSelectionContent().addedUsers()
          .stream()
          .toList();
      List<UserInfo> removedUsers = editUserAccessToProjectDialog.getUserSelectionContent()
          .removedUsers()
          .stream().toList();
      if (!addedUsers.isEmpty()) {
        addUsersToProject(addedUsers);
      }
      if (!removedUsers.isEmpty()) {
        removeUsersFromProject(removedUsers);
      }
      loadInformationForProject(context.projectId().orElseThrow());
      editUserAccessToProjectDialog.close();
    });
  }

  private void addUsersToProject(List<UserInfo> users) {
    for (UserInfo user : users) {
      projectAccessService.grant(user.id(), context.projectId().orElseThrow(), BasePermission.READ);
      accessDomainService.grantProjectAccessFor(context.projectId().orElseThrow().value(),
          user.id());
    }
  }

  private void removeUsersFromProject(List<UserInfo> users) {
    for (UserInfo user : users) {
      projectAccessService.denyAll(user.id(), context.projectId().orElseThrow());
    }
  }

  private record UserProjectAccess(String fullName, String emailAddress, String projectRole) {

  }

  private record RoleProjectAccess(String projectRole) {

  }

}
