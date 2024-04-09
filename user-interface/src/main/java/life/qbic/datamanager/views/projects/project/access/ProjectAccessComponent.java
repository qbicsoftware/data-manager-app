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
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
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
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.projects.project.access.AddCollaboratorToProjectDialog.ConfirmEvent;
import life.qbic.identity.api.UserInfo;
import life.qbic.identity.api.UserInformationService;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.authorization.QbicUserDetails;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.ProjectRole;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.infrastructure.project.access.SidRepository;
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
  private final transient SidRepository sidRepository;
  private final transient UserInformationService userInformationService;
  private final UserPermissions userPermissions;
  private final Grid<ProjectAccessService.ProjectCollaborator> projectCollaborators;
  private final Span buttonBar;

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

    buttonBar = new Span();
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

  private Grid<ProjectAccessService.ProjectCollaborator> projectCollaboratorGrid(
      UserInformationService userInformationService, UserPermissions userPermissions) {
    Grid<ProjectAccessService.ProjectCollaborator> grid = new Grid<>();
    Column<ProjectAccessService.ProjectCollaborator> usernameColumn = grid.addColumn(
            projectCollaborator -> userInformationService.findById(projectCollaborator.userId())
                .map(UserInfo::userName).orElseThrow())
        .setKey("username");
    Column<ProjectAccessService.ProjectCollaborator> projectRoleColumn = grid.addColumn(
            new ComponentRenderer<>(
                collaborator -> renderProjectRoleComponent(userPermissions, collaborator)))
        .setKey("projectRole");
    grid.addColumn(new ComponentRenderer<>(
            collaborator ->
                collaborator.projectRole() == ProjectRole.OWNER || isCurrentUser(collaborator)
                    ? new Span() // you cannot remove yourself or the owner
                    : new Button("Remove", clickEvent -> removeCollaborator(collaborator))))
        .setKey("removeButton");
    grid.sort(
        List.of(new GridSortOrder<>(projectRoleColumn, SortDirection.DESCENDING),
            new GridSortOrder<>(usernameColumn, SortDirection.ASCENDING)));
    grid.setSelectionMode(SelectionMode.NONE);
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
    String labelPrefix = "Role: ";
    if (collaborator.projectRole() == ProjectRole.OWNER) {
      return new Span(labelPrefix + collaborator.projectRole().label()); //can not change owner
    }
    if (isCurrentUser(collaborator)) {
      return new Span(labelPrefix + collaborator.projectRole().label());
    }
    if (!userPermissions.changeProjectAccess(context.projectId().orElseThrow())) {
      return new Span(labelPrefix + collaborator.projectRole()
          .label()); //insufficient permissions to change roles
    }

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
    roleSelect.addValueChangeListener(
        valueChanged -> onProjectRoleSelectionChanged(collaborator, valueChanged));
    return roleSelect;
  }

  private static boolean isCurrentUser(ProjectAccessService.ProjectCollaborator collaborator) {
    return Objects.equals(collaborator.userId(),
        ((QbicUserDetails) SecurityContextHolder.getContext()
            .getAuthentication().getPrincipal()).getUserId());
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
    addCollaboratorToProjectDialog.addConfirmListener(this::onAddCollaboratorConfirmed);
  }

  private void onAddCollaboratorConfirmed(ConfirmEvent event) {
    projectAccessService.addCollaborator(context.projectId().orElseThrow(),
        event.projectCollaborator()
            .userId(), event.projectCollaborator().projectRole());
    reloadProjectCollaborators(projectCollaborators, projectAccessService);
    event.getSource().close();
  }

}
