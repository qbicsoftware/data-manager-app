package life.qbic.datamanager.views.projects.project.access;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.ComboBoxVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.shared.Registration;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import life.qbic.application.commons.SortOrder;
import life.qbic.datamanager.views.general.DialogWindow;
import life.qbic.identity.api.UserInfo;
import life.qbic.identity.api.UserInformationService;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.ProjectCollaborator;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.ProjectRole;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.ProjectRoleRecommendationRenderer;
import life.qbic.projectmanagement.domain.model.project.ProjectId;

/**
 * TODO! next step use this dialog to add people to a project
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public class AddCollaboratorToProjectDialog extends DialogWindow {

  @Serial
  private static final long serialVersionUID = 6582904858073255011L;
  private final RadioButtonGroup<ProjectRole> projectRoleSelection;
  private final ComboBox<UserInfo> userSelection;
  private final ProjectId projectId;

  private transient ProjectCollaborator projectCollaborator;

  public AddCollaboratorToProjectDialog(UserInformationService userInformationService,
      ProjectAccessService projectAccessService, ProjectId projectId,
      List<ProjectCollaborator> alreadyExistingCollaborators) {
    requireNonNull(userInformationService, "userInformationService must not be null");
    requireNonNull(projectAccessService, "projectAccessService must not be null");
    this.projectId = requireNonNull(projectId, "projectId must not be null");

    projectRoleSelection = projectRoleRadioButtons();
    projectRoleSelection.setVisible(false);
    projectRoleSelection.setHelperText("Please select the role of the person within your project.");
    userSelection = new ComboBox<>();
    userSelection.addThemeVariants(ComboBoxVariant.LUMO_HELPER_ABOVE_FIELD);
    userSelection.setItems(query -> {
      List<SortOrder> sortOrders = query.getSortOrders().stream().map(
              it -> new SortOrder(it.getSorted(), it.getDirection().equals(SortDirection.DESCENDING)))
          .collect(Collectors.toCollection(ArrayList::new));
      // if no order is provided by the grid order by username
      sortOrders.add(SortOrder.of("userName").descending());
      List<UserInfo> allActiveWithUsername = userInformationService.findAllActive(
          query.getFilter().orElse(null), query.getOffset(),
          query.getLimit(), List.copyOf(sortOrders));
      // filter for not already
      return allActiveWithUsername.stream()
          .filter(userInfo -> alreadyExistingCollaborators.stream()
              .noneMatch(collaborator -> collaborator.userId().equals(userInfo.id())));
    });
    userSelection.setItemLabelGenerator(UserInfo::platformUserName);
    userSelection.setRenderer(new ComponentRenderer<>(
        userInfo -> {
          Span userName = new Span(userInfo.platformUserName());
          userName.addClassName("new-collaborator-username");
          return userName;
        }
    ));
    userSelection.setHelperText("Please select the person you want to add.");

    projectRoleSelection.addValueChangeListener(
        valueChanged -> this.projectCollaborator = new ProjectCollaborator(
            userSelection.getValue().id(),
            this.projectId, valueChanged.getValue()));
    userSelection.addValueChangeListener(valueChangeEvent -> {
      if (Objects.isNull(valueChangeEvent.getValue())) {
        projectRoleSelection.setVisible(false);
        this.projectCollaborator = null;
        userSelection.setHelperText("Please select the person you want to add.");
      } else {
        userSelection.setHelperText(null);
        projectRoleSelection.clear();
        projectRoleSelection.setValue(ProjectRole.READ);
        projectRoleSelection.setVisible(true);
      }
    });
    this.addClassName("add-user-to-project-dialog");
    add(new VerticalLayout(userSelection, projectRoleSelection));
  }

  private static RadioButtonGroup<ProjectRole> projectRoleRadioButtons() {
    RadioButtonGroup<ProjectRole> radioButtonGroup = new RadioButtonGroup<>();
    radioButtonGroup.setLabel("Project Role");
    radioButtonGroup.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL,
        RadioGroupVariant.LUMO_HELPER_ABOVE_FIELD);
    radioButtonGroup.setItems(List.of(
        ProjectRole.READ,
        ProjectRole.WRITE,
        ProjectRole.ADMIN
    ));
    radioButtonGroup.setRequired(true);
    radioButtonGroup.setValue(ProjectRole.READ);
    radioButtonGroup.setRenderer(new ComponentRenderer<>(
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
    return radioButtonGroup;
  }


  @Override
  protected void onConfirmClicked(ClickEvent<Button> clickEvent) {
    Optional.ofNullable(projectCollaborator).ifPresent(it ->
        fireEvent(new ConfirmEvent(this, clickEvent.isFromClient(), it)));
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
