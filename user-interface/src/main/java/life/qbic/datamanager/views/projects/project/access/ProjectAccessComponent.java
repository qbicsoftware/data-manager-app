package life.qbic.datamanager.views.projects.project.access;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.List;
import java.util.Objects;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.security.UserPermissions;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.account.UserAvatar;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.notifications.NotificationDialog;
import life.qbic.datamanager.views.projects.project.access.AddCollaboratorToProjectDialog.ProjectCollaboratorConfirmedEvent;
import life.qbic.identity.api.AuthenticationToUserIdTranslator;
import life.qbic.identity.api.UserInfo;
import life.qbic.identity.api.UserInformationService;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.ProjectCollaborator;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.ProjectRole;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;

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
  private final transient UserInformationService userInformationService;
  private final UserPermissions userPermissions;
  private final Grid<ProjectAccessService.ProjectCollaborator> projectCollaborators;
  private final Span buttonBar;
  private final AuthenticationToUserIdTranslator authenticationToUserIdTranslator;
  private Context context;

  protected ProjectAccessComponent(
      @Autowired ProjectAccessService projectAccessService,
      @Autowired UserInformationService userInformationService,
      UserPermissions userPermissions,
      AuthenticationToUserIdTranslator authenticationToUserIdTranslator) {
    this.projectAccessService = requireNonNull(projectAccessService,
        "projectAccessService must not be null");
    this.userInformationService = requireNonNull(userInformationService,
        "userInformationService must not be null");
    this.userPermissions = requireNonNull(userPermissions, "userPermissions must not be null");
    this.authenticationToUserIdTranslator = requireNonNull(authenticationToUserIdTranslator,
        "authenticationToUserIdTranslator must not be null");
    this.addClassName("project-access-component");
    log.debug("New instance for %s(#%d)".formatted(ProjectAccessComponent.class.getSimpleName(),
        System.identityHashCode(this)));

    Div header = new Div();
    header.addClassName("header");
    Span titleField = new Span();
    titleField.setText("Project Access Management");
    titleField.addClassName("title");

    buttonBar = new Span();
    header.add(titleField, buttonBar);
    add(header);
    Span userProjectAccessDescription = new Span("Users with access to this project");

    projectCollaborators = projectCollaboratorGrid();
    add(userProjectAccessDescription, projectCollaborators);
  }

  private boolean isCurrentUser(ProjectAccessService.ProjectCollaborator collaborator) {
    var userId = this.authenticationToUserIdTranslator.translateToUserId(
        SecurityContextHolder.getContext().getAuthentication()).orElseThrow();
    return Objects.equals(collaborator.userId(), userId);
  }

  private Button addCollaboratorButton() {
    Button button = new Button("Add people");
    button.addClickListener(event -> openAddCollaboratorDialog());
    return button;
  }

  private void showControls() {
    buttonBar.removeAll();
    buttonBar.add(addCollaboratorButton());
  }

  private void removeControls() {
    buttonBar.removeAll();
  }

  private Grid<ProjectAccessService.ProjectCollaborator> projectCollaboratorGrid() {

    Grid<ProjectAccessService.ProjectCollaborator> grid = new Grid<>();
    Editor<ProjectCollaborator> editor = grid.getEditor();
    Binder<ProjectCollaborator> binder = new Binder<>(ProjectCollaborator.class);
    editor.setBinder(binder);
    var usernameColumn = grid.addComponentColumn(
            projectCollaborator -> userInformationService.findById(projectCollaborator.userId())
                .map(ProjectAccessComponent::renderUserInfo)
                .orElse(null))
        .setKey("user").setHeader("User").setAutoWidth(true);
    Column<ProjectAccessService.ProjectCollaborator> projectRoleColumn = grid.addColumn(
            collaborator -> "Role: " + collaborator.projectRole().label())
        .setKey("projectRole").setHeader("Role").setEditorComponent(
            this::renderProjectRoleComponent).setAutoWidth(true);
    grid.addComponentColumn(collaborator -> {
      //You can't remove or edit your own role
      if (isCurrentUser(collaborator)) {
        return new Span();
      }
      //You can't remove or edit the project owner
      if (collaborator.projectRole() == ProjectRole.OWNER) {
        return new Span();
      }
      //You don't have the rights to change the user
      if (!userPermissions.changeProjectAccess(context.projectId().orElseThrow())) {
        return new Span();
      }
      return changeProjectAccessCell(collaborator);
    }).setHeader("Action").setAutoWidth(true);
    grid.sort(
        List.of(new GridSortOrder<>(usernameColumn, SortDirection.ASCENDING),
            new GridSortOrder<>(projectRoleColumn, SortDirection.DESCENDING)));
    grid.setSelectionMode(SelectionMode.NONE);
    return grid;
  }

  private static Component renderUserInfo(UserInfo userInfo) {
    UserAvatar userAvatar = new UserAvatar();
    userAvatar.setUserId(userInfo.id());
    userAvatar.setName(userInfo.platformUserName());
    return new UserAvatarWithNameComponent(userAvatar, userInfo.platformUserName());
  }

  private Span changeProjectAccessCell(ProjectCollaborator collaborator) {
    Span changeProjectAccessCell = new Span();
    //We want to ensure that even if the frontend components are shown no event is propagated
    // if the user doesn't have the correct role or tries to remove himself/the project owner
    Button removeButton = new Button("Remove", clickEvent -> {
      if (isCurrentUser(collaborator)) {
        displayError("Invalid user removal", "You can't remove yourself from a project");
        return;
      }
      if (collaborator.projectRole() == ProjectRole.OWNER) {
        displayError("Invalid user removal", "You can't remove the owner of a project");
        return;
      }
      if (!userPermissions.changeProjectAccess(context.projectId().orElseThrow())) {
        displayError("Invalid user removal",
            "You don't have permission to remove the user from this project");
        return;
      }
      removeCollaborator(collaborator);
    });
    //We want to ensure that even if the frontend components are shown no event is propagated
    // if the user doesn't have the correct role or tries to edit himself/the project owner
    Button editButton = new Button("Edit", clickEvent -> {
      if (isCurrentUser(collaborator)) {
        displayError("Invalid role edit", "You can't change your own project role");
        return;
      }
      if (collaborator.projectRole() == ProjectRole.OWNER) {
        displayError("Invalid role edit", "You can't change the owner of this project");
        return;
      }
      if (!userPermissions.changeProjectAccess(context.projectId().orElseThrow())) {
        displayError("Invalid role edit",
            "You don't have permission to change the role of this collaborator");
        return;
      }
      if (projectCollaborators.getEditor().isOpen()) {
        projectCollaborators.getEditor().cancel();
        projectCollaborators.getEditor().closeEditor();
        return;
      }
      projectCollaborators.getEditor().editItem(collaborator);
    });
    changeProjectAccessCell.add(editButton, removeButton);
    changeProjectAccessCell.addClassName("change-project-access-cell");
    return changeProjectAccessCell;
  }

  private void reloadProjectCollaborators(Grid<ProjectAccessService.ProjectCollaborator> grid,
      ProjectAccessService projectAccessService) {
    List<ProjectAccessService.ProjectCollaborator> collaborators = projectAccessService.listCollaborators(
        context.projectId().orElseThrow());
    grid.setItems(collaborators);
  }

  private Component renderProjectRoleComponent(
      ProjectAccessService.ProjectCollaborator collaborator) {
    String labelPrefix = "Role: ";
    Select<ProjectRole> roleSelect = new Select<>();
    roleSelect.addClassName("project-role-select");
    roleSelect.setItemLabelGenerator(ProjectRole::label);
    roleSelect.setPrefixComponent(new Span(labelPrefix));
    roleSelect.setItems(
        ProjectRole.READ,
        ProjectRole.WRITE,
        ProjectRole.ADMIN
    );
    roleSelect.setRenderer(new ComponentRenderer<>(
        projectRole -> {
          Span roleLabel = new Span(projectRole.label());
          roleLabel.addClassName("project-role-label");

          Span roleDescription = new Span(projectRole.description());
          roleDescription.addClassName("project-role-description");

          Div projectRoleDiv = new Div();
          projectRoleDiv.addClassName("project-role-item");
          projectRoleDiv.add(roleLabel, roleDescription);
          return projectRoleDiv;
        }));

    roleSelect.setValue(collaborator.projectRole());
    roleSelect.addValueChangeListener(valueChanged -> {
      onProjectRoleSelectionChanged(collaborator, valueChanged);
      projectCollaborators.getEditor().save();
      projectCollaborators.getEditor().closeEditor();
      reloadProjectCollaborators(projectCollaborators, projectAccessService);
    });
    return roleSelect;
  }

  private void removeCollaborator(ProjectAccessService.ProjectCollaborator collaborator) {
    ProjectId projectId = context.projectId().orElseThrow();
    projectAccessService.removeCollaborator(projectId, collaborator.userId());
    reloadProjectCollaborators(projectCollaborators, projectAccessService);
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
    if (userPermissions.changeProjectAccess(context.projectId().orElseThrow())) {
      showControls();
    } else {
      removeControls();
    }
  }


  private void openAddCollaboratorDialog() {
    List<ProjectAccessService.ProjectCollaborator> alreadyExistingCollaborators = projectAccessService.listCollaborators(
        context.projectId().orElseThrow());
    AddCollaboratorToProjectDialog addCollaboratorToProjectDialog = new AddCollaboratorToProjectDialog(
        userInformationService, projectAccessService, context.projectId().orElseThrow(),
        alreadyExistingCollaborators);
    addCollaboratorToProjectDialog.open();
    addCollaboratorToProjectDialog.addCancelListener(event -> event.getSource().close());
    addCollaboratorToProjectDialog.addProjectCollaboratorConfirmedListener(
        this::onAddCollaboratorConfirmed);
  }

  private void onAddCollaboratorConfirmed(ProjectCollaboratorConfirmedEvent event) {
    projectAccessService.addCollaborator(context.projectId().orElseThrow(),
        event.projectCollaborator()
            .userId(), event.projectCollaborator().projectRole());
    reloadProjectCollaborators(projectCollaborators, projectAccessService);
    event.getSource().close();
  }

  private void displayError(String title, String description) {
    NotificationDialog dialog = NotificationDialog.errorDialog();
    dialog.withTitle(title);
    dialog.withContent(new Span(description));
    dialog.open();
  }

}
