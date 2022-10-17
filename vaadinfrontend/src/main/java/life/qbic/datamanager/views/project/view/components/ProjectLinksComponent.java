package life.qbic.datamanager.views.project.view.components;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import life.qbic.datamanager.views.layouts.CardLayout;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.domain.finances.offer.Offer;
import life.qbic.projectmanagement.domain.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A component displaying all links of a project
 */
@SpringComponent
@UIScope
public class ProjectLinksComponent extends Composite<CardLayout> {

  @Serial
  private static final long serialVersionUID = 8598696156022371367L;

  final Grid<ProjectLink> projectLinks;

  private final List<ProjectLink> linkList;

  private final ProjectInformationService projectInformationService;


  public ProjectLinksComponent(@Autowired ProjectInformationService projectInformationService) {
    Objects.requireNonNull(projectInformationService);
    this.projectInformationService = projectInformationService;
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

  public void projectId(String projectId) {
    var linkedOffers = projectInformationService.queryLinkedOffers(ProjectId.parse(projectId));
    linkedOffers.forEach(offerId -> linkList.add(ProjectLink.of("Offer", offerId.value())));
  }

  /**
   * This adds styles to the component to define its size and position in the parent grid
   * @param col_span the number of columns to span
   * @param row_span the number of rows to span
   */
  public void setGridLayout(int col_span, int row_span){
    getContent().addClassNames("col-span-" + col_span,"row-span-" + row_span);
  }
}
