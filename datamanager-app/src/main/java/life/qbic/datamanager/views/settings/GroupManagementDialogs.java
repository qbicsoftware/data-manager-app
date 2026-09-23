package life.qbic.datamanager.views.settings;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import life.qbic.datamanager.views.general.dialog.AppDialog;
import life.qbic.datamanager.views.general.dialog.DialogBody;
import life.qbic.datamanager.views.general.dialog.DialogFooter;
import life.qbic.datamanager.views.general.dialog.DialogHeader;
import life.qbic.datamanager.views.general.dialog.InputValidation;
import life.qbic.datamanager.views.general.dialog.UserInput;
import life.qbic.identity.api.UserInfo;
import life.qbic.usergroups.api.GroupMember;
import life.qbic.usergroups.api.GroupRole;

/**
 * <b>Group management dialogs</b>
 * <p>
 * Encapsulates the owner/manager management dialogs of the My Groups view (FEAT-USER-GROUPS-04):
 * manage members (list, add, remove, appoint/demote managers), rename the group, change the
 * description, and dissolve the group.
 * <p>
 * All dialogs are seam-driven: the actual operations are performed by the provided
 * {@link ManagementHandler} callbacks so the dialogs stay testable without a Vaadin {@code UI}
 * or Spring context. The presentation components (list, combobox, fields) are plain Vaadin.
 *
 * @since 1.20.0
 */
public final class GroupManagementDialogs {

  private GroupManagementDialogs() {
  }

  /**
   * Callback handler for a completed management operation. Receives the request and performs the
   * service call + refresh; may throw to signal a failure the dialog should surface.
   */
  @FunctionalInterface
  public interface ManagementHandler extends Serializable {

    void handle(ManagementRequest request);
  }

  /**
   * A completed management request carrying the affected group and (where relevant) the affected
   * member.
   */
  public static class ManagementRequest implements Serializable {

    private final String groupId;
    private final String userId;
    private final String newName;
    private final String newDescription;
    private final ManageAction action;

    private ManagementRequest(String groupId, String userId, String newName,
        String newDescription, ManageAction action) {
      this.groupId = groupId;
      this.userId = userId;
      this.newName = newName;
      this.newDescription = newDescription;
      this.action = action;
    }

    public static ManagementRequest removeMember(String groupId, String userId) {
      return new ManagementRequest(groupId, userId, null, null, ManageAction.REMOVE_MEMBER);
    }

    public static ManagementRequest addMember(String groupId, String userId) {
      return new ManagementRequest(groupId, userId, null, null, ManageAction.ADD_MEMBER);
    }

    public static ManagementRequest appointManager(String groupId, String userId) {
      return new ManagementRequest(groupId, userId, null, null, ManageAction.APPOINT_MANAGER);
    }

    public static ManagementRequest demoteManager(String groupId, String userId) {
      return new ManagementRequest(groupId, userId, null, null, ManageAction.DEMOTE_MANAGER);
    }

    public static ManagementRequest rename(String groupId, String newName) {
      return new ManagementRequest(groupId, null, newName, null, ManageAction.RENAME);
    }

    public static ManagementRequest updateDescription(String groupId, String newDescription) {
      return new ManagementRequest(groupId, null, null, newDescription,
          ManageAction.UPDATE_DESCRIPTION);
    }

    public static ManagementRequest dissolve(String groupId) {
      return new ManagementRequest(groupId, null, null, null, ManageAction.DISSOLVE);
    }

    public String groupId() {
      return groupId;
    }

    public String userId() {
      return userId;
    }

    public String newName() {
      return newName;
    }

    public String newDescription() {
      return newDescription;
    }

    public ManageAction action() {
      return action;
    }
  }

  /**
   * The typed management operation.
   */
  public enum ManageAction {
    ADD_MEMBER,
    REMOVE_MEMBER,
    APPOINT_MANAGER,
    DEMOTE_MANAGER,
    RENAME,
    UPDATE_DESCRIPTION,
    DISSOLVE
  }

  /**
   * Search callback for the "add member" combobox honoring the Vaadin data-provider
   * paging contract (the fetch callback must read the query bounds).
   */
  @FunctionalInterface
  public interface MemberSearch extends Serializable {

    List<UserInfo> search(String filter, int offset, int limit);
  }

