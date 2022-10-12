package life.qbic.datamanager.views.project.create;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import life.qbic.datamanager.views.components.CardLayout;
import life.qbic.projectmanagement.domain.finances.offer.Offer;

/**
 * A component displaying all links of a project
 */
@SpringComponent
@UIScope
public class ProjectLinksComponent extends Composite<CardLayout> {

  final Grid<ProjectLink> projectLinks;

  private final List<ProjectLink> linkList;


  public ProjectLinksComponent() {

    linkList = new ArrayList<>();

    projectLinks = new Grid<>(ProjectLink.class);
    projectLinks.addColumn(ProjectLink::type).setHeader("Type");
    projectLinks.addColumn(ProjectLink::reference).setHeader("Reference");
    projectLinks.addColumn(
        new ComponentRenderer<>(Button::new, (button, projectLink) -> {
          button.addThemeVariants(ButtonVariant.LUMO_ICON,
              ButtonVariant.LUMO_ERROR,
              ButtonVariant.LUMO_TERTIARY);
          button.addClickListener(e -> {
            linkList.remove(projectLink);
            projectLinks.getDataProvider().refreshAll();
            }
          );
          button.setIcon(new Icon("lumo","cross"));
        })
    );
    projectLinks.setItems(linkList);

    getContent().addTitle("Links");
    getContent().addFields(projectLinks);
  }

  public void addLink(Offer offer) {
    linkList.add(ProjectLink.of("Offer", offer.offerId().id()));
  }

  public List<String> linkedOffers() {
    return linkList.stream().filter(it -> Objects.equals(it.type(), "Offer")).map(ProjectLink::reference).toList();
  }
}
