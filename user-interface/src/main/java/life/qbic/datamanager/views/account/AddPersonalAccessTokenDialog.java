package life.qbic.datamanager.views.account;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import java.io.Serial;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import life.qbic.datamanager.views.account.PersonalAccessTokenComponent.PersonalAccessTokenFrontendBean;
import life.qbic.datamanager.views.general.DialogWindow;
import life.qbic.identity.domain.model.token.PersonalAccessToken;


/**
 * AddPersonalAccessTokenDialog
 *
 * <p>Vaadin dialog component which enables the user to trigger the
 * {@link PersonalAccessToken} generation process with a selectable expiration date
 * </p>
 */
public class AddPersonalAccessTokenDialog extends DialogWindow {

  @Serial
  private static final long serialVersionUID = 6149070385320827888L;
  private final TextField tokenDescription = new TextField();
  private final Select<Duration> expirationDate = new Select<>();
  private final Binder<PersonalAccessTokenFrontendBean> personalAccessTokenDTOBinder;

  public AddPersonalAccessTokenDialog() {
    setHeaderTitle("Generate Personal Access Token");
    setConfirmButtonLabel("Generate");
    tokenDescription.setLabel("Token Description");
    tokenDescription.setPlaceholder("Please enter the description of its usage");
    List<Duration> selectableExpirationDates = computeSelectableExpirationDates();
    expirationDate.setItems(selectableExpirationDates);
    expirationDate.setItemLabelGenerator(
        item -> item.toDays() + " days");
    expirationDate.setLabel("Expiration");
    expirationDate.addValueChangeListener(event -> {
      String formattedDate = LocalDate.now().plusDays(event.getValue().toDays())
          .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL));
      expirationDate.setHelperText("The token will expire " + formattedDate);
    });
    expirationDate.addClassName("expiration-date");
    expirationDate.setValue(selectableExpirationDates.get(0));
    personalAccessTokenDTOBinder = new Binder<>();
    personalAccessTokenDTOBinder.forField(tokenDescription)
        .asRequired("Please provide a token description")
        .bind((PersonalAccessTokenFrontendBean::tokenDescription),
            PersonalAccessTokenFrontendBean::setTokenDescription);
    personalAccessTokenDTOBinder.forField(expirationDate)
        .asRequired("Please provide a valid expiration date")
        .bind(PersonalAccessTokenFrontendBean::expirationDate,
            PersonalAccessTokenFrontendBean::setExpirationDate);
    add(tokenDescription);
    add(expirationDate);
    addClassName("add-personal-access-token-dialog");
  }

  private List<Duration> computeSelectableExpirationDates() {
    List<Long> daysToAdd = new ArrayList<>(List.of(30L, 60L, 180L));
    List<Duration> expirationDates = new ArrayList<>();
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
    PersonalAccessTokenFrontendBean personalAccessTokenFrontendBean = new PersonalAccessTokenFrontendBean(
        "My new Id", tokenDescription.getValue(), expirationDate.getValue(), false);
    fireEvent(new ConfirmEvent(this, true, personalAccessTokenFrontendBean));
  }

  private Duration computeExpirationDate(Long addedDays) {
    return Duration.ofDays(addedDays);
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

    public PersonalAccessTokenFrontendBean personalAccessTokenDTO() {
      return personalAccessTokenFrontendBean;
    }

    private final PersonalAccessTokenFrontendBean personalAccessTokenFrontendBean;


    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public ConfirmEvent(AddPersonalAccessTokenDialog source, boolean fromClient,
        PersonalAccessTokenFrontendBean personalAccessTokenFrontendBean) {
      super(source, fromClient);
      this.personalAccessTokenFrontendBean = personalAccessTokenFrontendBean;
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
