package life.qbic.datamanager.views.projects.project.ontology;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.dataview.GridLazyDataView;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoUtility.IconSize;
import java.io.Serial;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import life.qbic.application.commons.SortOrder;
import life.qbic.datamanager.views.general.Card;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.general.Tag;
import life.qbic.projectmanagement.application.ontology.OntologyClass;
import life.qbic.projectmanagement.application.ontology.OntologyLookupService;
import life.qbic.projectmanagement.domain.model.Ontology;
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
  private final Div ontologyGridSection = new Div();
  private GridLazyDataView<OntologyClass> ontologyGridLazyDataView;
  private String searchTerm = "";
  private final Span foundResults = new Span();
  private final transient OntologyLookupService ontologyTermInformationService;
  private static final int ONTOLOGY_SEARCH_LOWER_LIMIT = 2;

  public OntologyLookupComponent(
      @Autowired OntologyLookupService ontologyTermInformationService) {
    requireNonNull(ontologyTermInformationService);
    this.ontologyTermInformationService = ontologyTermInformationService;
    Span title = new Span("Ontology Lookup");
    title.addClassName("title");
    add(title);
    int numOfOntologies = ontologyTermInformationService.findUniqueOntologies().size();
    Span description = new Span(String.format(
        "Here you can search our database for ontology terms from %d different ontologies.", numOfOntologies));
    add(description);
    initSearchField();
    add(searchField);
    initGridSection();
    add(ontologyGridSection);
    addClassName("ontology-lookup-component");
  }

  private void setLazyDataProviderForOntologyGrid(Grid<OntologyClass> ontologyGrid) {
    ontologyGridLazyDataView = ontologyGrid.setItems(query -> {
      List<SortOrder> sortOrders = query.getSortOrders().stream().map(
              it -> new SortOrder(it.getSorted(), it.getDirection().equals(SortDirection.DESCENDING)))
          .toList();
      var allOntologyAbbreviations = Arrays.stream(Ontology.values()).map(Ontology::getAbbreviation)
          .toList();
      return ontologyTermInformationService.queryOntologyTerm(searchTerm, allOntologyAbbreviations,
          query.getOffset(),
          query.getLimit(),
          List.copyOf(sortOrders)).stream();
    });
  }

  private void initGridSection() {
    Grid<OntologyClass> ontologyGrid = new Grid<>();
    ontologyGrid.setSelectionMode(SelectionMode.NONE);
    ontologyGrid.addComponentColumn(
        ontologyClass -> new OntologyItem(ontologyClass.getClassLabel(),
            ontologyClass.getCurie().replace("_", ":"),
            ontologyClass.getClassIri(), ontologyClass.getDescription(),
            Ontology.findOntologyByAbbreviation(ontologyClass.getOntologyAbbreviation())
                .getName()));
    ontologyGrid.addClassName("ontology-grid");
    ontologyGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS);
    setLazyDataProviderForOntologyGrid(ontologyGrid);
    ontologyGridSection.add(foundResults, ontologyGrid);
    foundResults.addClassName("secondary");
    ontologyGridSection.addClassName("ontology-grid-section");
  }

  /**
   * Resets the value within the searchField, which in turn resets the grid. Additionally, hides the
   * entire section so the result span is only shown when the user is actively searching for an
   * ontology
   */
  public void resetSearch() {
    searchField.setValue("");
    showResultSection(false);
  }

  private void initSearchField() {
    searchField.setClassName("search-field");
    searchField.setPlaceholder("Search for ontology term e.g. Whole blood");
    searchField.setHelperText("Please provide at least %s letters to search for entries".formatted(
        ONTOLOGY_SEARCH_LOWER_LIMIT));
    searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
    searchField.setValueChangeMode(ValueChangeMode.LAZY);
    searchField.addValueChangeListener(
        event -> {
          searchTerm = event.getValue();
          ontologyGridLazyDataView.refreshAll();
          boolean searchTermLowerLimitReached =
              event.getValue().length() >= ONTOLOGY_SEARCH_LOWER_LIMIT;
          showResultSection(searchTermLowerLimitReached);
        });
  }

  private void showResultSection(boolean isVisible) {
    foundResults.setVisible(isVisible);
    foundResults.setText(
        "%s results found".formatted(ontologyGridLazyDataView.getItems().count()));
    ontologyGridSection.setVisible(isVisible);
  }

  /**
   * Ontology Item
   * <p>
   * The ontology Item is a Div container styled similiar to the {@link Card} component, hosting the
   * ontology information of interest provided by a {@link OntologyClass}
   */
  private static class OntologyItem extends Div {

    public OntologyItem(String label, String curie, String classIri, String descriptionText,
        String ontologyAbbreviation) {
      Span header = createHeader(label, curie);
      add(header);
      Span url = createUrl(classIri);
      add(url);
      Div description = createDescription(descriptionText);
      add(description);
      Span origin = createOrigin(ontologyAbbreviation);
      add(origin);
      addClassName("ontology-item");
    }

    private Span createHeader(String label, String curieText) {
      Span title = new Span(label);
      title.addClassName("ontology-item-title");
      Span curie = new Tag(curieText);

      Icon copyIcon = initCopyIcon();
      Span header = new Span(title, curie, copyIcon);

      copyIcon.addClickListener(
          event -> handleCopyClicked(header, curieText));

      header.addClassName("header");
      return header;
    }

    private void handleCopyClicked(Span header, String curieText) {
      UI.getCurrent().getPage().executeJs("window.copyToClipboard($0)", curieText);
      Icon copyIcon = (Icon) header.getChildren().filter(c -> c instanceof Icon).toList().get(0);
      Icon checkIcon = VaadinIcon.CHECK.create();
      checkIcon.addClassName(IconSize.SMALL);
      checkIcon.addClassNames("copy-icon-success");
      removeClassName("base-background");
      addClassName("success-background-hue");
      header.remove(copyIcon);
      header.add(checkIcon);

      // reset copy view after one second
      UI ui = UI.getCurrent();
      ui.getPushConfiguration().setPushMode(PushMode.MANUAL);
      new Thread(() -> {
        try {
          TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
        }
        ui.access(() -> {
          removeClassName("success-background-hue");
          addClassName("base-background");
          header.remove(checkIcon);
          header.add(copyIcon);
          ui.push();
        });
      }).start();
    }

    private Icon initCopyIcon() {
      Icon copyIcon = VaadinIcon.COPY_O.create();
      copyIcon.addClassName(IconSize.SMALL);
      copyIcon.addClassNames("clickable", "copy-icon");
      return copyIcon;
    }

    private Span createUrl(String ontologyURL) {
      Span url = new Span(ontologyURL);
      url.addClassName("url");
      return url;
    }

    private Div createDescription(String ontologyDescription) {
      Div description = new Div();
      description.add(ontologyDescription);
      description.addClassName("description");
      return description;
    }

    private Span createOrigin(String origin) {
      Tag originAbbreviation = new Tag(origin);
      originAbbreviation.addClassName("primary");
      Span originPreText = new Span("Comes from the Ontology: ");
      originPreText.addClassName("secondary");
      Span ontologyOrigin = new Span(originPreText, originAbbreviation);
      ontologyOrigin.addClassName("origin");
      return ontologyOrigin;
    }
  }
}
