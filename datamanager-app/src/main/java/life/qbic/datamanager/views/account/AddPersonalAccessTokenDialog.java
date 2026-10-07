package life.qbic.datamanager.views.account;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import java.io.Serial;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import life.qbic.datamanager.views.account.PersonalAccessTokenComponent.PersonalAccessTokenFrontendBean;
import life.qbic.datamanager.views.general.dialog.AppDialog;
import life.qbic.datamanager.views.general.dialog.DialogBody;
import life.qbic.datamanager.views.general.dialog.DialogFooter;
import life.qbic.datamanager.views.general.dialog.DialogHeader;
import life.qbic.datamanager.views.general.dialog.InputValidation;
import life.qbic.datamanager.views.general.dialog.UserInput;
import life.qbic.identity.domain.model.token.PersonalAccessToken;

/**
 * Add Personal Access Token Dialog
 *
 * <p>Dialog for generating a new {@link PersonalAccessToken} with a description
 * and selectable expiration date. Uses the modern {@link AppDialog} pattern
 * with {@link UserInput} validation.</p>
 */
public class AddPersonalAccessTokenDialog extends Div {

  private final AppDialog dialog;
  private final TextField tokenDescription = new TextField();
  private final Select<Duration> expirationDate = new Select<>();
  private final TokenFormInput formInput;
  private final List<ComponentEventListener<ConfirmEvent>> confirmListeners = new ArrayList<>();
  private final List<ComponentEventListener<CancelEvent>> cancelListeners = new ArrayList<>();

  public AddPersonalAccessTokenDialog() {
    dialog = AppDialog.medium();
    DialogHeader.with(dialog, "Generate Personal Access Token");

    // Build form
    var form = new Div();
    form.addClassName("pat-form");

    tokenDescription.setLabel("Token description");
    tokenDescription.setPlaceholder("e.g. CI pipeline, local scripts");
    tokenDescription.setWidthFull();
    tokenDescription.setRequired(true);

    List<Duration> selectableExpirationDates = computeSelectableExpirationDates();
    expirationDate.setItems(selectableExpirationDates);
    expirationDate.setItemLabelGenerator(this::formatDurationLabel);
    expirationDate.setLabel("Expiration");
    expirationDate.setWidthFull();
    expirationDate.setValue(selectableExpirationDates.get(0));
    expirationDate.addValueChangeListener(event -> {
      String formattedDate = LocalDate.now().plusDays(event.getValue().toDays())
          .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL));
      expirationDate.setHelperText("The token will expire on " + formattedDate);
    });
    // Trigger initial helper text
    expirationDate.setHelperText("The token will expire on "
        + LocalDate.now().plusDays(selectableExpirationDates.get(0).toDays())
        .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)));

    form.add(tokenDescription, expirationDate);

    formInput = new TokenFormInput(tokenDescription, expirationDate);
    DialogBody.with(dialog, form, formInput);
    DialogFooter.with(dialog, "Cancel", "Generate");

    // Wire confirm action: build the bean and fire all registered listeners
    dialog.registerConfirmAction(() -> {
      PersonalAccessTokenFrontendBean bean = new PersonalAccessTokenFrontendBean(
          "pending",
          tokenDescription.getValue(),
          Instant.now(),
          Instant.now().plus(expirationDate.getValue()),
          false);
      var event = new ConfirmEvent(this, true, bean);
      confirmListeners.forEach(l -> l.onComponentEvent(event));
    });

    // Wire cancel action: fire all registered listeners then close
    dialog.registerCancelAction(() -> {
      var event = new CancelEvent(this, true);
      cancelListeners.forEach(l -> l.onComponentEvent(event));
      dialog.close();
    });
  }

  private String formatDurationLabel(Duration duration) {
    long days = duration.toDays();
    if (days >= 365) {
      long years = days / 365;
      return years + " year" + (years > 1 ? "s" : "");
    }
    return days + " days";
  }

  private List<Duration> computeSelectableExpirationDates() {
    List<Long> daysToAdd = new ArrayList<>(List.of(30L, 60L, 90L, 180L, 365L));
    List<Duration> expirationDates = new ArrayList<>();
    daysToAdd.forEach(day -> expirationDates.add(Duration.ofDays(day)));
    return expirationDates;
  }

  public void open() {
    dialog.open();
  }

  public void close() {
    dialog.close();
  }

  public void addCancelListener(ComponentEventListener<CancelEvent> listener) {
    cancelListeners.add(listener);
  }

  public void addConfirmListener(ComponentEventListener<ConfirmEvent> listener) {
    confirmListeners.add(listener);
  }

  /**
   * {@link UserInput} implementation that validates the token form fields.
   */
  private static class TokenFormInput implements UserInput {

    private final TextField descriptionField;
    private final Select<Duration> expirationSelect;

    TokenFormInput(TextField descriptionField, Select<Duration> expirationSelect) {
      this.descriptionField = descriptionField;
      this.expirationSelect = expirationSelect;
    }

    @Override
    public InputValidation validate() {
      boolean descriptionValid = descriptionField.getValue() != null
          && !descriptionField.getValue().isBlank();
      boolean expirationValid = expirationSelect.getValue() != null;

      if (!descriptionValid) {
        descriptionField.setErrorMessage("Please provide a token description");
        descriptionField.setInvalid(true);
      } else {
        descriptionField.setInvalid(false);
      }

      if (!expirationValid) {
        expirationSelect.setErrorMessage("Please select an expiration period");
        expirationSelect.setInvalid(true);
      } else {
        expirationSelect.setInvalid(false);
      }

      return (descriptionValid && expirationValid)
          ? InputValidation.passed()
          : InputValidation.failed();
    }

    @Override
    public boolean hasChanges() {
      return descriptionField.getValue() != null && !descriptionField.getValue().isBlank();
    }
  }

  // ── Events ─────────────────────────────────────────────────────

  public static class ConfirmEvent extends ComponentEvent<AddPersonalAccessTokenDialog> {

    @Serial
    private static final long serialVersionUID = 1L;
    private final PersonalAccessTokenFrontendBean bean;

    public ConfirmEvent(AddPersonalAccessTokenDialog source, boolean fromClient,
        PersonalAccessTokenFrontendBean bean) {
      super(source, fromClient);
      this.bean = bean;
    }

    public PersonalAccessTokenFrontendBean personalAccessTokenDTO() {
      return bean;
    }
  }

  public static class CancelEvent extends ComponentEvent<AddPersonalAccessTokenDialog> {

    @Serial
    private static final long serialVersionUID = 1L;

    public CancelEvent(AddPersonalAccessTokenDialog source, boolean fromClient) {
      super(source, fromClient);
    }
  }
}
