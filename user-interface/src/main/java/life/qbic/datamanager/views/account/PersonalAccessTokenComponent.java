package life.qbic.datamanager.views.account;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoUtility.IconSize;
import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import life.qbic.datamanager.views.general.CopyToClipBoardComponent;
import life.qbic.datamanager.views.general.Disclaimer;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.identity.api.PersonalAccessToken;
import life.qbic.identity.api.RawToken;

/**
 * Personal Access Token Component
 * <p>
 * This {@link PageArea} allows the user to manage his personal access tokens. The user is able to
 * view a tokens expiration date and description. Additionally,he is able to delete and create
 * personal access tokens. Only after a personal access token is created its raw text is shown to
 * the user with the ability to copy it to the clipboard
 */

@SpringComponent
@UIScope
public class PersonalAccessTokenComponent extends PageArea implements Serializable {

  @Serial
  private static final long serialVersionUID = -8972242722349756972L;
  private static final String TITLE = "Personal Access Token (PAT)";
  private final Disclaimer noTokensRegisteredDisclaimer;
  private final Div createdTokenLayout = new Div();
  private final VirtualList<PersonalAccessTokenFrontendBean> personalAccessTokens = new VirtualList<>();


  public PersonalAccessTokenComponent() {
    addClassName("personal-access-token-component");
    addComponentAsFirst(generateHeader());
    add(generateDescription());
    Div personalAccessTokenContainer = new Div();
    noTokensRegisteredDisclaimer = createNoTokensRegisteredDisclaimer();
    add(noTokensRegisteredDisclaimer);
    personalAccessTokenContainer.add(createdTokenLayout, personalAccessTokens);
    add(personalAccessTokenContainer);
    createNoTokensRegisteredDisclaimer();
    personalAccessTokenContainer.addClassName("personal-access-token-container");
    personalAccessTokens.setRenderer(showEncryptedPersonalAccessTokenRenderer());
    personalAccessTokens.addClassName("personal-access-token-list");
    createdTokenLayout.addClassName("show-created-personal-access-token-layout");
    updateUI();
  }

  private ComponentRenderer<Component, PersonalAccessTokenFrontendBean> showEncryptedPersonalAccessTokenRenderer() {
    return new ComponentRenderer<>(personalAccessTokenFrontendBean -> {
      Div showEncryptedPersonalTokenDetails = new Div();
      Span personalAccessToken = new Span(personalAccessTokenFrontendBean.tokenDescription());
      Span expirationDate = createExpirationDate(personalAccessTokenFrontendBean);
      Icon deletionIcon = VaadinIcon.TRASH.create();
      deletionIcon.addClickListener(event -> fireEvent(new DeleteTokenEvent(this,
          event.isFromClient(), personalAccessTokenFrontendBean.tokenId())));
      deletionIcon.addClassNames("error", "clickable");
      showEncryptedPersonalTokenDetails.addClassName(
          "show-encrypted-personal-access-token-details");
      showEncryptedPersonalTokenDetails.add(personalAccessToken, expirationDate);
      Div showEncryptedPersonalTokenLayout = new Div(showEncryptedPersonalTokenDetails,
          deletionIcon);
      showEncryptedPersonalTokenLayout.addClassName("show-encrypted-personal-access-token-layout");
      return showEncryptedPersonalTokenLayout;
    });
  }

