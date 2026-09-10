package life.qbic.datamanager.views.account;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoUtility.IconSize;
import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import life.qbic.datamanager.views.general.ContextNote;
import life.qbic.datamanager.views.general.CopyToClipBoardComponent;
import life.qbic.datamanager.views.general.Disclaimer;
import life.qbic.datamanager.views.general.InfoBox;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.general.Tag;
import life.qbic.datamanager.views.general.Tag.TagColor;
import life.qbic.datamanager.views.settings.SettingsSection;
import life.qbic.identity.api.PersonalAccessToken;
import life.qbic.identity.api.RawToken;

/**
 * Personal Access Token Component
 * <p>
 * This {@link PageArea} allows the user to manage personal access tokens. Each token is rendered
 * as a card showing its description, status badge, creation and expiration dates. The user can
 * revoke tokens and create new ones. After creation the raw token value is shown once with a
 * prominent copy action.
 */
@SpringComponent
@UIScope
public class PersonalAccessTokenComponent extends PageArea implements Serializable {

  @Serial
  private static final long serialVersionUID = -8972242722349756972L;
  private static final String TITLE = "Personal Access Tokens";
  private final Disclaimer noTokensRegisteredDisclaimer;
  private final Div createdTokenLayout = new Div();
  private final VirtualList<PersonalAccessTokenFrontendBean> personalAccessTokens = new VirtualList<>();

  public PersonalAccessTokenComponent() {
    addClassName("personal-access-token-component");

    SettingsSection section = new SettingsSection(TITLE, buildDescription());

    // Security reassurance
    var securityNote = new ContextNote(
        "Tokens are stored encrypted and grant API access to your data — treat them like passwords.");
    securityNote.addClassName("context-note--compact");
    section.addContent(securityNote);

    Button generateTokenButton = new Button("Generate new token");
    generateTokenButton.addClassName("primary");
    generateTokenButton.addClickListener(
        event -> fireEvent(new AddTokenEvent(this, event.isFromClient())));
    section.addAction(generateTokenButton);

    Div personalAccessTokenContainer = new Div();
    noTokensRegisteredDisclaimer = createNoTokensRegisteredDisclaimer();
    personalAccessTokenContainer.add(createdTokenLayout, personalAccessTokens);
    personalAccessTokenContainer.addClassName("personal-access-token-container");
    personalAccessTokens.setRenderer(buildTokenCardRenderer());
    personalAccessTokens.addClassName("personal-access-token-list");
    createdTokenLayout.addClassName("show-created-personal-access-token-layout");

    section.addContent(noTokensRegisteredDisclaimer, personalAccessTokenContainer);
    add(section);
    updateUI();
  }

  // ── Token card renderer ────────────────────────────────────────

