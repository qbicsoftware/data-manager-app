package life.qbic.datamanager.views.account;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import java.io.Serial;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import life.qbic.datamanager.views.account.PersonalAccessTokenComponent.PersonalAccessTokenDTO;
import life.qbic.datamanager.views.general.DialogWindow;
import life.qbic.logging.api.Logger;

/**
 * <class short description - One Line!>
 * <p>
 * <More detailed description - When to use, what it solves, etc.>
 *
 * @since <version tag>
 */
public class AddPersonalAccessTokenDialog extends DialogWindow {

  private static final Logger log = logger(AddPersonalAccessTokenDialog.class);
  @Serial
  private static final long serialVersionUID = 6149070385320827888L;
  private final TextField tokenDescription = new TextField();
  private final Select<LocalDate> expirationDate = new Select<>();
  private final Binder<PersonalAccessTokenDTO> personalAccessTokenDTOBinder;

  public AddPersonalAccessTokenDialog() {
    setHeaderTitle("Generate Personal Access Token");
    setConfirmButtonLabel("Generate");
    tokenDescription.setLabel("Token Description");
    tokenDescription.setPlaceholder("Please enter the description of its usage");
    expirationDate.setItems(computeSelectableExpirationDates());
    expirationDate.setItemLabelGenerator(
        item -> LocalDate.now().until(item, ChronoUnit.DAYS) + " days");
    expirationDate.setLabel("Expiration");
    expirationDate.addValueChangeListener(event -> {
      String formattedDate = event.getValue()
          .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL));
      expirationDate.setHelperText("The token will expire " + formattedDate);
    });
    expirationDate.addClassName("expiration-date");
    personalAccessTokenDTOBinder = new Binder<>(PersonalAccessTokenDTO.class);
    personalAccessTokenDTOBinder.forField(tokenDescription)
        .asRequired("Please provide a token description")
        .bind((PersonalAccessTokenDTO::tokenDescription),
            PersonalAccessTokenDTO::setTokenDescription);
    personalAccessTokenDTOBinder.forField(expirationDate)
        .asRequired("Please provide a valid expiration date")
        .bind(PersonalAccessTokenDTO::expirationDate,
            PersonalAccessTokenDTO::setExpirationDate);
    add(tokenDescription);
    add(expirationDate);
    addClassName("add-personal-access-token-dialog");
  }

  private List<LocalDate> computeSelectableExpirationDates() {
    List<Long> daysToAdd = new ArrayList<>(List.of(30L, 60L, 180L));
    List<LocalDate> expirationDates = new ArrayList<>();
    daysToAdd.forEach(day -> expirationDates.add(computeExpirationDate(day)));
    return expirationDates;
  }

  /**
   * Overwrite to change what happens on confirm button clicked
   *
   * @param clickEvent
   */
  @Override
  protected void onConfirmClicked(ClickEvent<Button> clickEvent) {
    if (!personalAccessTokenDTOBinder.isValid()) {
      personalAccessTokenDTOBinder.validate();
      return;
    }
    PersonalAccessTokenDTO personalAccessTokenDTO = new PersonalAccessTokenDTO(
        tokenDescription.getValue(), expirationDate.getValue());
    fireEvent(new ConfirmEvent(this, true, personalAccessTokenDTO));
  }

  private LocalDate computeExpirationDate(Long addedDays) {
    return LocalDate.now().plusDays(addedDays);
  }

  /**
   * Overwrite to change what happens on cancel button clicked.
   *
   * @param clickEvent
   */
  @Override
  protected void onCancelClicked(ClickEvent<Button> clickEvent) {
    fireEvent(new CancelEvent(this, clickEvent.isFromClient()));
  }

  public void addCancelListener(
      ComponentEventListener<CancelEvent> listener) {
    addListener(CancelEvent.class, listener);
  }

  public void addConfirmListener(ComponentEventListener<ConfirmEvent> listener) {
    addListener(ConfirmEvent.class, listener);
  }

  public static class ConfirmEvent extends ComponentEvent<AddPersonalAccessTokenDialog> {

    private final PersonalAccessTokenDTO personalAccessTokenDTO;


    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public ConfirmEvent(AddPersonalAccessTokenDialog source, boolean fromClient,
        PersonalAccessTokenDTO personalAccessTokenDTO) {
      super(source, fromClient);
      this.personalAccessTokenDTO = personalAccessTokenDTO;
    }

    public PersonalAccessTokenDTO personalAccessTokenDTO() {
      return personalAccessTokenDTO;
    }
  }

  public static class CancelEvent extends ComponentEvent<AddPersonalAccessTokenDialog> {

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public CancelEvent(AddPersonalAccessTokenDialog source, boolean fromClient) {
      super(source, fromClient);
    }
  }
}
