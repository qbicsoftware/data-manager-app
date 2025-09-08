package life.qbic.datamanager.views.account;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import java.net.URI;
import life.qbic.datamanager.views.general.oidc.OidcLogo;
import life.qbic.datamanager.views.general.oidc.OidcType;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class AccountContentBox extends Div {

  public AccountContentBox(String orcidId, URI record) {
    addClassNames("flex-horizontal", "flex-align-items-center");
    var iconContainer = new Div();
    iconContainer.add(new OidcLogo(OidcType.ORCID));

    var accountInfo = new Div();
    accountInfo.addClassNames("flex-vertical");
    accountInfo.add(new Div("ORCiD Account"));
    accountInfo.add(new Div(orcidId));

    var publicRecord = new Div();
    publicRecord.add(new Anchor(record.toString(), "View public record", AnchorTarget.BLANK));

    add(iconContainer, accountInfo, publicRecord);

  }

}
