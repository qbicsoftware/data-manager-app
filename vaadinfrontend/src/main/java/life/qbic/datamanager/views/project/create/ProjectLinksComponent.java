package life.qbic.datamanager.views.project.create;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import life.qbic.datamanager.views.components.CardLayout;
import life.qbic.projectmanagement.domain.finances.offer.Offer;

/**
 * A component displaying all links of a project
 */
public class ProjectLinksComponent extends Composite<CardLayout> {

  private final LinkList<Offer, OfferLinkComponent> offerLinks;

  public ProjectLinksComponent() {
    this.offerLinks = new LinkList<>(new ComponentRenderer<>(OfferLinkComponent::of));
    getContent().addFields(offerLinks);
    getContent().addTitle("Links");
  }

  public void addLink(Offer offer) {
    offerLinks.addLink(offer);
  }


}
