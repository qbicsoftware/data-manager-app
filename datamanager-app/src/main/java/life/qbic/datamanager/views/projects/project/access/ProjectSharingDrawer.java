package life.qbic.datamanager.views.projects.project.access;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import life.qbic.datamanager.views.AppRoutes.ProjectRoutes;
import life.qbic.datamanager.views.UiHandle;
import life.qbic.datamanager.views.account.UserAvatar;
import life.qbic.datamanager.views.general.Tag;
import life.qbic.datamanager.views.general.Tag.TagColor;
import life.qbic.datamanager.views.projects.project.access.ProjectSharingComposer.GrantRequest;
import life.qbic.datamanager.views.projects.project.access.ProjectSharingComposer.GrantRequestedEvent;
import life.qbic.datamanager.views.projects.project.access.ProjectSharingComposer.PrincipalType;
import life.qbic.identity.api.UserInformationService;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.ProjectCollaborator;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.ProjectRole;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.SharedProjectGroup;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.usergroups.api.GroupInformationService;
import life.qbic.usergroups.api.GroupSidProvider;

/**
 * <b>Project Sharing Drawer</b>
 *
 * <p>A non-modal, right-anchored panel that lets a user share a project with people and groups
 * directly from the project list, without navigating into the project and without a modal dialog.
 * It hosts the same {@link ProjectSharingComposer} used on the project access page and shows a
 * read-only summary of the currently granted people and groups.</p>
 *
 * <p>The panel never exposes group membership data: only group names and descriptions are shown,
 * matching the visibility policy of the user-groups strategy.</p>
 *
 * @since 1.20.0
 */
public class ProjectSharingDrawer extends Div {

  @Serial
  private static final long serialVersionUID = 3928119923411238841L;
  private static final Logger log = logger(ProjectSharingDrawer.class);

  private final transient ProjectAccessService projectAccessService;
  private final transient UserInformationService userInformationService;
  private final transient GroupInformationService groupInformationService;
  private final transient Executor taskExecutor;
  private final UiHandle uiHandle = new UiHandle();
  private final ProjectId projectId;
  private final ProjectSharingComposer composer;
  private final Div overlay = new Div();
  private final Div panel = new Div();
  private final Div summary = new Div();

  public ProjectSharingDrawer(ProjectAccessService projectAccessService,
      UserInformationService userInformationService,
      GroupInformationService groupInformationService,
      Executor taskExecutor,
      ProjectId projectId,
      String projectLabel) {
    this.projectAccessService = requireNonNull(projectAccessService);
    this.userInformationService = requireNonNull(userInformationService);
    this.groupInformationService = requireNonNull(groupInformationService);
    this.taskExecutor = requireNonNull(taskExecutor);
    this.projectId = requireNonNull(projectId);
    addClassName("project-sharing-drawer");
    composer = new ProjectSharingComposer(userInformationService, groupInformationService);
    composer.addGrantListener(this::onGrantRequested);
    layout(projectLabel);
    close();
  }

  private void layout(String projectLabel) {
    overlay.addClassName("psd-backdrop");
    overlay.addClickListener(event -> close());

    panel.addClassName("psd-panel");
    Div body = new Div();
    body.addClassName("psd-body");

    Div header = new Div();
    header.addClassName("psd-header");
    Span title = new Span("Share project");
    title.addClassName("heading-4");
    Span subtitle = new Span(projectLabel);
    subtitle.addClassName("secondary");
    Div titleBlock = new Div(title, subtitle);
    titleBlock.addClassName("psd-title");
    Button closeButton = new Button(VaadinIcon.CLOSE.create());
    closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    closeButton.getElement().setAttribute("aria-label", "Close sharing panel");
    closeButton.addClickListener(event -> close());

    Anchor manageAccess = new Anchor(
        String.format(ProjectRoutes.ACCESS, projectId.value()),
        VaadinIcon.USERS.create(), new Span("Manage access"));
    manageAccess.addClassName("psd-manage-access");
    Div headerActions = new Div(manageAccess, closeButton);
    headerActions.addClassName("psd-header-actions");
    header.add(titleBlock, headerActions);

    Div content = new Div(composer, summary);
    content.addClassName("psd-content");

    body.add(header, content);
    panel.add(body);
    add(overlay, panel);
  }

