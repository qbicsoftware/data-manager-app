package life.qbic.datamanager.views.account;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.identity.api.PersonalAccessToken;
import life.qbic.identity.api.RawToken;
import life.qbic.logging.api.Logger;

/**
 * <class short description - One Line!>
 * <p>
 * <More detailed description - When to use, what it solves, etc.>
 *
 * @since <version tag>
 */

@SpringComponent
@UIScope
@JsModule("./javascript/copytoclipboard.js")
public class PersonalAccessTokenComponent extends PageArea implements Serializable {

  @Serial
  private static final long serialVersionUID = -8972242722349756972L;
  private static final Logger log = logger(PersonalAccessTokenComponent.class);
  private final String TITLE = "Personal Access Token (PAT)";
  private final Div createdTokenLayout = new Div();
  private final VirtualList<PersonalAccessTokenDTO> personalAccessTokens = new VirtualList<>();


  public PersonalAccessTokenComponent() {
    addClassName("personal-access-token-component");
    addComponentAsFirst(generateHeader());
    add(generateDescription());
    Div personalAccessTokenContainer = new Div();
    personalAccessTokenContainer.add(createdTokenLayout, personalAccessTokens);
    add(personalAccessTokenContainer);
    personalAccessTokenContainer.addClassName("personal-access-token-container");
    personalAccessTokens.setRenderer(showEncryptedPersonalAccessTokenRenderer());
    personalAccessTokens.addClassName("personal-access-token-list");
    createdTokenLayout.addClassName("show-created-personal-access-token-layout");
  }

  private ComponentRenderer<Component, PersonalAccessTokenDTO> showEncryptedPersonalAccessTokenRenderer() {
    return new ComponentRenderer<>(personalAccessTokenDTO -> {
      Div showEncryptedPersonalTokenDetails = new Div();
      Span personalAccessToken = new Span(personalAccessTokenDTO.tokenDescription());
      Span expirationDate = new Span(
          "Your token expires on: " + LocalDate.now()
              .plusDays(personalAccessTokenDTO.expirationDate.toDays()).format(
                  DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)));
      expirationDate.setClassName("secondary");
      Icon deletionIcon = VaadinIcon.TRASH.create();
      deletionIcon.addClickListener(event -> fireEvent(new DeleteTokenEvent(this,
          event.isFromClient(), personalAccessTokenDTO.tokenId())));
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

  private void showCreatedPersonalAccessToken(String rawTokenText) {
    createdTokenLayout.removeAll();
    Div createdPersonalAccessTokenDetails = new Div();
    Icon copyIcon = VaadinIcon.COPY_O.create();
    copyIcon.addClassName("clickable");
    copyIcon.addClickListener(
        event -> UI.getCurrent().getPage().executeJs("window.copyToClipboard($0)", rawTokenText));
    Span rawToken = new Span(rawTokenText);
    Span personalAccessTokenWithIcon = new Span(rawToken, copyIcon);
    personalAccessTokenWithIcon.addClassName("token-text");
    Span copyDisclaimer = new Span(VaadinIcon.EXCLAMATION_CIRCLE_O.create(),
        new Text("Please copy your personal access token now. You won't be able to see it again"));
    copyDisclaimer.addClassName("copy-disclaimer");
    copyDisclaimer.addClassName("primary");
    createdPersonalAccessTokenDetails.add(personalAccessTokenWithIcon, copyDisclaimer);
    createdPersonalAccessTokenDetails.addClassName("show-created-personal-access-token-details");
    createdTokenLayout.add(createdPersonalAccessTokenDetails);
    createdTokenLayout.setVisible(true);
  }

  private Span generateHeader() {
    Span title = new Span(TITLE);
    title.addClassName("title");
    Span buttonBar = new Span();
    buttonBar.addClassName("buttons");
    Button generateTokenButton = new Button("Generate new token");
    buttonBar.add(generateTokenButton);
    generateTokenButton.addClickListener(
        event -> fireEvent(new addTokenEvent(this, event.isFromClient())));
    Span header = new Span(title, buttonBar);
    header.addClassName("header");
    return header;
  }

  private Div generateDescription() {
    Anchor downloadGuideLink = new Anchor("https://letmegooglethat.com/?q=never+gonna+give+you+up",
        "Download Guide");
    Text upperDescription = new Text("""
        Personal access tokens allow you to access your own data via the API. They can be used as an alternative over basic authentication (authentication through username and password).""");
    Div description = generateDescriptionText(downloadGuideLink, upperDescription);
    description.addClassName("description");
    return description;
  }

  private Div generateDescriptionText(Anchor downloadGuideLink, Text upperDescription) {
    Text lowerDescriptionText1 = new Text("""
        Tokens that you have generated can be used to access your data via qPostman. You can know more about how you can use your personal access tokens through this \s""");
    Text lowerDescriptionText2 = new Text(""" 
        . Please do not share your personal access tokens with anyone you don't want to access your files.
        """);
    Span lowerDescription = new Span(lowerDescriptionText1, downloadGuideLink,
        lowerDescriptionText2);
    return new Div(upperDescription, lowerDescription);
  }

  /**
   * Sets the provided collection of {@link PersonalAccessTokenDTO} within the
   * {@link PersonalAccessTokenComponent}
   *
   * @param personalAccessTokenDTOs Collection of {@link PersonalAccessTokenDTO} for the logged-in user
   *                             to be displayed within {@link VirtualList} the component
   */
  public void setTokens(Collection<PersonalAccessTokenDTO> personalAccessTokenDTOs) {
    //Sort list so the tokens with the remaining duration of expiration date is on top
    List<PersonalAccessTokenDTO> sortedTokenList = personalAccessTokenDTOs.stream()
        .sorted(Comparator.comparing(PersonalAccessTokenDTO::expirationDate, Duration::compareTo)
            .reversed())
        .toList();
    personalAccessTokens.setItems(sortedTokenList);
    //Each time the component is updated the generated token should not be visible anymore(e.g. deletion, adding another token etc.)
    createdTokenLayout.setVisible(false);
  }

  /**
   * Register an {@link ComponentEventListener} that will get informed with a {@link addTokenEvent},
   * as soon as a user wants to edit batch Information.
   *
   * @param addTokenListener a listener on the batch edit trigger
   */
  public void addTokenListener(ComponentEventListener<addTokenEvent> addTokenListener) {
    addListener(addTokenEvent.class, addTokenListener);
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
  public static class addTokenEvent extends ComponentEvent<PersonalAccessTokenComponent> {

    @Serial
    private static final long serialVersionUID = 2389754662171510873L;

    public addTokenEvent(PersonalAccessTokenComponent source, boolean fromClient) {
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
   * PersonalAccessTokenDTO
   * <p>
   * Class which is used to store and display the frontend relevant properties of the
   * {@link PersonalAccessToken} in the {@link PersonalAccessTokenComponent}
   */
  public static class PersonalAccessTokenDTO {

    private final String tokenId;
    private String tokenDescription;
    private Duration expirationDate;

    public PersonalAccessTokenDTO(String tokenId, String tokenDescription,
        Duration expirationDate) {
      this.tokenId = tokenId;
      this.tokenDescription = tokenDescription;
      this.expirationDate = expirationDate;
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
  }
}
