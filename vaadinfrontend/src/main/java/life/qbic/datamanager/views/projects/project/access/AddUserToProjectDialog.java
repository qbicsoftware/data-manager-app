package life.qbic.datamanager.views.projects.project.access;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import life.qbic.authentication.domain.user.concept.User;
import life.qbic.authentication.domain.user.repository.UserInformationService;
import life.qbic.datamanager.views.general.CancelEvent;
import life.qbic.datamanager.views.general.ConfirmEvent;
import life.qbic.datamanager.views.general.DialogWindow;

/**
 * <class short description - One Line!>
 * <p>
 * <More detailed description - When to use, what it solves, etc.>
 *
 * @since <version tag>
 */
public class AddUserToProjectDialog extends DialogWindow {

  @Serial
  private static final long serialVersionUID = -7896582476882842608L;
  private static final String TITLE = "Add Users to Project";
  private final Grid<User> userGrid = new Grid<>();
  private final TextField searchField = new TextField();
  private final List<ComponentEventListener<CancelEvent<AddUserToProjectDialog>>> cancelEventListeners = new ArrayList<>();
  private final List<ComponentEventListener<ConfirmEvent<AddUserToProjectDialog>>> confirmEventListeners = new ArrayList<>();
  private final transient UserInformationService userInformationService;

  public AddUserToProjectDialog(UserInformationService userInformationService) {
    super();
    this.userInformationService = Objects.requireNonNull(userInformationService);
    addClassName("add-user-to-project-dialog");
    layoutComponent();
    configureComponent();
  }

  private void layoutComponent() {
    setHeaderTitle(TITLE);
    initSearchField();
    layoutGrid();
    addUsersToGrid();
    setConfirmButtonLabel("Add");
    setCancelButtonLabel("Cancel");
    final DialogFooter footer = getFooter();
    footer.add(this.cancelButton, this.confirmButton);
  }

  private void layoutGrid() {
    userGrid.addColumn(User -> User.fullName().get()).setHeader("User Name");
    userGrid.addColumn(User -> User.emailAddress().get()).setHeader("User Email");
    userGrid.setSelectionMode(SelectionMode.MULTI);
    add(userGrid);
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
    userGrid.setItems(userInformationService.findAllActiveUsers());
  }

  private boolean matchesTerm(String value, String searchTerm) {
    return value.toLowerCase().contains(searchTerm.toLowerCase());
  }

  private void configureComponent() {
    configureSearching();
    configureCancelling();
    configureConfirmation();
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
      boolean matchesFullName = matchesTerm(user.fullName().get(),
          searchTerm);
      boolean matchesEmail = matchesTerm(user.emailAddress().get(), searchTerm);
      return matchesFullName || matchesEmail;
    });
  }

  private void configureConfirmation() {
    this.confirmButton.addClickListener(event -> fireConfirmEvent());
  }

  private void configureCancelling() {
    this.cancelButton.addClickListener(cancelListener -> fireCancelEvent());
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
      final ComponentEventListener<ConfirmEvent<AddUserToProjectDialog>> listener) {
    this.confirmEventListeners.add(listener);
  }

  /**
   * Adds a listener for {@link CancelEvent}s
   *
   * @param listener the listener to add
   */
  public void addCancelEventListener(
      final ComponentEventListener<CancelEvent<AddUserToProjectDialog>> listener) {
    this.cancelEventListeners.add(listener);
  }

  /**
   * Provides set of users selected in the Dialog
   *
   * @return set of {@link User} which were selected within this dialog
   */
  public Set<User> getSelectedUsers() {
    return userGrid.getSelectedItems();
  }
}
