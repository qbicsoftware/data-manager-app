package life.qbic.datamanager.views.projects.project.access;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.List;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.security.UserPermissions;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.projects.project.access.EditUserAccessToProjectDialog.ProjectCollaborator;
import life.qbic.identity.api.UserInfo;
import life.qbic.identity.api.UserInformationService;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.ProjectRole;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.ProjectRoleRecommendationRenderer;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.infrastructure.project.access.SidRepository;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Project Access Component
 * <p>
 * The access component is a {@link PageArea} component, which shows the current permissions for all
 * users and user groups within a {@link Project}.
 * <p>
 * Additionally, it provides the possibility to add or revoke project access for individual users
 * and user groups for the selected {@link Project}.
 * <p>
 */
@SpringComponent
@UIScope
public class ProjectAccessComponent extends PageArea {

  private static final Logger log = logger(ProjectAccessMain.class);
  @Serial
  private static final long serialVersionUID = 6832688939965353201L;

  private final transient ProjectAccessService projectAccessService;
  private final transient SidRepository sidRepository;
  private final transient UserInformationService userInformationService;
  private final UserPermissions userPermissions;
  private final Grid<ProjectAccessService.ProjectCollaborator> projectCollaborators;

  private Context context;

  protected ProjectAccessComponent(
      @Autowired ProjectAccessService projectAccessService,
      @Autowired UserInformationService userInformationService,
      @Autowired SidRepository sidRepository, UserPermissions userPermissions) {
    this.projectAccessService = requireNonNull(projectAccessService,
        "projectAccessService must not be null");
    this.userInformationService = requireNonNull(userInformationService,
        "userInformationService must not be null");
    this.sidRepository = requireNonNull(sidRepository, "sidRepository must not be null");
    this.userPermissions = requireNonNull(userPermissions, "userPermissions must not be null");

    this.addClassName("project-access-component");
    log.debug("New instance for %s(#%d)".formatted(ProjectAccessComponent.class.getSimpleName(),
        System.identityHashCode(this)));

    Div header = new Div();
    header.addClassName("header");
    Span titleField = new Span();
    titleField.setText("Project Access Management");
    titleField.addClassName("title");

    Button editButton = new Button("Edit");
    editButton.addClickListener(event -> openEditUserAccessToProjectDialog());

    Span buttonBar = new Span();
    buttonBar.add(editButton);
    header.add(titleField, buttonBar);
    add(header);

    Div content = new Div();
    content.addClassName("content");
    Span userProjectAccessDescription = new Span("Users with access to this project");

    projectCollaborators = projectCollaboratorGrid(userInformationService, this.userPermissions);
    Div userProjectAccess = new Div(userProjectAccessDescription, projectCollaborators);
    userProjectAccess.addClassName("user-access");
    content.add(userProjectAccess);
    add(content);
  }

  private Grid<ProjectAccessService.ProjectCollaborator> projectCollaboratorGrid(
      UserInformationService userInformationService, UserPermissions userPermissions) {
    Grid<ProjectAccessService.ProjectCollaborator> grid = new Grid<>();
    Column<ProjectAccessService.ProjectCollaborator> usernameColumn = grid.addColumn(
            projectCollaborator -> userInformationService.findById(projectCollaborator.userId())
                .map(UserInfo::userName).orElseThrow())
        .setHeader("username")
        .setKey("username");
    Column<ProjectAccessService.ProjectCollaborator> projectRoleColumn = grid.addColumn(
            ProjectAccessService.ProjectCollaborator::projectRole)
        .setRenderer(new ComponentRenderer<>(
            collaborator -> renderProjectRoleComponent(userPermissions, collaborator)))
        .setHeader("project role")
        .setKey("projectRole");
    grid.sort(
        List.of(new GridSortOrder<>(projectRoleColumn, SortDirection.DESCENDING),
            new GridSortOrder<>(usernameColumn, SortDirection.ASCENDING)));
    return grid;
  }

  private void reloadProjectCollaborators(Grid<ProjectAccessService.ProjectCollaborator> grid,
      ProjectAccessService projectAccessService) {
    List<ProjectAccessService.ProjectCollaborator> collaborators = projectAccessService.listCollaborators(
        context.projectId().orElseThrow());
    grid.setItems(collaborators);
  }

  private Component renderProjectRoleComponent(UserPermissions userPermissions,
      ProjectAccessService.ProjectCollaborator collaborator) {
    if (collaborator.projectRole() == ProjectRole.OWNER) {
      return new Span(collaborator.projectRole().label());
    }
    if (!userPermissions.changeProjectAccess(context.projectId().orElseThrow())) {
      return new Span(collaborator.projectRole().label());
    }

    Select<ProjectRole> roleSelect = new Select<>();
    roleSelect.setItemLabelGenerator(ProjectRole::label);
    roleSelect.setItems(
        ProjectRole.READ,
        ProjectRole.WRITE,
        ProjectRole.ADMIN
    );
    roleSelect.setRenderer(new ComponentRenderer<>(
        projectRole -> new Div(new Span(projectRole.label()), new Span(
            ProjectRoleRecommendationRenderer.render(
                projectRole)))));
    roleSelect.setValue(collaborator.projectRole());
    roleSelect.addValueChangeListener(
        valueChanged -> onProjectRoleSelectionChanged(collaborator, valueChanged));
    return roleSelect;
  }

  private void onProjectRoleSelectionChanged(ProjectAccessService.ProjectCollaborator collaborator,
      ComponentValueChangeEvent<Select<ProjectRole>, ProjectRole> valueChanged) {
    projectAccessService.changeRole(context.projectId().orElseThrow(), collaborator.userId(),
        valueChanged.getValue());
  }


  public void setContext(Context context) {
    if (context.projectId().isEmpty()) {
      throw new ApplicationException("no project id in context " + context);
    }
    this.context = context;
    onProjectChanged();
  }

  private void onProjectChanged() {
    reloadProjectCollaborators(projectCollaborators, projectAccessService);
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
      reloadProjectCollaborators(projectCollaborators, projectAccessService);
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

}
