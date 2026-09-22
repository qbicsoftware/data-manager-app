package life.qbic.datamanager.views.settings;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.shared.Registration;
import jakarta.annotation.Nullable;
import java.io.Serial;
import java.util.function.Function;

/**
 * <b>New group form</b>
 * <p>
 * Renders the creation form for a new ad-hoc group: a required name field (max 80 characters)
 * and an optional description field (max 500 characters). Validation mirrors the domain value
 * objects ({@code GroupName}, {@code GroupDescription}) and is performed client-side before any
 * service call.
 * <p>
 * The component deliberately does <em>not</em> navigate or talk to services: data entry is
 * exposed through a {@link CreateEvent} and a cancel event, so it can be tested without a
 * Vaadin {@code UI} or Spring context.
 *
 * @since 1.19.0
 */
public class NewGroupForm extends Div {

  @Serial
  private static final long serialVersionUID = 4319256744530793490L;

  private static final int NAME_MAX_LENGTH = 80;
  private static final int DESCRIPTION_MAX_LENGTH = 500;

  private final TextField nameField = new TextField("Name");
  private final TextArea descriptionField = new TextArea("Description");
  private final Button createButton = new Button("Create");
  private final Button cancelButton = new Button("Cancel");
  private final Function<String, Boolean> nameAvailabilityCheck;

  /**
   * Creates a new group creation form.
   *
   * @param nameAvailabilityCheck optional case-insensitive name availability check invoked on
   *                              name blur; may be {@code null} to disable the live hint
   */
  public NewGroupForm(@Nullable Function<String, Boolean> nameAvailabilityCheck) {
    this.nameAvailabilityCheck = nameAvailabilityCheck;
    addClassName("new-group-form");

    nameField.setRequiredIndicatorVisible(true);
    nameField.setMaxLength(NAME_MAX_LENGTH);
    nameField.setPlaceholder("e.g. Acknowledgements Working Group");

    descriptionField.setMaxLength(DESCRIPTION_MAX_LENGTH);
    descriptionField.setPlaceholder("What is this group about? (optional)");
    descriptionField.setMinHeight("8em");

    FormLayout form = new FormLayout(nameField, descriptionField);
    form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
    Div fields = new Div(form);
    fields.addClassName("new-group-form__fields");
    add(fields);

    Div actions = new Div();
    actions.addClassName("new-group-form__actions");
    createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    createButton.addClickListener(event -> validateAndSubmit());
    cancelButton.addClickListener(event -> fireEvent(new CancelEvent(this)));
    actions.add(cancelButton, createButton);
    add(actions);

    if (nameAvailabilityCheck != null) {
      nameField.addBlurListener(event -> checkNameAvailability(nameAvailabilityCheck));
    }
  }

  private void checkNameAvailability(Function<String, Boolean> availabilityCheck) {
    if (nameField.getValue().isEmpty()) {
      nameField.setHelperText(null);
      return;
    }
    boolean available = Boolean.TRUE.equals(availabilityCheck.apply(nameField.getValue()));
    nameField.setHelperText(available
        ? "Group name is available."
        : "This name is already taken. Group names must be unique (case-insensitive).");
  }

  /**
   * Package-private trigger for the live name-availability hint; exposed so unit tests can
   * invoke the check without a Vaadin {@code UI} (blur listeners do not fire in component
   * tests). The production wiring calls this on name-field blur.
   */
  final void runNameAvailabilityCheck() {
    if (nameAvailabilityCheck != null) {
      checkNameAvailability(nameAvailabilityCheck);
    }
  }

  private void validateAndSubmit() {
    String name = nameField.getValue().trim();
    String description = descriptionField.getValue().trim();

    boolean valid = true;
    if (name.isEmpty()) {
      nameField.setErrorMessage("A name is required.");
      nameField.setInvalid(true);
      valid = false;
    } else if (name.length() > NAME_MAX_LENGTH) {
      nameField.setErrorMessage("Group name must not exceed " + NAME_MAX_LENGTH + " characters.");
      nameField.setInvalid(true);
      valid = false;
    } else {
      nameField.setInvalid(false);
    }

    if (description.length() > DESCRIPTION_MAX_LENGTH) {
      descriptionField.setErrorMessage(
          "Group description must not exceed " + DESCRIPTION_MAX_LENGTH + " characters.");
      descriptionField.setInvalid(true);
      valid = false;
    } else {
      descriptionField.setInvalid(false);
    }

    if (valid) {
      fireEvent(new CreateEvent(this, name, description.isEmpty() ? null : description));
    }
  }

  /**
   * Programmatically fills the form with the given values; used by tests and pre-fills.
   */
  public void setValues(String name, String description) {
    nameField.setValue(name == null ? "" : name);
    descriptionField.setValue(description == null ? "" : description);
  }

  /**
   * Returns the currently entered name value.
   *
   * @return the (untrimmed) name field value
   */
  public String name() {
    return nameField.getValue();
  }

  /**
   * Returns the currently entered description value.
   *
   * @return the (untrimmed) description field value
   */
  public String description() {
    return descriptionField.getValue();
  }

  /**
   * Sets an inline error on the name field (e.g. a server-side uniqueness rejection).
   *
   * @param message the error message to show
   */
  public void markNameError(String message) {
    nameField.setErrorMessage(message);
    nameField.setInvalid(true);
  }

  /**
   * Clears the name error state.
   */
  public void clearNameError() {
    nameField.setInvalid(false);
    nameField.setHelperText(null);
  }

  /**
   * Registers a listener invoked when the create button is clicked with valid input.
   *
   * @param listener the CreateEvent listener
   * @return the registration to remove the listener
   */
  public Registration addCreateListener(ComponentEventListener<CreateEvent> listener) {
    return addListener(CreateEvent.class, listener);
  }

  /**
   * Registers a listener invoked when the cancel button is clicked.
   *
   * @param listener the CancelEvent listener
   * @return the registration to remove the listener
   */
  public Registration addCancelListener(ComponentEventListener<CancelEvent> listener) {
    return addListener(CancelEvent.class, listener);
  }

  /**
   * Event fired with the validated group draft when the user submits the form.
   */
  public static class CreateEvent extends ComponentEvent<NewGroupForm> {

    private final String groupName;
    private final String groupDescription;

    public CreateEvent(NewGroupForm source, String groupName, @Nullable String groupDescription) {
      super(source, false);
      this.groupName = groupName;
      this.groupDescription = groupDescription;
    }

    public String groupName() {
      return groupName;
    }

    @Nullable
    public String groupDescription() {
      return groupDescription;
    }
  }

  /**
   * Event fired when the user cancels creation.
   */
  public static class CancelEvent extends ComponentEvent<NewGroupForm> {

    public CancelEvent(NewGroupForm source) {
      super(source, false);
    }
  }
}