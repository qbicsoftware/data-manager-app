package life.qbic.datamanager.views.projects.project.experiments.experiment;

import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import java.util.List;
import java.util.stream.Collectors;
import life.qbic.datamanager.views.general.OntologyComponent;
import life.qbic.projectmanagement.application.OntologyTermInformationService;
import life.qbic.projectmanagement.application.SortOrder;
import life.qbic.projectmanagement.domain.model.Ontology;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.OntologyClassDTO;
import com.vaadin.flow.data.renderer.Renderer;

/**
 * Connects the OntologyTermInformationService to a Combobox of variable type, setting up a user-
 * provided filter converter between OntologyClassEntity and the Combobox type
 */
public class OntologyFilterConnector {

  private final OntologyTermInformationService ontologyTermInformationService;

  public OntologyFilterConnector(OntologyTermInformationService ontologyTermInformationService) {
    this.ontologyTermInformationService = ontologyTermInformationService;
  }


  /**
   *
   * @param box             The ComboBox that will allow selection of database terms
   * @param ontologies      A list of Ontologies whose terms should be shown in the ComboBox
   */
  public void initComboBoxWithOntologyDatasource(
      MultiSelectComboBox<OntologyClassDTO> box, List<Ontology> ontologies) {

    box.setRequired(true);
    box.setHelperText("Please provide at least two letters to search for entries.");

    box.setItemsWithFilterConverter(
        query -> ontologyTermInformationService.queryOntologyTerm(query.getFilter().orElse(""),
            ontologies.stream().map(Ontology::getAbbreviation).toList(),
            query.getOffset(),
            query.getLimit(), query.getSortOrders().stream().map(
                    it -> new SortOrder(it.getSorted(), it.getDirection().equals(SortDirection.DESCENDING)))
                .collect(Collectors.toList())).stream().map(entity -> OntologyClassDTO.from(entity)),
        entity -> entity
    );
    box.setRenderer(ontologyComponentRenderer());
  }

  private Renderer<OntologyClassDTO> ontologyComponentRenderer() {
      return new ComponentRenderer<OntologyComponent, OntologyClassDTO>(ontologyClassDTO ->
        new OntologyComponent(ontologyClassDTO) );
    }

}
