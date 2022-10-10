package life.qbic.datamanager.views.project.create;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import java.util.Objects;
import life.qbic.projectmanagement.domain.finances.offer.Offer;
import org.springframework.stereotype.Component;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
@Component
public class ProjectLinksHandler {

  private ProjectLinksLayout projectLinksLayout;

  private static class OfferLinkComponent extends Composite<HorizontalLayout> {

    private final Button removeButton;

    private OfferLinkComponent(String offerTitle, String offerIdentifier) {
      removeButton = new Button(VaadinIcon.CLOSE.create());
      getContent().add(new Label(offerTitle), new Label(offerIdentifier), removeButton);
    }


    public Button getRemoveButton() {
      return removeButton;
    }
  }


  public void addLink(Offer offer) {
    OfferLinkComponent offerLinkComponent = new OfferLinkComponent(offer.projectTitle().title(),
        offer.offerId().id());
    offerLinkComponent.getRemoveButton()
        .addClickListener(it -> projectLinksLayout.offerLinks.remove(offerLinkComponent));
    projectLinksLayout.offerLinks.add(offerLinkComponent);
  }

  public void handle(ProjectLinksLayout projectLinksLayout) {
    Objects.requireNonNull(projectLinksLayout);

    if (projectLinksLayout != this.projectLinksLayout) {
      this.projectLinksLayout = projectLinksLayout;
    }
  }
}
