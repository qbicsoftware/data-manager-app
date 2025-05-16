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
import life.qbic.projectmanagement.domain.model.OntologyTermV1;

/**
 * Factory class for creating MultiSelectComboBox instances for different ontology types. It
 * provides methods to create comboboxes for analytes, species, and specimens.
 */
public class OntologyComboboxFactory {

  private final SpeciesLookupService speciesLookupService;

  private final TerminologyService terminologyService;

  public OntologyComboboxFactory(SpeciesLookupService speciesLookupService, TerminologyService terminologyService) {
    this.speciesLookupService = requireNonNull(speciesLookupService,
        "ontologyTermInformationService must not be null");
    this.terminologyService = Objects.requireNonNull(terminologyService);
  }

  public MultiSelectComboBox<OntologyTermV1> analyteBox() {
    MultiSelectComboBox<OntologyTermV1> box = newBox();
    box.setItems(ontologyTermFetchCallback());

    box.setPlaceholder("Search and select one or more analytes for your samples");
    box.setLabel("Analytes");
    box.addClassName("full-width-input");
    return box;
  }

  private FetchCallback<OntologyTermV1, String> speciesFetchCallback() {
    return query -> OntologyFilterConnector.loadOntologyTerms(query,
        speciesLookupService);
  }

  private FetchCallback<OntologyTermV1, String> ontologyTermFetchCallback() {
    return query -> OntologyFilterConnector.loadOntologyTerms(query, terminologyService);
  }

  public MultiSelectComboBox<OntologyTermV1> speciesBox() {

    MultiSelectComboBox<OntologyTermV1> box = newBox();
    box.setItems(speciesFetchCallback());

    box.setPlaceholder("Search and select one or more species for your samples");
    box.setLabel("Species");
    return box;
  }

  public MultiSelectComboBox<OntologyTermV1> specimenBox() {

    MultiSelectComboBox<OntologyTermV1> box = newBox();
    box.setItems(ontologyTermFetchCallback());

    box.setPlaceholder("Search and select one or more specimen for your samples");
    box.setLabel("Specimen");
    return box;
  }

  private static MultiSelectComboBox<OntologyTermV1> newBox() {
    MultiSelectComboBox<OntologyTermV1> box = new MultiSelectComboBox<>();
    box.setRequired(true);
    box.setHelperText("Please provide at least two letters to search for entries.");
    box.setRenderer(new ComponentRenderer<>(OntologyComponent::new));
    box.setItemLabelGenerator(OntologyComboboxFactory::ontologyItemFormatted);
    box.addClassName("chip-badge");
    box.addClassName("no-chevron");
    return box;
  }

  private static String ontologyItemFormatted(OntologyTermV1 ontologyTermV1) {
    String ontologyLinkName = ontologyTermV1.getOboId().replace("_", ":");
    return String.format("%s (%s)", ontologyTermV1.getLabel(), ontologyLinkName);
  }

}
