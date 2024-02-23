package life.qbic.datamanager.views.projects.project.ontology;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.dataview.GridLazyDataView;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoUtility.IconSize;
import java.io.Serial;
import java.util.Arrays;
import java.util.List;
import life.qbic.datamanager.views.general.Card;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.general.Tag;
import life.qbic.projectmanagement.application.OntologyTermInformationService;
import life.qbic.projectmanagement.application.SortOrder;
import life.qbic.projectmanagement.domain.model.Ontology;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.OntologyClassDTO;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Ontology Lookup Component
 * <p>
 * The ontology lookup component is a {@link PageArea} component, which enables a user to look up
 * the ontologies supported within the data manager. It allows the user to provide a string based
 * search term and get results with the correct IRI for the ontology terms.
 */
@SpringComponent
@UIScope
@JsModule("./javascript/copytoclipboard.js")
public class OntologyLookupComponent extends PageArea {

  @Serial
  private static final long serialVersionUID = -1777819501917841723L;
  private final TextField searchField = new TextField();
  private final Grid<OntologyClassDTO> ontologyGrid = new Grid<>();
  private GridLazyDataView<OntologyClassDTO> ontologyGridLazyDataView;
  private String searchTerm = "";
  private final OntologyTermInformationService ontologyTermInformationService;

  public OntologyLookupComponent(
      @Autowired OntologyTermInformationService ontologyTermInformationService) {
    requireNonNull(ontologyTermInformationService);
    this.ontologyTermInformationService = ontologyTermInformationService;
    Span title = new Span("Ontology Search/Lookup");
    title.addClassName("title");
    add(title);
    Span description = new Span(
        "You can search here for ontology terms from over 20 different ontologies.");
    add(description);
    addSearchEventListener(event -> {
      searchTerm = event.searchValue();
      ontologyGridLazyDataView.refreshAll();
    });
    initSearchField();
    layoutGrid();
    setLazyDataProviderForOntologyGrid();
    addClassName("ontology-lookup-component");
  }

  private void setLazyDataProviderForOntologyGrid() {
    List<Ontology> ontologies = Arrays.stream(Ontology.values()).toList();
    List<String> ontologyAbbreviations = ontologies.stream()
        .map(Ontology::getAbbreviation)
        .toList();
    ontologyGridLazyDataView = ontologyGrid.setItems(query -> {
      List<SortOrder> sortOrders = query.getSortOrders().stream().map(
              it -> new SortOrder(it.getSorted(), it.getDirection().equals(SortDirection.DESCENDING)))
          .toList();
      return ontologyTermInformationService.queryOntologyTerm(searchTerm, ontologyAbbreviations,
          query.getOffset(), query.getLimit(),
          List.copyOf(sortOrders)).stream().map(OntologyClassDTO::from);
    });
  }

  private void layoutGrid() {
    ontologyGrid.addComponentColumn(
        ontologyClassDTO -> new OntologyItem(ontologyClassDTO.getLabel(),
            ontologyClassDTO.getName(),
            ontologyClassDTO.getClassIri(), ontologyClassDTO.getDescription(),
            ontologyClassDTO.getOntologyAbbreviation()));
    ontologyGrid.addClassName("ontology-grid");
    ontologyGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS);
    add(ontologyGrid);
  }

  /**
   * Resets the value within the searchfield, which in turn resets the grid via the fired
   * {@link OntologySearchEvent}
   */
  public void resetSearch() {
    searchField.setValue("");
  }

  /**
   * Register an {@link ComponentEventListener} that will get informed with a
   * {@link OntologySearchEvent}, as soon as a user searches for an ontology of interest.
   *
   * @param ontologySearchListener a listener on the search event trigger
   */
  public void addSearchEventListener(
      ComponentEventListener<OntologySearchEvent> ontologySearchListener) {
    addListener(OntologySearchEvent.class, ontologySearchListener);
  }

  private void initSearchField() {
    searchField.setClassName("search-field");
    searchField.setPlaceholder("Search for ontology term e.g. Whole blood");
    searchField.setPlaceholder("Search");
    searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
    searchField.setValueChangeMode(ValueChangeMode.LAZY);
    searchField.addValueChangeListener(
        event -> fireEvent(new OntologySearchEvent(this, event.isFromClient(), event.getValue())));
    add(searchField);
  }


  /**
   * Ontology Item
   * <p>
   * The ontology Item is a Div container styled similiar to the {@link Card} component, hosting the
   * ontology information of interest provided by a {@link OntologyClassDTO}
   */
  private static class OntologyItem extends Div {

    public OntologyItem(String label, String name, String classIri, String description,
        String ontologyAbbreviation) {
      addHeader(label, name);
      addUrl(classIri);
      addDescription(description);
      addOrigin(ontologyAbbreviation);
      addClassName("ontology-item");
    }

    private void addHeader(String label, String nameText) {
      Span title = new Span(label);
      title.addClassName("ontology-item-title");
      Span name = new Tag(nameText);
      name.addClassNames("primary", "clickable");
      name.addClickListener(
          event -> UI.getCurrent().getPage().executeJs("window.copyToClipboard($0)", nameText));
      Span header = new Span(title, name, initCopyIcon(nameText));
      header.addClassName("header");
      add(header);
    }

    private void addUrl(String ontologyURL) {
      Span url = new Span(ontologyURL);
      url.addClassName("url");
      add(url);
    }

    private void addDescription(String ontologyDescription) {
      Div description = new Div();
      description.add(ontologyDescription);
      description.addClassName("description");
      add(description);
    }

    private Icon initCopyIcon(String copyContent) {
      Icon copyIcon = VaadinIcon.COPY_O.create();
      copyIcon.addClassName(IconSize.SMALL);
      copyIcon.addClassNames("clickable", "copy-icon");
      copyIcon.addClickListener(
          event -> UI.getCurrent().getPage().executeJs("window.copyToClipboard($0)", copyContent));
      return copyIcon;
    }

    private void addOrigin(String origin) {
      Tag originAbbreviation = new Tag(origin);
      originAbbreviation.addClassName("primary");
      Span originPreText = new Span("Comes from the Ontology: ");
      originPreText.addClassName("secondary");
      Span ontologyOrigin = new Span(originPreText, originAbbreviation);
      ontologyOrigin.addClassName("origin");
      add(ontologyOrigin);
    }
  }

  /**
   * <b>Ontology Search Event</b>
   *
   * <p>Indicates that an user wants to search for a list of {@link OntologyClassDTO} containing
   * the provided value within the {@link OntologyLookupComponent}</p>
   */
  public static class OntologySearchEvent extends ComponentEvent<OntologyLookupComponent> {

    @Serial
    private static final long serialVersionUID = 6244919186359669052L;
    private final String searchValue;

    public OntologySearchEvent(OntologyLookupComponent source, boolean fromClient,
        String searchValue) {
      super(source, fromClient);
      this.searchValue = searchValue;
    }

    public String searchValue() {
      return searchValue;
    }
  }
}
