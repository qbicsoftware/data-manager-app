package life.qbic.datamanager.views.projects.create;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.data.provider.CallbackDataProvider.FetchCallback;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import java.util.List;
import life.qbic.datamanager.views.general.OntologyComponent;
import life.qbic.datamanager.views.projects.project.experiments.OntologyFilterConnector;
import life.qbic.projectmanagement.application.OntologyTermInformationService;
import life.qbic.projectmanagement.domain.model.Ontology;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.OntologyClassDTO;

/**
 * Factory class for creating MultiSelectComboBox instances for different ontology types. It
 * provides methods to create comboboxes for analytes, species, and specimens.
 */
public class OntologyComboboxFactory {

  private final OntologyTermInformationService ontologyTermInformationService;
  private static final String[] BOX_CLASSES = {"chip-badge", "full-width-input"};

  public OntologyComboboxFactory(OntologyTermInformationService ontologyTermInformationService) {
    this.ontologyTermInformationService = requireNonNull(ontologyTermInformationService,
        "ontologyTermInformationService must not be null");
  }

  public MultiSelectComboBox<OntologyClassDTO> analyteBox() {
    List<Ontology> analyteOntologies = List.of(Ontology.BIOASSAY_ONTOLOGY);

    MultiSelectComboBox<OntologyClassDTO> box = newBox();
    box.setItems(ontologyFetchCallback(analyteOntologies));

    box.setPlaceholder("Please select one or more analytes for your samples");
    box.setLabel("Analytes");
    return box;
  }

  private FetchCallback<OntologyClassDTO, String> ontologyFetchCallback(
      List<Ontology> ontologies) {
    return query -> OntologyFilterConnector.loadOntologyTerms(ontologies, query,
        ontologyTermInformationService);
  }

  public MultiSelectComboBox<OntologyClassDTO> speciesBox() {
    List<Ontology> speciesOntologies = List.of(Ontology.NCBI_TAXONOMY);

    MultiSelectComboBox<OntologyClassDTO> box = newBox();
    box.setItems(ontologyFetchCallback(speciesOntologies));

    box.setPlaceholder("Please select one or more species for your samples");
    box.setLabel("Species");
    return box;
  }

  public MultiSelectComboBox<OntologyClassDTO> specimenBox() {
    List<Ontology> specimenOntologies = List.of(Ontology.PLANT_ONTOLOGY,
        Ontology.BRENDA_TISSUE_ONTOLOGY);

    MultiSelectComboBox<OntologyClassDTO> box = newBox();
    box.setItems(ontologyFetchCallback(specimenOntologies));

    box.setPlaceholder("Please select one or more specimen for your samples");
    box.setLabel("Specimen");
    return box;
  }

  private static MultiSelectComboBox<OntologyClassDTO> newBox() {
    MultiSelectComboBox<OntologyClassDTO> box = new MultiSelectComboBox<>();
    box.setRequired(true);
    box.setHelperText("Please provide at least two letters to search for entries.");
    box.setRenderer(new ComponentRenderer<>(OntologyComponent::new));
    box.setItemLabelGenerator(OntologyClassDTO::getLabel);
    box.addClassNames(BOX_CLASSES);
    return box;
  }
}