  /**
   * Opens the "Manage members" dialog for the given group.
   *
   * @param parent             the owner component (for dialog attachment)
   * @param groupId            the group being managed
   * @param members            the current members of the group
   * @param actingUserRole     the acting user's role inside the group
   * @param memberNameResolver maps a user id to a display name for the roster
   * @param memberSearch       returns users who could be added (not yet members); must honor the
   *                           given offset/limit paging contract
   * @param handler            handles the concrete member op and refreshes
   */
  public static void openManageMembersDialog(Component parent, String groupId,
      List<GroupMember> members, GroupRole actingUserRole,
      Function<String, String> memberNameResolver,
      MemberSearch memberSearch,
      ManagementHandler handler, Supplier<List<GroupMember>> reloadMembers) {
    requireNonNull(parent, "parent must not be null");
    requireNonNull(groupId, "groupId must not be null");
    requireNonNull(members, "members must not be null");
    requireNonNull(handler, "handler must not be null");

    AppDialog dialog = AppDialog.medium();
    DialogHeader.with(dialog, "Manage members");
    var body = new ManageMembersBody(groupId, members, actingUserRole, memberNameResolver,
        memberSearch, handler, reloadMembers);
    dialog.registerUserInput(body);
    DialogBody.with(dialog, body, body);
    DialogFooter.with(dialog, "Close", "Done");
    dialog.registerConfirmAction(dialog::close);
    dialog.registerCancelAction(dialog::close);
    dialog.open();
  }

  /**
   * Opens the "Appoint manager" dialog: lets the owner pick one of the current members
   * (that is not yet a manager/owner) to promote to MANAGER.
   *
   * @param parent             the owner component
   * @param groupId            the group being managed
   * @param members            the current members of the group
   * @param memberNameResolver maps a user id to a display name
   * @param candidateSearcher  (optional) not used for this simple picker
   * @param handler            handles the appointment and refreshes
   */
  public static void openAppointManagerDialog(Component parent, String groupId,
      List<GroupMember> members, Function<String, String> memberNameResolver,
      MemberSearch candidateSearcher, ManagementHandler handler) {
    requireNonNull(parent, "parent must not be null");
    requireNonNull(groupId, "groupId must not be null");
    requireNonNull(members, "members must not be null");
    requireNonNull(handler, "handler must not be null");

    AppDialog dialog = AppDialog.small();
    DialogHeader.with(dialog, "Appoint manager");
    ComboBox<GroupMember> picker = new ComboBox<>("Member");
    picker.setPlaceholder("Select a member");
    List<GroupMember> candidates = members.stream()
        .filter(m -> m.role() == GroupRole.MEMBER)
        .toList();
    picker.setItems(candidates);
    picker.setItemLabelGenerator(member -> {
      String name = memberNameResolver == null ? null : memberNameResolver.apply(member.userId());
      return name == null || name.isBlank() ? member.userId() : name;
    });
    picker.setRequired(true);
    picker.setErrorMessage("Please select a member to appoint as manager.");
    FormLayout layout = new FormLayout(picker);
    layout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
    DialogBody.withoutUserInput(dialog, layout);
    DialogFooter.with(dialog, "Cancel", "Appoint");
    dialog.registerConfirmAction(() -> {
      GroupMember selected = picker.getValue();
      if (selected != null) {
        handler.handle(ManagementRequest.appointManager(groupId, selected.userId()));
        dialog.close();
      } else {
        picker.setInvalid(picker.isEmpty());
      }
    });
    dialog.registerCancelAction(dialog::close);
    dialog.open();
  }

  /**
   * Opens the "Rename group" dialog.
   *
   * @param parent  the owner component
   * @param groupId the group to rename
   * @param oldName the current group name
   * @param handler handles the rename and refreshes
   */
  public static void openRenameDialog(Component parent, String groupId, String oldName,
      ManagementHandler handler) {
    requireNonNull(parent, "parent must not be null");
    requireNonNull(handler, "handler must not be null");
    AppDialog dialog = AppDialog.small();
    DialogHeader.with(dialog, "Rename group");
    TextField nameField = new TextField("Group name");
    nameField.setValue(oldName == null ? "" : oldName);
    nameField.setMaxLength(80);
    nameField.setRequiredIndicatorVisible(true);
    TextFieldInput input = TextFieldInput.wrap(nameField);
    FormLayout layout = new FormLayout(nameField);
    layout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
    DialogBody.with(dialog, layout, input);
    DialogFooter.with(dialog, "Cancel", "Rename");
    dialog.registerConfirmAction(() -> {
      if (input.validate().hasPassed()) {
        handler.handle(ManagementRequest.rename(groupId, nameField.getValue().trim()));
        dialog.close();
      }
    });
    dialog.registerCancelAction(dialog::close);
    dialog.open();
  }

