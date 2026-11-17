package life.qbic.datamanager.views.account;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import life.qbic.datamanager.views.general.oidc.OidcLogo;
import life.qbic.datamanager.views.general.oidc.OidcType;

/**
 * <b>Account Content Box</b>
 * <p>
 * Small container for OIDC Linked Account information.
 *
 * @since 1.11.0
 */
public class AccountContentBox extends Div {

  private final Div actionArea = new Div();

  /**
   * @param orcidId the ORCID iD to display
   * @param record  the public record URL, or {@code null} when the issuer is not one this application
   *                knows. In that case the iD is shown without a record link rather than the box being
   *                omitted — otherwise an unknown issuer would leave the user unable to unlink.
   */
  public AccountContentBox(String orcidId, String record) {
    addClassNames("flex-horizontal", "flex-align-items-center", "account-box", "gap-04",
        "padding-04");
    var iconContainer = new Div();
    iconContainer.addClassNames("flex-vertical", "flex-align-items-center");
    var orcidLogo = new OidcLogo(OidcType.ORCID);
    orcidLogo.addClassName("icon-size-l");
    iconContainer.add(orcidLogo);

    var accountInfo = new Div();
    accountInfo.addClassNames("flex-vertical", "gap-01");

    var accountLabel = new Div("ORCiD Account");
    accountLabel.addClassNames("font-bold", "text-size-m", "line-height-m", "text-contrast-90pct");

    var orcidIdLabel = new Div(orcidId);
    orcidIdLabel.addClassNames("text-line-height-s", "text-contrast-70pct", "text-size-s");

    accountInfo.add(accountLabel);
    accountInfo.add(orcidIdLabel);

    var spacer = new Div();
    spacer.addClassNames("flex-grow-1");

    if (record != null) {
      var publicRecord = new Div();
      publicRecord.addClassName("white-space-nowrap");
      publicRecord.add(new Anchor(record, "View public record", AnchorTarget.BLANK));
      add(iconContainer, accountInfo, spacer, publicRecord, actionArea);
    } else {
      add(iconContainer, accountInfo, spacer, actionArea);
    }
  }

  /**
   * Adds the unlink action to the box.
   *
   * <p>The action is registered rather than built here so this class stays presentational: it does not
   * know what unlinking means, and it does not need a service to render.</p>
   *
   * @param onUnlink invoked when the user asks to disconnect
   */
  public void addUnlinkAction(Runnable onUnlink) {
    var unlink = new Button("Disconnect", event -> onUnlink.run());
    unlink.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ERROR);
    unlink.getElement().setAttribute("aria-label", "Disconnect this ORCiD iD");
    actionArea.add(unlink);
  }

}
