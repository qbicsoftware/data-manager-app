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
import life.qbic.projectmanagement.application.ProjectModificationService;
import life.qbic.projectmanagement.domain.finances.offer.OfferId;
import life.qbic.projectmanagement.domain.project.OfferIdentifier;
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

  private final ProjectModificationService projectModificationService;
  private static final String OFFER_TYPE_NAME = "Offer";
  private ProjectId projectId;


  public ProjectLinksComponent(@Autowired ProjectInformationService projectInformationService,
      @Autowired ProjectModificationService projectModificationService,
      @Autowired OfferSearchComponent offerSearchComponent) {
    Objects.requireNonNull(offerSearchComponent);
    Objects.requireNonNull(projectInformationService);
    this.projectInformationService = projectInformationService;
    Objects.requireNonNull(projectModificationService);
    this.projectModificationService = projectModificationService;
    linkList = new ArrayList<>();

    projectLinks = new Grid<>(ProjectLink.class);
    projectLinks.addColumn(ProjectLink::type).setHeader("Type");
    projectLinks.addColumn(ProjectLink::reference).setHeader("Reference");
    projectLinks.addColumn(
        new ComponentRenderer<>(Button::new, (button, projectLink) -> {
          button.addThemeVariants(ButtonVariant.LUMO_ICON,
              ButtonVariant.LUMO_ERROR,
              ButtonVariant.LUMO_TERTIARY);
          button.setIcon(new Icon("lumo", "cross"));
          button.addClickListener(e -> removeLink(projectLink));
        })
    );
    projectLinks.setItems(linkList);
    offerSearchComponent.addSelectedOfferChangeListener(it -> {
      if (Objects.isNull(it.getValue())) {
        return;
      }
      if (!it.isFromClient()) {
        return;
      }
      if (it.getValue() != it.getOldValue()) {
        addLink(offerLink(it.getValue().offerId()));
        it.getSource().clearSelection();
      }
    });

    getContent().addTitle("Links");
    getContent().addFields(offerSearchComponent, projectLinks);
  }

  private static ProjectLink offerLink(OfferIdentifier offerIdentifier) {
    return ProjectLink.of(OFFER_TYPE_NAME, offerIdentifier.value());
  }

  private static ProjectLink offerLink(OfferId offerIdentifier) {
    return ProjectLink.of(OFFER_TYPE_NAME, offerIdentifier.id());
  }

  private void addLink(ProjectLink projectLink) {
    if (projectLink.type().equals(OFFER_TYPE_NAME)) {
      projectModificationService.linkOfferToProject(projectLink.reference(),
          this.projectId.value());
    }
    linkList.add(projectLink);
    projectLinks.getDataProvider().refreshAll();
  }

  private void removeLink(ProjectLink projectLink) {
    if (projectLink.type().equals(OFFER_TYPE_NAME)) {
      projectModificationService.unlinkOfferFromProject(projectLink.reference(),
          this.projectId.value());
    }
    linkList.remove(projectLink);
    projectLinks.getDataProvider().refreshAll();
  }

  public List<String> linkedOffers() {
    return linkList.stream().filter(it -> Objects.equals(it.type(), OFFER_TYPE_NAME))
        .map(ProjectLink::reference).toList();
  }

  public void projectId(String projectId) {
    this.projectId = ProjectId.parse(projectId);
    loadContentForProject(this.projectId);
  }

  private void loadContentForProject(ProjectId projectId) {
    linkList.clear();
    projectLinks.getDataProvider().refreshAll();
    var linkedOffers = projectInformationService.queryLinkedOffers(projectId);
    List<ProjectLink> offerLinks = linkedOffers.stream()
        .map(ProjectLinksComponent::offerLink)
        .toList();
    linkList.addAll(offerLinks);
    projectLinks.getDataProvider().refreshAll();

  }
}