  /**
   * Opens the "Edit description" dialog.
   *
   * @param parent         the owner component
   * @param groupId        the group to change
   * @param oldDescription the current description (may be null)
   * @param handler        handles the description change and refreshes
   */
  public static void openEditDescriptionDialog(Component parent, String groupId,
      String oldDescription, ManagementHandler handler) {
    requireNonNull(parent, "parent must not be null");
    requireNonNull(handler, "handler must not be null");
    AppDialog dialog = AppDialog.small();
    DialogHeader.with(dialog, "Edit description");
    TextArea descriptionField = new TextArea("Description");
    descriptionField.setValue(oldDescription == null ? "" : oldDescription);
    descriptionField.setMaxLength(500);
    descriptionField.setMinHeight("8em");
    TextAreaInput input = TextAreaInput.wrap(descriptionField);
    FormLayout layout = new FormLayout(descriptionField);
    layout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
    DialogBody.with(dialog, layout, input);
    DialogFooter.with(dialog, "Cancel", "Save");
    dialog.registerConfirmAction(() -> {
      if (input.validate().hasPassed()) {
        handler.handle(
            ManagementRequest.updateDescription(groupId, descriptionField.getValue().trim()));
        dialog.close();
      }
    });
    dialog.registerCancelAction(dialog::close);
    dialog.open();
  }

  /**
   * Opens the "Dissolve group" confirmation dialog.
   *
   * @param parent    the owner component
   * @param groupId   the group to dissolve
   * @param groupName the group's display name
   * @param handler   handles the dissolve and refreshes
   */
  public static void openDissolveDialog(Component parent, String groupId, String groupName,
      ManagementHandler handler) {
    requireNonNull(parent, "parent must not be null");
    requireNonNull(handler, "handler must not be null");
    AppDialog dialog = AppDialog.small();
    DialogHeader.with(dialog, "Dissolve group");
    Div message = new Div();
    Span text = new Span(
        "Dissolving the group \"" + (groupName == null ? "" : groupName)
            + "\" removes all members and permanently ends the group. "
            + "Members will lose access to projects this group is shared with. This cannot be undone.");
    text.addClassName("my-groups-dialog-message");
    message.add(text);
    DialogBody.withoutUserInput(dialog, message);
    DialogFooter.withDangerousConfirm(dialog, "Cancel", "Dissolve group");
    dialog.registerConfirmAction(() -> {
      handler.handle(ManagementRequest.dissolve(groupId));
      dialog.close();
    });
    dialog.registerCancelAction(dialog::close);
    dialog.open();
  }

  /**
   * Body of the "Manage members" dialog: a roster of current members with per-member actions plus
   * an "add member" search.
   */
  static class ManageMembersBody extends Div implements UserInput {

    private static final long serialVersionUID = -2012222222222222222L;

    private final String groupId;
    private final List<GroupMember> members;
    private final GroupRole actingUserRole;
    private final Function<String, String> memberNameResolver;
    private final MemberSearch memberSearch;
    private final ManagementHandler handler;
    private final Supplier<List<GroupMember>> reloadMembers;
    private final Div roster = new Div();

    ManageMembersBody(String groupId, List<GroupMember> members, GroupRole actingUserRole,
        Function<String, String> memberNameResolver,
        MemberSearch memberSearch, ManagementHandler handler,
        Supplier<List<GroupMember>> reloadMembers) {
      this.groupId = requireNonNull(groupId, "groupId must not be null");
      this.members = new ArrayList<>(requireNonNull(members, "members must not be null"));
      if (reloadMembers != null) {
        this.members.addAll(reloadMembers.get());
      }
      this.actingUserRole = Objects.requireNonNull(actingUserRole,
          "actingUserRole must not be null");
      this.memberNameResolver = memberNameResolver;
      this.memberSearch = memberSearch;
      this.handler = requireNonNull(handler, "handler must not be null");
      this.reloadMembers = reloadMembers;
      addClassName("manage-members-dialog");
      build();
    }

    private void perform(ManagementRequest request) {
      handler.handle(request);
      if (reloadMembers != null) {
        List<GroupMember> fresh = reloadMembers.get();
        members.clear();
        members.addAll(fresh);
      }
      renderRoster();
    }

