package life.qbic.datamanager.views.projects.project.access;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

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
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.ProjectCollaborator;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.ProjectRole;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.ProjectRoleRecommendationRenderer;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;

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
  private static final Logger log = logger(AddCollaboratorToProjectDialog.class);
  private final Div projectRoleSelectionSection = new Div();
  private final Div personSelectionSection = new Div();
  private final RadioButtonGroup<ProjectRole> projectRoleSelection = new RadioButtonGroup<>();
  private final ComboBox<UserInfo> personSelection = new ComboBox<>();
  private final ProjectId projectId;

  public AddCollaboratorToProjectDialog(UserInformationService userInformationService,
      ProjectId projectId,
      List<ProjectCollaborator> projectCollaborators) {
    requireNonNull(userInformationService, "userInformationService must not be null");
    this.projectId = requireNonNull(projectId, "projectId must not be null");
    addClassName("add-user-to-project-dialog");
    initPersonSelection(userInformationService, projectCollaborators);
    initProjectRoleSelection();
    setHeaderTitle("Add Collaborator");
    add(personSelectionSection, projectRoleSelectionSection);
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
        "Please select the username of the person you want to grant access to");
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
    personSelection.setPlaceholder("Please select a username");
    personSelection.setRenderer(new ComponentRenderer<>(
        AddCollaboratorToProjectDialog::renderUserInfo
    ));
    personSelection.addClassName("person-selection");
    personSelectionSection.addClassName("person-selection-section");
    personSelectionSection.add(title, description, personSelection);

  }

  private void initProjectRoleSelection() {
    Span title = new Span("Assign a Role");
    title.addClassNames("section-title");
    Span description = new Span("Please select the role of the person within the project");
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
    projectRoleSelection.setInvalid(projectRoleSelection.isEmpty());
    if (!personSelection.isInvalid() && !projectRoleSelection.isInvalid()) {
      String userId = personSelection.getValue().id();
      ProjectRole projectRole = projectRoleSelection.getValue();
      ProjectCollaborator projectCollaborator = new ProjectCollaborator(userId, projectId,
          projectRole);
      fireEvent(new ConfirmEvent(this, clickEvent.isFromClient(), projectCollaborator));
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

    private final ProjectCollaborator projectCollaborator;

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
}
