package life.qbic.datamanager.views.projects.project.info;

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.finance.application.FinanceService;
import life.qbic.finances.api.OfferSummary;
import life.qbic.projectmanagement.application.ProjectLinkingService;
import life.qbic.finance.domain.model.OfferId;
import life.qbic.finance.domain.model.OfferPreview;
import life.qbic.projectmanagement.domain.model.project.OfferIdentifier;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A component displaying all links of a project
 */
@SpringComponent
@UIScope
public class ProjectLinksComponent extends PageArea {

  @Serial
  private static final long serialVersionUID = 8598696156022371367L;
  private static final String TITLE = "Attachments";
  private static final String OFFER_TYPE_NAME = "Offer";
  private OfferSearch offerSearch;
  private final Grid<ProjectLink> projectLinks = new Grid<>(ProjectLink.class);
  private final ProjectLinksComponentHandler projectLinksComponentHandler;

  public ProjectLinksComponent(@Autowired ProjectLinkingService projectLinkingService,
      @Autowired FinanceService financeService) {
    Objects.requireNonNull(financeService);
    Objects.requireNonNull(projectLinkingService);
    addClassName("attachments-area");

    initOfferSearch(financeService);
    initProjectLinksGrid();

    initLayout();

    projectLinksComponentHandler = new ProjectLinksComponentHandler(projectLinkingService);
  }

  private void initLayout() {
    Span titleSpan = new Span();
    titleSpan.add(TITLE);
    titleSpan.addClassName("title");

    add(titleSpan);
    add(offerSearch);
    add(projectLinks);
  }

  public void setContext(Context context) {
    projectLinksComponentHandler.setProjectId(context.projectId().orElseThrow());
  }

  private static ProjectLink offerLink(OfferIdentifier offerIdentifier) {
    return ProjectLink.of(OFFER_TYPE_NAME, offerIdentifier.value());
  }

  private void initOfferSearch(FinanceService financeService) {
    offerSearch = new OfferSearch(financeService);
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
    projectLinks.addColumn(ProjectLink::type).setHeader("Type").setSortable(true);
    projectLinks.addColumn(ProjectLink::reference).setHeader("Reference").setSortable(true);
    projectLinks.addColumn(new ComponentRenderer<>(Button::new, (deleteButton, projectLink) -> {
      deleteButton.addClassName("delete-button");
      deleteButton.setIcon(new Icon("lumo", "cross"));
      deleteButton.addClickListener(e -> projectLinksComponentHandler.removeLink(projectLink));
    }));
    projectLinks.setItems(new ArrayList<>());
  }


  private static ProjectLink offerLink(String offerIdentifier) {
    return ProjectLink.of(OFFER_TYPE_NAME, offerIdentifier);
  }

  public List<String> linkedOffers() {
    return projectLinks.getDataProvider().fetch(new Query<>())
        .filter(it -> Objects.equals(it.type(), OFFER_TYPE_NAME)).map(ProjectLink::reference)
        .toList();
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

  private static class OfferSearch extends Composite<ComboBox<OfferSummary>> {

    private final transient FinanceService financeService;

    public static class SelectedOfferChangeEvent extends
        ComponentValueChangeEvent<OfferSearch, OfferSummary> {

      /**
       * Creates a new component value change event.
       *
       * @param source     the source component
       * @param hasValue   the HasValue from which the value originates
       * @param oldValue   the old value
       * @param fromClient whether the value change originated from the client
       */
      public SelectedOfferChangeEvent(OfferSearch source, HasValue<?, OfferSummary> hasValue,
          OfferSummary oldValue, boolean fromClient) {
        super(source, hasValue, oldValue, fromClient);
      }
    }


    public OfferSearch(@Autowired FinanceService financeService) {
      Objects.requireNonNull(financeService);
      this.financeService = financeService;
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

    public void addSelectedOfferChangeListener(
        ComponentEventListener<SelectedOfferChangeEvent> listener) {
      addListener(SelectedOfferChangeEvent.class, listener);
    }

    public void clearSelection() {
      getContent().clear();
    }

    private void setLabels() {
      getContent().setItemLabelGenerator(OfferSummary::offerId);
    }

    private void setRenderer() {
      getContent().setRenderer(new ComponentRenderer<>(OfferSearch::textFromPreview));
    }

    private static Text textFromPreview(OfferSummary summary) {
      return new Text(summary.offerId() + ", " + summary.title());
    }

    private void setItems() {
      getContent().setItems(query -> financeService.findOfferContainingProjectTitleOrId(
          query.getFilter().orElse(""), query.getFilter().orElse(""), query.getOffset(),
          query.getLimit()).stream());
    }

  }
}