  /**
   * Opens the drawer and loads the current access summary.
   */
  public void open() {
    uiHandle.bind(UI.getCurrent());
    refresh();
    overlay.getStyle().set("display", "block");
    panel.getStyle().set("display", "block");
  }

  /**
   * Closes the drawer. The panel stays mounted so it can be reopened quickly.
   */
  public void close() {
    uiHandle.unbind();
    overlay.getStyle().set("display", "none");
    panel.getStyle().set("display", "none");
  }

  private void refresh() {
    List<ProjectCollaborator> collaborators = projectAccessService.listCollaborators(projectId);
    List<SharedProjectGroup> groups = projectAccessService.listSharedGroups(projectId);
    composer.setAlreadyGranted(collaborators, groups);
    renderSummary(collaborators, groups);
  }

  private void renderSummary(List<ProjectCollaborator> collaborators,
      List<SharedProjectGroup> groups) {
    summary.removeAll();
    summary.addClassName("psd-summary");
    summary.add(section("People with access", collaborators.stream()
        .sorted(collaboratorOrder())
        .map(this::renderCollaborator).toList()));
    summary.add(section("Groups with access", groups.stream()
        .sorted(groupOrder())
        .map(this::renderGroup).toList()));
  }

  /**
   * Roster order: highest privilege first (owner &gt; admin &gt; write &gt; read), then
   * lexicographically by last name (ascending). People without a resolvable full name fall back
   * to their username.
   */
  private Comparator<ProjectCollaborator> collaboratorOrder() {
    return Comparator
        .comparingInt((ProjectCollaborator collaborator) -> roleRank(collaborator.projectRole()))
        .thenComparing(collaborator -> lastNameOf(collaborator.userId()),
            String.CASE_INSENSITIVE_ORDER);
  }

  /**
   * Roster order for groups: highest privilege first, then by group name (ascending).
   */
  private Comparator<SharedProjectGroup> groupOrder() {
    return Comparator
        .comparingInt((SharedProjectGroup group) -> roleRank(group.projectRole()))
        .thenComparing(SharedProjectGroup::groupName, String.CASE_INSENSITIVE_ORDER);
  }

  private static int roleRank(ProjectRole role) {
    return switch (role) {
      case OWNER -> 0;
      case ADMIN -> 1;
      case WRITE -> 2;
      case READ -> 3;
    };
  }

  private String lastNameOf(String userId) {
    return userInformationService.findById(userId)
        .map(userInfo -> lastName(userInfo.fullName(), userInfo.platformUserName()))
        .orElse(userId);
  }

  private static String lastName(String fullName, String fallback) {
    if (fullName == null || fullName.isBlank()) {
      return fallback == null ? "" : fallback;
    }
    String[] parts = fullName.trim().split("\\s+");
    return parts[parts.length - 1];
  }

  private Component renderCollaborator(ProjectCollaborator collaborator) {
    var userInfo = userInformationService.findById(collaborator.userId());
    String username = userInfo.map(info -> info.platformUserName())
        .orElse(collaborator.userId());
    String fullName = userInfo.map(info -> info.fullName()).orElse(null);
    UserAvatar avatar = new UserAvatar();
    avatar.setUserId(collaborator.userId());
    avatar.setName(username);
    Span nameSpan = new Span();
    nameSpan.addClassName("psd-name");
    Span usernameSpan = new Span(username);
    usernameSpan.addClassName("bold");
    nameSpan.add(usernameSpan);
    if (fullName != null && !fullName.isBlank()) {
      Span fullNameSpan = new Span(fullName);
      fullNameSpan.addClassName("psd-full-name");
      nameSpan.add(fullNameSpan);
    }
    Div identity = new Div(avatar, nameSpan);
    identity.addClassName("psd-user-identity");
    identity.getElement().setAttribute("title",
        fullName == null || fullName.isBlank() ? username
            : "%s (%s)".formatted(username, fullName));
    return summaryRow(PrincipalType.USER, identity, collaborator.projectRole());
  }