  private ComponentRenderer<Component, PersonalAccessTokenFrontendBean> buildTokenCardRenderer() {
    return new ComponentRenderer<>(bean -> {
      var card = new Div();
      card.addClassName("pat-card");
      card.addClassName(bean.expired() ? "pat-card--expired" : "pat-card--active");

      // Header row: description + status tag + revoke button
      var header = new Div();
      header.addClassName("pat-card__header");

      var description = new Span(bean.tokenDescription());
      description.addClassName("pat-card__description");

      var statusTag = bean.expired()
          ? createStatusTag("Expired", TagColor.ERROR)
          : createStatusTag("Active", TagColor.SUCCESS);

      var revokeButton = new Button("Revoke", VaadinIcon.TRASH.create());
      revokeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE,
          ButtonVariant.LUMO_ERROR);
      revokeButton.addClickListener(event -> fireEvent(
          new DeleteTokenEvent(this, event.isFromClient(), bean.tokenId())));

      header.add(description, statusTag, revokeButton);
      card.add(header);

      // Metadata row: created + expires
      var meta = new Div();
      meta.addClassName("pat-card__meta");
      meta.add(buildDateItem("Created", bean.createdAt()));
      meta.add(buildDateItem("Expires", bean.expirationInstant()));
      card.add(meta);

      return card;
    });
  }

  private Tag createStatusTag(String label, TagColor color) {
    var tag = new Tag(label);
    tag.setTagColor(color);
    return tag;
  }

  private Span buildDateItem(String prefix, Instant instant) {
    var span = new Span();
    span.addClassName("pat-card__date-item");
    String formatted = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withZone(ZoneId.systemDefault())
        .format(instant);
    span.setText(prefix + " " + formatted);
    return span;
  }

  // ── Created-token banner ───────────────────────────────────────

  private void showCreatedPersonalAccessToken(String rawTokenText) {
    createdTokenLayout.removeAll();
    var details = new CreatedPersonalAccessTokenDetails();
    details.setToken(rawTokenText);
    createdTokenLayout.add(details);
    updateUI();
  }

  // ── Description ────────────────────────────────────────────────

  private Div buildDescription() {
    var description = new Div();
    description.addClassName("description");

    var intro = new Paragraph(
        "Personal access tokens are the only way to authenticate via the Data Manager API. "
            + "They allow you to access your own data programmatically. ");
    description.add(intro);

    var warning = new InfoBox()
        .setInfoText("Do not share your personal access tokens with anyone you don't want to access your files.");
    description.add(warning);

    return description;
  }

  // ── Empty state ────────────────────────────────────────────────

  private Disclaimer createNoTokensRegisteredDisclaimer() {
    Disclaimer card = Disclaimer.createWithTitle(
        "Manage your tokens in one place",
        "Manage data access by registering your first personal access token",
        "Generate new token");
    card.addDisclaimerConfirmedListener(
        event -> fireEvent(new AddTokenEvent(this, event.isFromClient())));
    return card;
  }

  // ── Public API ─────────────────────────────────────────────────

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
    createdTokenLayout.removeAll();
    List<PersonalAccessTokenFrontendBean> sortedTokenList = personalAccessTokenFrontendBeans.stream()
        .sorted(Comparator.comparing(PersonalAccessTokenFrontendBean::expirationInstant,
                Instant::compareTo)
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

  public void addTokenListener(ComponentEventListener<AddTokenEvent> addTokenListener) {
    addListener(AddTokenEvent.class, addTokenListener);
  }

  public void addDeleteTokenListener(ComponentEventListener<DeleteTokenEvent> deleteTokenListener) {
    addListener(DeleteTokenEvent.class, deleteTokenListener);
  }

  // ── Events ─────────────────────────────────────────────────────

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

  // ── Frontend Bean ──────────────────────────────────────────────

  /**
   * Immutable frontend representation of a {@link PersonalAccessToken}.
   * <p>
   * Stores the actual {@link Instant} values for creation and expiration so that
   * display formatting is stable and does not drift with each render.
   */
  public static class PersonalAccessTokenFrontendBean {

    private final String tokenId;
    private final String tokenDescription;
    private final Instant expirationInstant;
    private final Instant createdAt;
    private final boolean expired;

    public PersonalAccessTokenFrontendBean(String tokenId, String tokenDescription,
        Instant createdAt, Instant expirationInstant, boolean expired) {
      this.tokenId = tokenId;
      this.tokenDescription = tokenDescription;
      this.createdAt = createdAt;
      this.expirationInstant = expirationInstant;
      this.expired = expired;
    }

    public static PersonalAccessTokenFrontendBean from(PersonalAccessToken pat) {
      Instant now = Instant.now();
      Instant expiration = pat.expiration();
      // Derive creation date: expiration minus the remaining duration
      Duration remaining = Duration.between(now, expiration);
      Instant createdAt = expiration.minus(
          Duration.ofDays(Math.max(0, remaining.toDays())));
      return new PersonalAccessTokenFrontendBean(
          pat.tokenId(),
          pat.description(),
          createdAt.truncatedTo(ChronoUnit.SECONDS),
          expiration,
          pat.expired());
    }

    public String tokenId() {
      return tokenId;
    }

    public String tokenDescription() {
      return tokenDescription;
    }

    public Instant expirationInstant() {
      return expirationInstant;
    }

    public Instant createdAt() {
      return createdAt;
    }

    public boolean expired() {
      return expired;
    }

    /**
     * Returns the token's lifetime as a {@link Duration}, computed from
     * creation to expiration. Useful for passing to the service layer
     * which expects a duration rather than an absolute instant.
     */
    public Duration expirationDuration() {
      return Duration.between(createdAt, expirationInstant);
    }
  }

  // ── Created Token Details ──────────────────────────────────────

  /**
   * Prominent banner showing the raw token value once after creation, with a
   * large copy button and a clear warning that the value won't be shown again.
   */
  private static class CreatedPersonalAccessTokenDetails extends Div {

    private final Span rawToken = new Span();
    private final Span copyDisclaimerText = new Span();
    private final CopyToClipBoardComponent copyToClipBoardComponent = new CopyToClipBoardComponent();

    public CreatedPersonalAccessTokenDetails() {
      addClassName("show-created-personal-access-token-details");

      // Warning header
      var warningHeader = new Div();
      warningHeader.addClassName("created-token__warning");
      var warningIcon = VaadinIcon.EXCLAMATION_CIRCLE_O.create();
      warningIcon.addClassName(IconSize.SMALL);
      var warningText = new Span(
          "Copy your token now — you won't be able to see it again after leaving this page.");
      warningHeader.add(warningIcon, warningText);
      add(warningHeader);

      // Token value + copy button
      var tokenRow = new Div();
      tokenRow.addClassName("created-token__value-row");
      rawToken.addClassName("created-token__value");
      copyToClipBoardComponent.setIconSize("");
      tokenRow.add(rawToken, copyToClipBoardComponent);
      add(tokenRow);

      // Copy status text
      copyDisclaimerText.addClassName("created-token__status");
      add(copyDisclaimerText);
    }

    public void setToken(String token) {
      rawToken.setText(token);
      UI ui = UI.getCurrent();
      copyToClipBoardComponent.setCopyText(token);
      copyToClipBoardComponent.addSwitchToSuccessfulCopyIconListener(event ->
          ui.access(() -> {
            addClassName("success-background-hue");
            copyDisclaimerText.setText("Token copied to clipboard.");
          }));
      copyToClipBoardComponent.addSwitchToCopyIconListener(event ->
          ui.access(() -> removeClassName("success-background-hue")));
    }
  }
}
