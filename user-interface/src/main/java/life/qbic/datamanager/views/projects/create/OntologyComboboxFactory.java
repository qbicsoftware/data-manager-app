package life.qbic.datamanager.views.projects.create;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.data.provider.CallbackDataProvider.FetchCallback;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import java.util.List;
import life.qbic.datamanager.views.general.OntologyComponent;
import life.qbic.datamanager.views.projects.project.experiments.OntologyFilterConnector;
import life.qbic.projectmanagement.application.ontology.OntologyLookupService;
import life.qbic.projectmanagement.domain.model.Ontology;
import life.qbic.projectmanagement.domain.model.OntologyTerm;

/**
 * Factory class for creating MultiSelectComboBox instances for different ontology types. It
 * provides methods to create comboboxes for analytes, species, and specimens.
 */
public class OntologyComboboxFactory {

  private final OntologyLookupService ontologyLookupService;

  public OntologyComboboxFactory(OntologyLookupService ontologyLookupService) {
    this.ontologyLookupService = requireNonNull(ontologyLookupService,
        "ontologyTermInformationService must not be null");
  }

  public MultiSelectComboBox<OntologyTerm> analyteBox() {
    List<Ontology> analyteOntologies = List.of(Ontology.BIOASSAY_ONTOLOGY);

    MultiSelectComboBox<OntologyTerm> box = newBox();
    box.setItems(ontologyFetchCallback(analyteOntologies));

    box.setPlaceholder("Search and select one or more analytes for your samples");
    box.setLabel("Analytes");
    box.addClassName("full-width-input");
    return box;
  }

  private FetchCallback<OntologyTerm, String> ontologyFetchCallback(
      List<Ontology> ontologies) {
    return query -> OntologyFilterConnector.loadOntologyTerms(ontologies, query,
        ontologyLookupService);
  }

  public MultiSelectComboBox<OntologyTerm> speciesBox() {
    List<Ontology> speciesOntologies = List.of(Ontology.NCBI_TAXONOMY);

    MultiSelectComboBox<OntologyTerm> box = newBox();
    box.setItems(ontologyFetchCallback(speciesOntologies));

    box.setPlaceholder("Search and select one or more species for your samples");
    box.setLabel("Species");
    return box;
  }

  public MultiSelectComboBox<OntologyTerm> specimenBox() {
    List<Ontology> specimenOntologies = List.of(Ontology.PLANT_ONTOLOGY,
        Ontology.BRENDA_TISSUE_ONTOLOGY);

    MultiSelectComboBox<OntologyTerm> box = newBox();
    box.setItems(ontologyFetchCallback(specimenOntologies));

    box.setPlaceholder("Search and select one or more specimen for your samples");
    box.setLabel("Specimen");
    return box;
  }

  private static MultiSelectComboBox<OntologyTerm> newBox() {
    MultiSelectComboBox<OntologyTerm> box = new MultiSelectComboBox<>();
    box.setRequired(true);
    box.setHelperText("Please provide at least two letters to search for entries.");
    box.setRenderer(new ComponentRenderer<>(OntologyComponent::new));
    box.setItemLabelGenerator(OntologyComboboxFactory::ontologyItemFormatted);
    box.addClassName("chip-badge");
    box.addClassName("no-chevron");
    return box;
  }

  private static String ontologyItemFormatted(OntologyTerm ontologyTerm) {
    String ontologyLinkName = ontologyTerm.getName().replace("_", ":");
    return String.format("%s (%s)", ontologyTerm.getLabel(), ontologyLinkName);
  }

}