  private Component renderGroup(SharedProjectGroup group) {
    UserAvatar avatar = new UserAvatar();
    avatar.setGroupId(group.groupId());
    avatar.addClassName("psd-group-avatar");
    Span nameSpan = new Span(group.groupName());
    nameSpan.addClassName("psd-name");
    Div identity = new Div(avatar, nameSpan);
    identity.addClassName("psd-group-identity");
    if (group.groupName() != null && !group.groupName().isBlank()) {
      identity.getElement().setAttribute("title", group.groupName());
    }
    return summaryRow(PrincipalType.GROUP, identity, group.projectRole());
  }

  private Component section(String title, List<Component> rows) {
    Div section = new Div();
    section.addClassName("psd-section");
    Span sectionTitle = new Span(title);
    sectionTitle.addClassName("psd-section-title");
    Span roleHeader = new Span("Role");
    roleHeader.addClassName("psd-role-header");
    Div headerRow = new Div(sectionTitle, roleHeader);
    headerRow.addClassName("psd-section-header");
    section.add(headerRow);
    if (rows.isEmpty()) {
      Span empty = new Span("None yet.");
      empty.addClassName("secondary");
      section.add(empty);
    } else {
      rows.forEach(section::add);
    }
    return section;
  }

  private Component summaryRow(PrincipalType type, Component identity, ProjectRole role) {
    Tag typeTag = new Tag(type == PrincipalType.USER ? "User" : "Group");
    typeTag.setTagColor(type == PrincipalType.USER ? TagColor.CONTRAST : TagColor.TEAL);
    typeTag.addClassName("psd-type-tag");
    Div identityWrapper = new Div(typeTag, identity);
    identityWrapper.addClassName("psd-identity");
    Tag roleTag = new Tag(ProjectSharingComposer.roleLabel(role));
    roleTag.setTagColor(ProjectSharingComposer.roleColor(role));
    roleTag.addClassName("psd-role-tag");
    Div row = new Div(identityWrapper, roleTag);
    row.addClassName("psd-summary-row");
    return row;
  }

  private void onGrantRequested(GrantRequestedEvent event) {
    List<GrantRequest> requests = List.copyOf(event.requests());
    composer.setBusy(true);
    // Run the ACL write on the UI thread (via UiHandle/ui.access): the app uses a
    // VaadinAwareSecurityContextHolderStrategy whose ACL strategy resolves the authenticated
    // principal from the Vaadin session, which is not available on a raw pool thread. The busy
    // overlay is sent to the client with the current response before the scheduled task runs.
    CompletableFuture.runAsync(
        () -> uiHandle.onUiAndPush(() -> onGrantsApplied(applyGrants(requests))),
        taskExecutor);
  }

  private GrantOutcome applyGrants(List<GrantRequest> requests) {
    int granted = 0;
    List<String> problems = new ArrayList<>();
    for (GrantRequest request : requests) {
      try {
        if (request.type() == PrincipalType.USER) {
          projectAccessService.addCollaborator(projectId, request.id(), request.role());
        } else {
          projectAccessService.addAuthorityAccess(projectId,
              GroupSidProvider.GROUP_SID_PREFIX + request.id(), request.role());
        }
        granted++;
      } catch (RuntimeException e) {
        log.error("Could not grant project access for %s".formatted(request.id()), e);
        problems.add(ProjectSharingComposer.describeFailure(request, e));
      }
    }
    return new GrantOutcome(granted, List.copyOf(problems));
  }

  /**
   * Keeps the drawer open after a grant: hides the spinner, refreshes the roster in place and leaves
   * the confirmation visible inline in the composer. Failures name the affected principals and why.
   */
  private void onGrantsApplied(GrantOutcome outcome) {
    composer.setBusy(false);
    composer.reset();
    refresh();
    if (!outcome.problems().isEmpty()) {
      String title = outcome.granted() > 0
          ? "Access granted to %d, but some requests failed:".formatted(outcome.granted())
          : "Access could not be granted:";
      composer.showInlineError(title, outcome.problems());
    } else if (outcome.granted() > 0) {
      composer.showInlineConfirmation(outcome.granted() == 1
          ? "Access granted to 1 principal."
          : "Access granted to %d principals.".formatted(outcome.granted()));
    }
  }

  private record GrantOutcome(int granted, List<String> problems) {

  }
}
