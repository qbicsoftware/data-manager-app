package life.qbic.datamanager.views.projects.create;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.data.provider.CallbackDataProvider.FetchCallback;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import java.util.Objects;
import life.qbic.datamanager.views.general.OntologyComponent;
import life.qbic.datamanager.views.projects.project.experiments.OntologyFilterConnector;
import life.qbic.projectmanagement.application.ontology.SpeciesLookupService;
import life.qbic.projectmanagement.application.ontology.TerminologyService;
import life.qbic.projectmanagement.domain.model.OntologyTerm;

/**
 * Factory class for creating MultiSelectComboBox instances for different ontology types. It
 * provides methods to create comboboxes for analytes, species, and specimens.
 */
public class OntologyComboboxFactory {

  /**
   * Hint shown while the field is focused but its term is too short to search. Without it the field
   * looks broken: {@link OntologyFilterConnector} deliberately returns nothing below this length, so
   * the dropdown never opens and nothing explains why.
   */
  private static final String MINIMUM_TERM_HINT =
      "Type at least " + OntologyFilterConnector.MINIMUM_QUERY_LENGTH + " letters to search.";

  private static final String SEARCH_HINT = "Start typing — public ontology terms will be suggested.";

  private final SpeciesLookupService speciesLookupService;

  private final TerminologyService terminologyService;

  public OntologyComboboxFactory(SpeciesLookupService speciesLookupService, TerminologyService terminologyService) {
    this.speciesLookupService = requireNonNull(speciesLookupService,
        "ontologyTermInformationService must not be null");
    this.terminologyService = Objects.requireNonNull(terminologyService);
  }

  public MultiSelectComboBox<OntologyTerm> analyteBox() {
    MultiSelectComboBox<OntologyTerm> box = newBox();
    box.setItems(ontologyTermFetchCallback());

    box.setPlaceholder("Search and select one or more analytes for your samples");
    box.setLabel("Analytes");
    box.addClassName("full-width-input");
    return box;
  }

  private FetchCallback<OntologyTerm, String> speciesFetchCallback() {
    return query -> OntologyFilterConnector.loadOntologyTerms(query,
        speciesLookupService);
  }

  private FetchCallback<OntologyTerm, String> ontologyTermFetchCallback() {
    return query -> OntologyFilterConnector.loadOntologyTerms(query, terminologyService);
  }

  public MultiSelectComboBox<OntologyTerm> speciesBox() {

    MultiSelectComboBox<OntologyTerm> box = newBox();
    box.setItems(speciesFetchCallback());

    box.setPlaceholder("Search and select one or more species for your samples");
    box.setLabel("Species");
    return box;
  }

  public MultiSelectComboBox<OntologyTerm> specimenBox() {

    MultiSelectComboBox<OntologyTerm> box = newBox();
    box.setItems(ontologyTermFetchCallback());

    box.setPlaceholder("Search and select one or more specimen for your samples");
    box.setLabel("Specimen");
    return box;
  }

  private static MultiSelectComboBox<OntologyTerm> newBox() {
    MultiSelectComboBox<OntologyTerm> box = new MultiSelectComboBox<>();
    box.setRequired(true);
    // No per-box helper text for the *contract*: three origin fields in a row repeated the same
    // sentence, so the shared hint lives in the form's purpose note instead. What the helper slot
    // does carry is the live search hint below, because it changes with what the user has typed
    // and a field whose dropdown silently refuses to open needs that feedback next to it.
    box.setRenderer(new ComponentRenderer<>(OntologyComponent::new));
    box.setItemLabelGenerator(OntologyComboboxFactory::ontologyItemFormatted);
    box.addClassName("chip-badge");
    // The chevron is kept. It was suppressed here, which left a field holding only chips and an
    // "x" reading as a static value rather than as something that opens a list. Opening the list
    // is now cheap and honest rather than a wasted request, because OntologyFilterConnector refuses
    // a query shorter than the minimum: a click on a focused field prompts the user to type instead
    // of scanning every ontology.
    attachSearchHint(box);
    return box;
  }

  /**
   * Keeps the helper text in step with the typed term: it prompts the user to start typing, then
   * says why nothing is being suggested until the term is long enough, then gets out of the way
   * once a real search is possible.
   *
   * <p>The term is read from the element's own {@code filter} property (the server-side
   * {@code getFilter()} is protected) so the hint updates as the user types rather than only on
   * value changes.
   */
  private static void attachSearchHint(MultiSelectComboBox<OntologyTerm> box) {
    box.getElement().addPropertyChangeListener("filter", "filter-changed", event ->
        box.setHelperText(hintFor(event.getValue())));
  }

  /**
   * The helper text for a given typed term. Package-private so the contract can be tested without
   * building a UI: the user must never face a field whose dropdown silently refuses to open.
   */
  static String hintFor(Object filterValue) {
    String term = filterValue == null ? "" : filterValue.toString().trim();
    return term.length() < OntologyFilterConnector.MINIMUM_QUERY_LENGTH
        ? MINIMUM_TERM_HINT
        : SEARCH_HINT;
  }

  private static String ontologyItemFormatted(OntologyTerm ontologyTerm) {
    String ontologyLinkName = ontologyTerm.oboId().toString();
    return String.format("%s (%s)", ontologyTerm.getLabel(), ontologyLinkName);
  }

}
