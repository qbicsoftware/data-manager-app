package life.qbic.datamanager.views.projects.project.access;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import java.io.Serial;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import life.qbic.projectmanagement.infrastructure.project.access.SidRepository;
import life.qbic.datamanager.views.general.CancelEvent;
import life.qbic.datamanager.views.general.ConfirmEvent;
import life.qbic.datamanager.views.general.DialogWindow;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.identity.api.UserInfo;
import life.qbic.identity.api.UserInformationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <b>Edit User Access To Project Dialog</b>
 *
 * <p>A dialog window that enables the user to add or remove the access to a project for other
 * users
 */
public class EditUserAccessToProjectDialog extends DialogWindow {

  @Serial
  private static final long serialVersionUID = -7896582476882842608L;
  private static final String TITLE = "Edit User Access to Project";
  private final Grid<UserInfo> userGrid = new Grid<>();
  private final TextField searchField = new TextField();
  private final List<ComponentEventListener<CancelEvent<EditUserAccessToProjectDialog>>> cancelEventListeners = new ArrayList<>();
  private final List<ComponentEventListener<ConfirmEvent<EditUserAccessToProjectDialog>>> confirmEventListeners = new ArrayList<>();
  private final transient SidRepository sidRepository;
  private final transient UserInformationService userInformationService;
  private final transient ProjectAccessService projectAccessService;
  private final ProjectId projectId;
  private UserSelectionContent userSelectionContent;
  private Set<UserInfo> originalUsersInProject;

  public EditUserAccessToProjectDialog(@Autowired ProjectAccessService projectAccessService,
      @Autowired ProjectId projectId,
      @Autowired SidRepository sidRepository,
      @Autowired UserInformationService userInformationService) {
    super();
    Objects.requireNonNull(projectAccessService);
    Objects.requireNonNull(projectId);
    Objects.requireNonNull(sidRepository);
    Objects.requireNonNull(userInformationService);
    this.projectAccessService = projectAccessService;
    this.projectId = projectId;
    this.sidRepository = sidRepository;
    this.userInformationService = userInformationService;
    addClassName("add-user-to-project-dialog");
    layoutComponent();
    configureComponent();
  }

  private void layoutComponent() {
    setHeaderTitle(TITLE);
    initSearchField();
    layoutGrid();
    addUsersToGrid();
    setPreselectedUsers();
    setConfirmButtonLabel("Save");
    setCancelButtonLabel("Cancel");
    final DialogFooter footer = getFooter();
    footer.add(this.cancelButton, this.confirmButton);
  }

  private void layoutGrid() {
    userGrid.addColumn(UserInfo::fullName).setHeader("User Name");
    userGrid.addColumn(UserInfo::emailAddress).setHeader("User Email");
    userGrid.setSelectionMode(SelectionMode.MULTI);
    add(userGrid);
  }

  private void setPreselectedUsers() {
    userGrid.deselectAll();
    List<String> addedUserIdsInProject = projectAccessService.listUserIds(projectId);
    originalUsersInProject = addedUserIdsInProject.stream().map(userInformationService::findById)
        .filter(Optional::isPresent).map(Optional::get)
        .collect(Collectors.toUnmodifiableSet());
    originalUsersInProject.forEach(userGrid::select);
  }


  private void initSearchField() {
    searchField.setClassName("search-field");
    searchField.setPlaceholder("Search");
    searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
    searchField.setValueChangeMode(ValueChangeMode.EAGER);
    searchField.addValueChangeListener(e -> userGrid.getListDataView().refreshAll());
    add(searchField);
  }

  private void addUsersToGrid() {
    List<String> userSids = new ArrayList<>();
    List<UserInfo> users = new ArrayList<>();
    sidRepository.findAllByPrincipalIsTrue().forEach(qBiCSid -> userSids.add(qBiCSid.getSid()));
    userSids.forEach(
        sId -> users.add(userInformationService.findById(sId).get()));
    userGrid.setItems(users);

  }

  private boolean matchesTerm(String value, String searchTerm) {
    return value.toLowerCase().contains(searchTerm.toLowerCase());
  }

  private void configureComponent() {
    configureSearching();
  }

  private void configureSearching() {
    userGrid.getListDataView().addFilter(user -> {
      String searchTerm = searchField.getValue().trim();
      //Show all users if no input was provided in searchField
      if (searchTerm.isEmpty()) {
        return true;
      }
      //Show selected Users at all Time
      if (userGrid.getSelectedItems().contains(user)) {
        return true;
      }
      boolean matchesFullName = matchesTerm(user.fullName(),
          searchTerm);
      boolean matchesEmail = matchesTerm(user.emailAddress(), searchTerm);
      return matchesFullName || matchesEmail;
    });
  }

  @Override
  protected void onConfirmClicked(ClickEvent<Button> clickEvent) {
    fireConfirmEvent();
  }

  @Override
  protected void onCancelClicked(ClickEvent<Button> clickEvent) {
    fireCancelEvent();
  }

  private void fireConfirmEvent() {
    this.confirmEventListeners.forEach(
        listener -> listener.onComponentEvent(new ConfirmEvent<>(this, true)));
  }

  private void fireCancelEvent() {
    this.cancelEventListeners.forEach(
        listener -> listener.onComponentEvent(new CancelEvent<>(this, true)));
  }

  /**
   * Adds a listener for {@link ConfirmEvent}s
   *
   * @param listener the listener to add
   */
  public void addConfirmEventListener(
      final ComponentEventListener<ConfirmEvent<EditUserAccessToProjectDialog>> listener) {
    this.confirmEventListeners.add(listener);
  }

  /**
   * Adds a listener for {@link CancelEvent}s
   *
   * @param listener the listener to add
   */
  public void addCancelEventListener(
      final ComponentEventListener<CancelEvent<EditUserAccessToProjectDialog>> listener) {
    this.cancelEventListeners.add(listener);
  }

  /**
   * Provides set of users selected and deselected in the Dialog
   *
   * @return set of {@link UserInfo} which were selected or deselected within this dialog
   */
  public UserSelectionContent getUserSelectionContent() {
    determineChangedUsers();
    return userSelectionContent;
  }

  private void determineChangedUsers() {
    Set<UserInfo> selectedUsers = new HashSet<>(userGrid.getSelectedItems());
    Set<UserInfo> preSelectedUsers = new HashSet<>(originalUsersInProject);
    preSelectedUsers.removeAll(userGrid.getSelectedItems());
    selectedUsers.removeAll(originalUsersInProject);
    userSelectionContent = new UserSelectionContent(selectedUsers, preSelectedUsers);
  }

  public record UserSelectionContent(Set<UserInfo> addedUsers, Set<UserInfo> removedUsers) {

  }
}
