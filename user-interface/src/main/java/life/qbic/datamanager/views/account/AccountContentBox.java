package life.qbic.datamanager.views.account;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import java.net.URI;
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

  public AccountContentBox(String orcidId, URI record) {
    addClassNames("flex-horizontal", "flex-align-items-center", "account-box", "gap-04",
        "padding-left-right-04", "padding-top-bottom-04");
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

    var publicRecord = new Div();
    publicRecord.addClassName("white-space-nowrap");
    publicRecord.add(new Anchor(record.toString(), "View public record", AnchorTarget.BLANK));

    var spacer = new Div();
    spacer.setWidthFull();

    add(iconContainer, accountInfo, spacer, publicRecord);
  }

}