  private Span createExpirationDate(
      PersonalAccessTokenFrontendBean personalAccessTokenFrontendBean) {
    Span expirationDate = new Span();
    expirationDate.addClassName("expiration-date");
    String expirationDateText = LocalDate.now()
        .plusDays(personalAccessTokenFrontendBean.expirationDate.toDays()).format(
            DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG));
    if (personalAccessTokenFrontendBean.expired()) {
      Icon warningIcon = VaadinIcon.EXCLAMATION_CIRCLE_O.create();
      warningIcon.addClassName(IconSize.SMALL);
      expirationDate.add(warningIcon);
      expirationDate.add("Your token has expired on: " + expirationDateText);
      expirationDate.addClassName("warning");
    } else {
      expirationDate.setText(
          "Your token expires on: " + expirationDateText);
      expirationDate.addClassName("secondary");
    }
    return expirationDate;
  }

  private void showCreatedPersonalAccessToken(String rawTokenText) {
    createdTokenLayout.removeAll();
    CreatedPersonalAccessTokenDetails createdPersonalAccessTokenDetails = new CreatedPersonalAccessTokenDetails();
    createdPersonalAccessTokenDetails.setToken(rawTokenText);
    createdTokenLayout.add(createdPersonalAccessTokenDetails);
    updateUI();
  }

  private Span generateHeader() {
    Span title = new Span(TITLE);
    title.addClassName("title");
    Span buttonBar = new Span();
    buttonBar.addClassName("buttons");
    Button generateTokenButton = new Button("Generate new token");
    buttonBar.add(generateTokenButton);
    generateTokenButton.addClickListener(
        event -> fireEvent(new AddTokenEvent(this, event.isFromClient())));
    Span header = new Span(title, buttonBar);
    header.addClassName("header");
    return header;
  }

  private Div generateDescription() {
    Anchor downloadGuideLink = new Anchor(
        "https://qbicsoftware.github.io/research-data-management/rawdata/raw_data_download/#personal-access-token",
        "Download Guide", AnchorTarget.BLANK);
    Text upperDescription = new Text(
        "Personal access tokens allow you to access your own data via the API. They can be used as an alternative over basic authentication (authentication through username and password).");
    Div description = generateDescriptionText(downloadGuideLink, upperDescription);
    description.addClassName("description");
    return description;
  }

  private Div generateDescriptionText(Anchor downloadGuideLink, Text upperDescription) {
    Text lowerDescriptionText1 = new Text(
        "Tokens that you have generated can be used to access your data via qPostman. You can learn more about how you can use your personal access tokens through this ");
    Text lowerDescriptionText2 = new Text(
        ". Please do not share your personal access tokens with anyone you don't want to access your files.");
    Span lowerDescription = new Span(lowerDescriptionText1, downloadGuideLink,
        lowerDescriptionText2);
    return new Div(upperDescription, lowerDescription);
  }

  private Disclaimer createNoTokensRegisteredDisclaimer() {
    Disclaimer noTokensRegisteredCard = Disclaimer.createWithTitle(
        "Manage your tokens in one place",
        "Manage data access by registering the first personal access token", "Generate new token");
    noTokensRegisteredCard.addDisclaimerConfirmedListener(
        event -> fireEvent(new AddTokenEvent(this, event.isFromClient())));
    return noTokensRegisteredCard;
  }

  /**
   * Sets the provided collection of {@link PersonalAccessTokenFrontendBean} within the
   * {@link PersonalAccessTokenComponent}
   *
   * @param personalAccessTokenFrontendBeans Collection of {@link PersonalAccessTokenFrontendBean}
   *                                         for the logged-in user to be displayed within
   *                                         {@link VirtualList} the component
   */
  public void setTokens(
      Collection<PersonalAccessTokenFrontendBean> personalAccessTokenFrontendBeans) {
    //Each time the component is updated the generated token should not be visible anymore(e.g. deletion, adding another token etc.)
    createdTokenLayout.removeAll();
    //Sort list so the tokens with the remaining duration of expiration date is on top
    List<PersonalAccessTokenFrontendBean> sortedTokenList = personalAccessTokenFrontendBeans.stream()
        .sorted(Comparator.comparing(PersonalAccessTokenFrontendBean::expirationDate,
                Duration::compareTo)
            .reversed())
        .toList();
    personalAccessTokens.setItems(sortedTokenList);
    updateUI();
  }

  private void updateUI() {
    boolean userHasTokens = !personalAccessTokens.getDataProvider().fetch(new Query<>()).toList()
        .isEmpty();
    boolean userCreatedToken = createdTokenLayout.getChildren().findAny().isPresent();
    personalAccessTokens.setVisible(userHasTokens);
    createdTokenLayout.setVisible(userCreatedToken);
    //Show Disclaimer only if no token was generated and user has no tokens currently
    noTokensRegisteredDisclaimer.setVisible(!userHasTokens && !userCreatedToken);
  }

  /**
   * Sets the provided {@link RawToken} within the {@link PersonalAccessTokenComponent}
   * <p>
   * This method is used to show a newly generated Token to the user on Top of the
   * {@link VirtualList} within the component
   *
   * @param rawToken The {@link RawToken} to be displayed to the user
   */
  public void showCreatedToken(RawToken rawToken) {
    showCreatedPersonalAccessToken(rawToken.value());
  }

  /**
   * Register an {@link ComponentEventListener} that will get informed with a {@link AddTokenEvent},
   * as soon as a user wants to edit batch Information.
   *
   * @param addTokenListener a listener on the batch edit trigger
   */
  public void addTokenListener(ComponentEventListener<AddTokenEvent> addTokenListener) {
    addListener(AddTokenEvent.class, addTokenListener);
  }

  /**
   * Register an {@link ComponentEventListener} that will get informed with a
   * {@link DeleteTokenEvent}, as soon as a user wants to edit batch Information.
   *
   * @param deleteTokenListener a listener on the batch edit trigger
   */
  public void addDeleteTokenListener(ComponentEventListener<DeleteTokenEvent> deleteTokenListener) {
    addListener(DeleteTokenEvent.class, deleteTokenListener);
  }

  /**
   * <b>Generate Personal Access Token Clicked</b>
   *
   * <p>Indicates that a user wants to create a {@link PersonalAccessToken}
   * within the {@link PersonalAccessTokenComponent}</p>
   */
  public static class AddTokenEvent extends ComponentEvent<PersonalAccessTokenComponent> {

    @Serial
    private static final long serialVersionUID = 2389754662171510873L;

    public AddTokenEvent(PersonalAccessTokenComponent source, boolean fromClient) {
      super(source, fromClient);
    }
  }

  /**
   * <b>Delete Token Clicked</b>
   *
   * <p>Indicates that a user wants to delete a {@link PersonalAccessToken}
   * within the {@link PersonalAccessTokenComponent}</p>
   */
  public static class DeleteTokenEvent extends ComponentEvent<PersonalAccessTokenComponent> {

    @Serial
    private static final long serialVersionUID = 5303581981248150518L;
    private final String tokenId;

    public DeleteTokenEvent(PersonalAccessTokenComponent source, boolean fromClient,
        String tokenId) {
      super(source, fromClient);
      this.tokenId = tokenId;
    }

    public String tokenId() {
      return tokenId;
    }
  }

  /**
   * PersonalAccessTokenFrontendBean
   * <p>
   * Class which is used to store and display the frontend relevant properties of the
   * {@link PersonalAccessToken} in the {@link PersonalAccessTokenComponent}
   */
  public static class PersonalAccessTokenFrontendBean {

    private final String tokenId;
    private final boolean hasExpired;
    private String tokenDescription;
    private Duration expirationDate;

    public PersonalAccessTokenFrontendBean(String tokenId, String tokenDescription,
        Duration expirationDate, boolean hasExpired) {
      this.tokenId = tokenId;
      this.tokenDescription = tokenDescription;
      this.expirationDate = expirationDate;
      this.hasExpired = hasExpired;
    }

    public static PersonalAccessTokenFrontendBean from(PersonalAccessToken personalAccessToken) {
      return new PersonalAccessTokenFrontendBean(personalAccessToken.tokenId(),
          personalAccessToken.description(), Duration.between(
          Instant.now(), personalAccessToken.expiration()),
          personalAccessToken.expired());
    }

    public String tokenId() {
      return tokenId;
    }

    public String tokenDescription() {
      return tokenDescription;
    }

    public void setTokenDescription(String tokenDescription) {
      this.tokenDescription = tokenDescription;
    }

    public Duration expirationDate() {
      return expirationDate;
    }

    public void setExpirationDate(Duration expirationDate) {
      this.expirationDate = expirationDate;
    }

    public boolean expired() {
      return hasExpired;
    }
  }


  /**
   * Created Personal Access Token Details
   * <p>
   * Component based on {@link Div} showing the created Personal access token, providing the
   * possibility to copy the new token via the {@link CopyToClipBoardComponent}
   */
  private static class CreatedPersonalAccessTokenDetails extends Div {

    private final Span rawToken = new Span();
    private final Span copyDisclaimerText = new Span();
    private final CopyToClipBoardComponent copyToClipBoardComponent = new CopyToClipBoardComponent();

    public CreatedPersonalAccessTokenDetails() {

      Span personalAccessTokenWithIcon = new Span(rawToken, copyToClipBoardComponent);
      personalAccessTokenWithIcon.addClassName("token-text");
      Icon disclaimerIcon = VaadinIcon.EXCLAMATION_CIRCLE_O.create();
      disclaimerIcon.addClassName(IconSize.SMALL);
      copyDisclaimerText.setText(
          "Please copy your personal access token now. You won't be able to see it again");
      Span copyDisclaimer = new Span(disclaimerIcon, copyDisclaimerText);
      copyDisclaimer.addClassName("copy-disclaimer");
      copyDisclaimer.addClassName("primary");
      add(personalAccessTokenWithIcon, copyDisclaimer);
      addClassName("show-created-personal-access-token-details");
    }

    public void setToken(String token) {
      rawToken.setText(token);
      UI ui = UI.getCurrent();
      ui.getPushConfiguration().setPushMode(PushMode.MANUAL);
      copyToClipBoardComponent.setCopyText(token);
      copyToClipBoardComponent.addSwitchToSuccessfulCopyIconListener(event ->
          ui.access(() -> {
            addClassName("success-background-hue");
            copyDisclaimerText.setText("Token successfully copied.");
          }));
      copyToClipBoardComponent.addSwitchToCopyIconListener(event ->
          ui.access(() -> {
            removeClassName("success-background-hue");
          }));
    }
  }
}
