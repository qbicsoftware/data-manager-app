package life.qbic.datamanager.views.project.create;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.spring.annotation.SpringComponent;
import java.util.ArrayList;
import java.util.List;
import life.qbic.datamanager.views.components.CardLayout;
import life.qbic.projectmanagement.domain.finances.offer.Offer;

/**
 * A component displaying all links of a project
 */
@SpringComponent
public class ProjectLinksComponent extends Composite<CardLayout> {

  private final LinkList<Offer, OfferLinkComponent> offerLinks;
  final Grid<ProjectLinkComponent> projectLinks;

  private final List<ProjectLinkComponent> list;


  public ProjectLinksComponent() {
    this.offerLinks = new LinkList<>(new ComponentRenderer<>(OfferLinkComponent::of));

    list = new ArrayList<>();

    projectLinks = new Grid<>(ProjectLinkComponent.class);
    projectLinks.addColumn(ProjectLinkComponent::type).setHeader("Type");
    projectLinks.addColumn(ProjectLinkComponent::reference).setHeader("Reference");
    projectLinks.addColumn(
        new ComponentRenderer<>(Button::new, (button, projectLink) -> {
          button.addThemeVariants(ButtonVariant.LUMO_ICON,
              ButtonVariant.LUMO_ERROR,
              ButtonVariant.LUMO_TERTIARY);
          button.addClickListener(e -> {
            list.remove(projectLink);
            projectLinks.getDataProvider().refreshAll();
            }
          );
          button.setIcon(new Icon("lumo","cross"));
        })
    );
    projectLinks.setItems(list);

    getContent().addTitle("Links");
    getContent().addFields(projectLinks);
  }

  public void addLink(Offer offer) {
    offerLinks.addLink(offer);

    list.add(new ProjectLinkComponent("Offer", offer.offerId().id()));
  }

  public List<String> linkedOffers() {
    return offerLinks.getItems().stream().map(it -> it.offerId().id()).toList();
  }


}
