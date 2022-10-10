package life.qbic.datamanager.views.project.create;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.spring.annotation.SpringComponent;
import life.qbic.datamanager.views.components.CardLayout;
import life.qbic.projectmanagement.domain.finances.offer.Offer;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
@SpringComponent
public class ProjectLinksComponent extends Composite<CardLayout> {

  private final HorizontalLayout offerLinks;

  public ProjectLinksComponent() {
    this.offerLinks = new HorizontalLayout();
    getContent().addFields(new Hr(), offerLinks, new Hr());
    getContent().addTitle("Links");
    Button linkOfferButton = new Button();
    linkOfferButton.setIcon(VaadinIcon.LINK.create());
    linkOfferButton.setText("Link offer");
    getContent().addButtons(linkOfferButton);
  }

  public void addLink(Offer offer) {
    OfferLinkComponent linkComponent = OfferLinkComponent.of(offer);
    offerLinks.add(linkComponent);
  }

  private static class OfferLinkComponent extends Composite<HorizontalLayout> {


    private OfferLinkComponent(String offerId) {
      Label referenceLabel = new Label(offerId);
      Button unlinkButton = new Button(VaadinIcon.UNLINK.create());
      unlinkButton.addClickListener(it -> this.getElement().removeFromTree());
      getContent().add(referenceLabel, unlinkButton);
      getContent().setDefaultVerticalComponentAlignment(Alignment.BASELINE);
    }

    public static OfferLinkComponent of(Offer offer) {
      return new OfferLinkComponent(offer.offerId().id());
    }
  }

}
