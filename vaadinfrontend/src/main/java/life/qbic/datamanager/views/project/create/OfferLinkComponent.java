package life.qbic.datamanager.views.project.create;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import java.util.function.Consumer;
import life.qbic.datamanager.views.project.create.LinkList.LinkElement;
import life.qbic.projectmanagement.domain.finances.offer.Offer;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
class OfferLinkComponent extends Composite<HorizontalLayout> implements
    LinkElement {


  private final Button unlinkButton;

  private OfferLinkComponent(String offerId) {
    Label referenceLabel = new Label(offerId);
    unlinkButton = new Button(VaadinIcon.UNLINK.create());
    getContent().add(referenceLabel, unlinkButton);
    getContent().setDefaultVerticalComponentAlignment(Alignment.BASELINE);
  }

  public static OfferLinkComponent of(Offer offer) {
    return new OfferLinkComponent(offer.offerId().id());
  }

  @Override
  public void onUnlink(Consumer<LinkElement> consumer) {
    unlinkButton.addClickListener(it -> consumer.accept(this));
  }
}
