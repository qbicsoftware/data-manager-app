package life.qbic.datamanager.views.account;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
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
import java.util.List;
import life.qbic.datamanager.views.general.PageArea;
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
public class PersonalAccessTokenComponent extends PageArea implements Serializable {

  @Serial
  private static final long serialVersionUID = -8972242722349756972L;
  private static final Logger log = logger(PersonalAccessTokenComponent.class);
  private final String TITLE = "Personal Access Token (PAT)";
  private final VirtualList<PersonalAccessTokenDTO> personalAccessTokenDTOVirtualList = new VirtualList<>();

  public PersonalAccessTokenComponent() {
    addClassName("personal-access-token-component");
    addComponentAsFirst(generateHeader());
    add(generateDescription());
    //ToDo figure out how to best extract this list
    personalAccessTokenDTOVirtualList.setRenderer(showEncryptedPersonalAccessTokenRenderer());
    add(personalAccessTokenDTOVirtualList);
    personalAccessTokenDTOVirtualList.addClassName("personal-access-token-list");
  }


  //Todo define custom component and add css
  private ComponentRenderer<Component, PersonalAccessTokenDTO> showEncryptedPersonalAccessTokenRenderer() {
    return new ComponentRenderer<>(personalAccessTokenDTO -> {
      Div showEncryptedPersonalTokenDetails = new Div();
      Span personalAccessToken = new Span(personalAccessTokenDTO.tokenDescription());
      Span expirationDate = new Span(
          "Your token expires on: " + personalAccessTokenDTO.expirationDate);
      expirationDate.setClassName("secondary");
      Icon deletionIcon = VaadinIcon.TRASH.create();
      deletionIcon.addClickListener(event -> fireEvent(new DeleteTokenClicked(this,
          event.isFromClient())));
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

  private ComponentRenderer<Component, PersonalAccessToken> showCreatedPersonalAccessTokenRenderer() {
    return new ComponentRenderer<>(personalAccessToken -> {
      Div createPersonalAccessTokenDetails = new Div();
      Icon copyIcon = VaadinIcon.COPY.create();
      Span personalAccessTokenWithIcon = new Span(copyIcon);
      //ToDo figure out how to copy text to clipboard in vaadin
      /*
      copyIcon.addClickListener(event -> UI.getCurrent().getPage()
          .executeJs("navigator.clipboard.writeText", personalAccessTokenDTO.tokenName()));
       */
      Icon deletionIcon = VaadinIcon.TRASH.create();
      deletionIcon.addClickListener(event -> fireEvent(new DeleteTokenClicked(this,
          event.isFromClient())));
      deletionIcon.addClassName("error");
      Span copyNotification = new Span(
          "Please copy your personal access token now. You won't be able to see it again");
      copyNotification.addClassName("primary");
      createPersonalAccessTokenDetails.add(personalAccessTokenWithIcon, copyNotification);
      Div createPersonalAccessTokenLayout = new Div(createPersonalAccessTokenDetails, deletionIcon);
      createPersonalAccessTokenLayout.addClassName("create-personal-access-token-layout");
      return createPersonalAccessTokenLayout;
    });
  }

  //ToDo move logic to create token

  private Span generateHeader() {
    Span title = new Span(TITLE);
    title.addClassName("title");
    Span buttonBar = new Span();
    buttonBar.addClassName("buttons");
    Button generateTokenButton = new Button("Generate new token");
    buttonBar.add(generateTokenButton);
    generateTokenButton.addClickListener(
        event -> fireEvent(new GenerateTokenClicked(this, event.isFromClient())));
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

  public void setTokens(List<PersonalAccessTokenDTO> personalAccessTokenDTOList) {
    personalAccessTokenDTOVirtualList.setItems(personalAccessTokenDTOList);
  }

  /**
   * <b>Generate Personal Access Token Clicked</b>
   *
   * <p>Indicates that a user wants to create a {@link PersonalAccessToken}
   * within the {@link PersonalAccessTokenComponent}</p>
   */
  public static class GenerateTokenClicked extends ComponentEvent<PersonalAccessTokenComponent> {

    @Serial
    private static final long serialVersionUID = 2389754662171510873L;

    public GenerateTokenClicked(PersonalAccessTokenComponent source, boolean fromClient) {
      super(source, fromClient);
    }
  }

  /**
   * <b>Delete Token Clicked</b>
   *
   * <p>Indicates that a user wants to delete a {@link PersonalAccessToken}
   * within the {@link PersonalAccessTokenComponent}</p>
   */
  public static class DeleteTokenClicked extends ComponentEvent<PersonalAccessTokenComponent> {

    @Serial
    private static final long serialVersionUID = 5303581981248150518L;

    public DeleteTokenClicked(PersonalAccessTokenComponent source, boolean fromClient) {
      super(source, fromClient);
    }
  }

  /*Todo replace with LocalDate and figure out how to generate and encrypt token*/
  public record PersonalAccessTokenDTO(String tokenId, String tokenDescription,
                                       String expirationDate) {

  }

  //ToDo Define how this is best stored and accessed in database
  public record PersonalAccessToken() {

  }
}
