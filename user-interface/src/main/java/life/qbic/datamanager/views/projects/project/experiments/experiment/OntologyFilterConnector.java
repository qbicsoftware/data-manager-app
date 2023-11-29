package life.qbic.datamanager.views.projects.project.experiments.experiment;

import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.data.provider.SortDirection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import life.qbic.projectmanagement.application.OntologyClassEntity;
import life.qbic.projectmanagement.application.OntologyTermInformationService;
import life.qbic.projectmanagement.application.SortOrder;
import life.qbic.projectmanagement.domain.model.Ontology;

/**
 *
 */
public class OntologyFilterConnector {

  private final OntologyTermInformationService ontologyTermInformationService;

  public OntologyFilterConnector(OntologyTermInformationService ontologyTermInformationService) {
    this.ontologyTermInformationService = ontologyTermInformationService;
  }


  public <T> void initComboBoxWithOntologyDatasource(
      MultiSelectComboBox<T> box, List<Ontology> ontologies,
      Function<OntologyClassEntity, T> ontologyMapping) {

    box.setRequired(true);
    box.setHelperText("Please provide at least two letters to search for entries.");

    box.setItemsWithFilterConverter(
        query -> ontologyTermInformationService.queryOntologyTerm(query.getFilter().orElse(""),
            ontologies.stream().map(Ontology::getAbbreviation).toList(),
            query.getOffset(),
            query.getLimit(), query.getSortOrders().stream().map(
                    it -> new SortOrder(it.getSorted(), it.getDirection().equals(SortDirection.DESCENDING)))
                .collect(Collectors.toList())).stream().map(ontologyMapping),
        entity -> entity
    );
  }
}