    private void build() {
      Span title = new Span("Members");
      title.addClassName("manage-members-title");
      roster.addClassName("manage-members-roster");
      add(title, roster);
      renderRoster();
      if (actingUserRole == GroupRole.OWNER && memberSearch != null) {
        ComboBox<UserInfo> addMember = new ComboBox<>("Add member");
        addMember.setPlaceholder("Search for username or full name");
        addMember.setItemLabelGenerator(UserInfo::platformUserName);
        addMember.setRenderer(new ComponentRenderer<>(candidate -> {
          Div div = new Div();
          div.setText(candidate.fullName() == null || candidate.fullName().isBlank()
              ? candidate.platformUserName()
              : candidate.fullName() + " (" + candidate.platformUserName() + ")");
          return div;
        }));
        addMember.setItems(query -> memberSearch.search(
                query.getFilter().orElse(null), query.getOffset(), query.getLimit())
            .stream());
        Button addButton = new Button("Add");
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.addClickListener(click -> {
          UserInfo selected = addMember.getValue();
          if (selected != null) {
            perform(ManagementRequest.addMember(groupId, selected.id()));
            addMember.clear();
          }
        });
        Div addSection = new Div();
        addSection.addClassName("manage-members-add");
        Div row = new Div();
        row.add(addMember, addButton);
        row.addClassName("manage-members-add-row");
        addSection.add(row);
        add(addSection);
      }
    }

    private void renderRoster() {
      roster.removeAll();
      for (GroupMember member : members) {
        Div memberRow = new Div();
        memberRow.addClassName("manage-members-row");
        String displayName = memberNameResolver == null ? null
            : memberNameResolver.apply(member.userId());
        Span name = new Span(displayName == null || displayName.isBlank()
            ? member.userId() : displayName);
        name.addClassName("manage-members-name");
        Span role = new Span(roleLabel(member.role()));
        role.addClassName("manage-members-role");
        memberRow.add(name, role);
        if (actingUserRole == GroupRole.OWNER && member.role() != GroupRole.OWNER) {
          if (member.role() == GroupRole.MEMBER) {
            Button appoint = new Button("Make manager");
            appoint.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            appoint.addClickListener(click ->
                perform(ManagementRequest.appointManager(groupId, member.userId())));
            memberRow.add(appoint);
          } else if (member.role() == GroupRole.MANAGER) {
            Button demote = new Button("Remove as manager");
            demote.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            demote.addClickListener(click ->
                perform(ManagementRequest.demoteManager(groupId, member.userId())));
            memberRow.add(demote);
          }
          Button remove = new Button("Remove");
          remove.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ERROR);
          remove.addClickListener(click ->
              perform(ManagementRequest.removeMember(groupId, member.userId())));
          memberRow.add(remove);
        }
        roster.add(memberRow);
      }
    }

    private static String roleLabel(GroupRole role) {
      return role == GroupRole.OWNER ? "Owner"
          : role == GroupRole.MANAGER ? "Manager" : "Member";
    }

    @Override
    public InputValidation validate() {
      return InputValidation.passed();
    }

    @Override
    public boolean hasChanges() {
      return false;
    }
  }

  /** Simple {@link UserInput} wrapping a {@link TextField}. */
  private static class TextFieldInput implements UserInput {

    private static final int MAX_LENGTH = 80;

    private final TextField field;

    private TextFieldInput(TextField field) {
      this.field = field;
    }

    static TextFieldInput wrap(TextField field) {
      return new TextFieldInput(field);
    }

    @Override
    public InputValidation validate() {
      String value = field.getValue();
      if (value == null || value.trim().isEmpty()) {
        field.setErrorMessage("A group name is required.");
        field.setInvalid(true);
        return InputValidation.failed();
      }
      if (value.trim().length() > MAX_LENGTH) {
        field.setErrorMessage("Group name must not exceed " + MAX_LENGTH + " characters.");
        field.setInvalid(true);
        return InputValidation.failed();
      }
      field.setInvalid(false);
      return InputValidation.passed();
    }

    @Override
    public boolean hasChanges() {
      return true;
    }
  }

  /** Simple {@link UserInput} wrapping a {@link TextArea}. */
  private static class TextAreaInput implements UserInput {

    private static final int MAX_LENGTH = 500;

    private final TextArea field;

    private TextAreaInput(TextArea field) {
      this.field = field;
    }

    static TextAreaInput wrap(TextArea field) {
      return new TextAreaInput(field);
    }

    @Override
    public InputValidation validate() {
      if (field.getValue() != null && field.getValue().length() > MAX_LENGTH) {
        field.setErrorMessage(
            "Group description must not exceed " + MAX_LENGTH + " characters.");
        field.setInvalid(true);
        return InputValidation.failed();
      }
      field.setInvalid(false);
      return InputValidation.passed();
    }

    @Override
    public boolean hasChanges() {
      return true;
    }
  }
}