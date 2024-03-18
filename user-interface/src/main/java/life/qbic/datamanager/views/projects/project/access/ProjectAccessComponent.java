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
import life.qbic.datamanager.views.projects.project.access.EditUserAccessToProjectDialog.ProjectCollaborator;
import life.qbic.identity.api.UserInfo;
import life.qbic.identity.api.UserInformationService;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.ProjectRole;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.infrastructure.project.access.SidRepository;
import org.springframework.beans.factory.annotation.Autowired;

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
  private final SidRepository sidRepository;
  private final UserInformationService userInformationService;
  private static final Logger log = logger(ProjectAccessMain.class);
  private final Div content = new Div();
  private final Div header = new Div();
  private final Span buttonBar = new Span();
  private final Span titleField = new Span();
  private final Grid<UserProjectAccess> userProjectAccessGrid = new Grid<>(
      UserProjectAccess.class);
  private Context context;

  protected ProjectAccessComponent(
      @Autowired ProjectAccessService projectAccessService,
      @Autowired UserInformationService userInformationService,
      @Autowired SidRepository sidRepository) {
    this.projectAccessService = projectAccessService;
    this.userInformationService = userInformationService;
    this.sidRepository = sidRepository;
    requireNonNull(projectAccessService, "projectAccessService must not be null");
    requireNonNull(userInformationService, "userRepository must not be null");
    requireNonNull(sidRepository, "sidRepository must not be null");
    layoutComponent();
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

  private void layoutComponent() {
    initHeader();
    initContent();
  }

  private void initHeader() {
    header.addClassName("header");
    titleField.setText("Project Access Management");
    titleField.addClassName("title");
    initButtonBar();
    header.add(titleField, buttonBar);
    add(header);
  }

  private void initButtonBar() {
    Button editButton = new Button("Edit");
    editButton.addClickListener(event -> openEditUserAccessToProjectDialog());
    buttonBar.add(editButton);
  }

  private void initContent() {
    content.addClassName("content");
    layoutUserProjectAccessGrid();
    add(content);
  }

  private void layoutUserProjectAccessGrid() {
    Span userProjectAccessDescription = new Span("Users with access to this project");
    userProjectAccessGrid.addColumn(UserProjectAccess::userName).setHeader("username");
    Div userProjectAccess = new Div(userProjectAccessDescription, userProjectAccessGrid);
    userProjectAccess.addClassName("user-access");
    content.add(userProjectAccess);
  }

  private void loadInformationForProject(ProjectId projectId) {
    loadProjectAccessibleUsers(projectId);
  }

  //shows active users in the UI
  private void loadProjectAccessibleUsers(ProjectId projectId) {
    List<String> userIds = projectAccessService.listCollaborators(projectId).stream()
        .map(ProjectAccessService.ProjectCollaborator::userId)
        .toList();
    var entries = userIds.stream()
        .map(userInformationService::findById)
        .filter(Optional::isPresent)
        .map(it -> it.map(UserInfo::userName)
            .map(userName -> userName.isBlank() ? "no username" : userName)
            .orElseThrow())
        .map(UserProjectAccess::new)
        .toList();

    List<UserProjectAccess> userProjectAccesses = new ArrayList<>(entries);
    setUserProjectAccessGridData(userProjectAccesses.stream().distinct().collect(Collectors.toList()));
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
      List<String> addedUsers = editUserAccessToProjectDialog.getUserSelectionContent().addedUsers()
          .stream()
          .map(ProjectCollaborator::userId)
          .toList();
      List<String> removedUsers = editUserAccessToProjectDialog.getUserSelectionContent()
          .removedUsers()
          .stream()
          .map(ProjectCollaborator::userId)
          .toList();
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

  private void addUsersToProject(List<String> userIDs) {
    ProjectId projectId = context.projectId().orElseThrow();
    for (String userId : userIDs) {
      projectAccessService.addCollaborator(projectId, userId, ProjectRole.READ);
    }
  }

  private void removeUsersFromProject(List<String> userIDs) {
    ProjectId projectId = context.projectId().orElseThrow();
    for (String userId : userIDs) {
      projectAccessService.removeCollaborator(projectId, userId);
    }
  }

  private record UserProjectAccess(String userName) {

  }

}
