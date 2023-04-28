package life.qbic.datamanager.views.projects.project.info;

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import life.qbic.datamanager.views.layouts.CardLayout;
import life.qbic.projectmanagement.application.ProjectLinkingService;
import life.qbic.projectmanagement.application.finances.offer.OfferLookupService;
import life.qbic.projectmanagement.domain.finances.offer.OfferId;
import life.qbic.projectmanagement.domain.finances.offer.OfferPreview;
import life.qbic.projectmanagement.domain.project.OfferIdentifier;
import life.qbic.projectmanagement.domain.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A component displaying all links of a project
 */
@SpringComponent
@UIScope
public class ProjectLinksComponent extends Composite<CardLayout> {

  @Serial
  private static final long serialVersionUID = 8598696156022371367L;
  private static final String TITLE = "Attachments";
  private OfferSearch offerSearch;
  private final Grid<ProjectLink> projectLinks = new Grid<>(ProjectLink.class);
  private static final String OFFER_TYPE_NAME = "Offer";

  private final ProjectLinksComponentHandler projectLinksComponentHandler;

  public ProjectLinksComponent(@Autowired ProjectLinkingService projectLinkingService,
                               @Autowired OfferLookupService offerLookupService) {
    Objects.requireNonNull(offerLookupService);
    Objects.requireNonNull(projectLinkingService);
    initOfferSearch(offerLookupService);
    initProjectLinksGrid();
    getContent().addTitle(TITLE);
    getContent().addFields(offerSearch, projectLinks);
    projectLinksComponentHandler = new ProjectLinksComponentHandler(projectLinkingService);
  }

  public void projectId(ProjectId projectId) {
    projectLinksComponentHandler.setProjectId(projectId);
  }

  private static ProjectLink offerLink(OfferIdentifier offerIdentifier) {
    return ProjectLink.of(OFFER_TYPE_NAME, offerIdentifier.value());
  }

  private void initOfferSearch(OfferLookupService offerLookupService) {
    offerSearch = new OfferSearch(offerLookupService);
    offerSearch.addSelectedOfferChangeListener(it -> {
      if (Objects.isNull(it.getValue())) {
        return;
      }
      if (!it.isFromClient()) {
        return;
      }
      if (it.getValue() != it.getOldValue()) {
        projectLinksComponentHandler.addLink(offerLink(it.getValue().offerId()));
        it.getSource().clearSelection();
      }
    });
  }

  private void initProjectLinksGrid() {
    projectLinks.addColumn(ProjectLink::type).setHeader("Type");
    projectLinks.addColumn(ProjectLink::reference).setHeader("Reference");
    projectLinks.addColumn(new ComponentRenderer<>(Button::new, (button, projectLink) -> {
      button.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR,
          ButtonVariant.LUMO_TERTIARY);
      button.setIcon(new Icon("lumo", "cross"));
      button.addClickListener(e -> projectLinksComponentHandler.removeLink(projectLink));
    }));
    projectLinks.setItems(new ArrayList<>());
  }


  private static ProjectLink offerLink(OfferId offerIdentifier) {
    return ProjectLink.of(OFFER_TYPE_NAME, offerIdentifier.id());
  }

  public List<String> linkedOffers() {
    return projectLinks.getDataProvider().fetch(new Query<>())
        .filter(it -> Objects.equals(it.type(), OFFER_TYPE_NAME)).map(ProjectLink::reference)
        .toList();
  }

  public void setStyles(String... componentStyles) {
    getContent().addClassNames(componentStyles);
  }

  private final class ProjectLinksComponentHandler {

    ProjectId projectId;
    private final ProjectLinkingService projectLinkingService;

    public ProjectLinksComponentHandler(ProjectLinkingService projectLinkingService) {
      this.projectLinkingService = projectLinkingService;
    }

    private void loadContentForProject(ProjectId projectId) {
      var linkedOffers = projectLinkingService.queryLinkedOffers(projectId);
      List<ProjectLink> offerLinks = linkedOffers.stream().map(ProjectLinksComponent::offerLink)
          .toList();
      projectLinks.setItems(offerLinks);
    }

    private void addLink(ProjectLink projectLink) {
      if (projectLink.type().equals(OFFER_TYPE_NAME)) {
        projectLinkingService.linkOfferToProject(projectLink.reference(), this.projectId.value());
      }
      projectLinksComponentHandler.loadContentForProject(projectId);
    }

    private void removeLink(ProjectLink projectLink) {
      if (projectLink.type().equals(OFFER_TYPE_NAME)) {
        projectLinkingService.unlinkOfferFromProject(projectLink.reference(),
            this.projectId.value());
      }
      loadContentForProject(projectId);
    }

    public void setProjectId(ProjectId projectId) {
      this.projectId = projectId;
      loadContentForProject(projectId);
    }

  }

  private static class OfferSearch extends Composite<ComboBox<OfferPreview>> {

    private final transient OfferLookupService offerLookupService;

    public static class SelectedOfferChangeEvent extends
        ComponentValueChangeEvent<OfferSearch, OfferPreview> {

      /**
       * Creates a new component value change event.
       *
       * @param source     the source component
       * @param hasValue   the HasValue from which the value originates
       * @param oldValue   the old value
       * @param fromClient whether the value change originated from the client
       */
      public SelectedOfferChangeEvent(OfferSearch source, HasValue<?, OfferPreview> hasValue,
                                      OfferPreview oldValue, boolean fromClient) {
        super(source, hasValue, oldValue, fromClient);
      }
    }


    public OfferSearch(@Autowired OfferLookupService offerLookupService) {
      Objects.requireNonNull(offerLookupService);
      this.offerLookupService = offerLookupService;
      setItems();
      setRenderer();
      setLabels();
      forwardValueChangeEvents();
    }

    private void forwardValueChangeEvents() {
      getContent().addValueChangeListener(it -> fireEvent(
          new SelectedOfferChangeEvent(this, it.getHasValue(), it.getOldValue(),
              it.isFromClient())));
    }

    public Registration addSelectedOfferChangeListener(
        ComponentEventListener<SelectedOfferChangeEvent> listener) {
      return this.addListener(SelectedOfferChangeEvent.class, listener);
    }

    public void clearSelection() {
      getContent().clear();
    }

    private void setLabels() {
      getContent().setItemLabelGenerator(it -> it.offerId().id());
    }

    private void setRenderer() {
      getContent().setRenderer(new ComponentRenderer<>(OfferSearch::textFromPreview));
    }

    private static Text textFromPreview(OfferPreview preview) {
      return new Text(preview.offerId().id() + ", " + preview.getProjectTitle().title());
    }

    private void setItems() {
      getContent().setItems(query -> offerLookupService.findOfferContainingProjectTitleOrId(
          query.getFilter().orElse(""), query.getFilter().orElse(""), query.getOffset(),
          query.getLimit()).stream());
    }

  }
}
