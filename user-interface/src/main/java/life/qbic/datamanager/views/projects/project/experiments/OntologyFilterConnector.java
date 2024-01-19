package life.qbic.datamanager.views.projects.project.experiments;

import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.SortDirection;
import java.util.List;
import java.util.stream.Stream;
import life.qbic.projectmanagement.application.OntologyClassEntity;
import life.qbic.projectmanagement.application.OntologyTermInformationService;
import life.qbic.projectmanagement.application.SortOrder;
import life.qbic.projectmanagement.domain.model.Ontology;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.OntologyClassDTO;

/**
 * Connects the OntologyTermInformationService to a Combobox of variable type, setting up a user-
 * provided filter converter between OntologyClassEntity and the Combobox type
 */
public class OntologyFilterConnector {

  private OntologyFilterConnector() {

  }

  public static Stream<OntologyClassDTO> loadOntologyTerms(List<Ontology> ontologies,
      Query<OntologyClassDTO, String> query,
      OntologyTermInformationService ontologyTermInformationService) {
    List<String> ontologyAbbreviations = ontologies.stream()
        .map(Ontology::getAbbreviation)
        .toList();
    List<SortOrder> sortOrders = query.getSortOrders().stream()
        .map(querySortOrder -> new SortOrder(querySortOrder.getSorted(),
            querySortOrder.getDirection().equals(SortDirection.DESCENDING)))
        .toList();
    List<OntologyClassEntity> ontologyClassEntities = ontologyTermInformationService
        .queryOntologyTerm(query.getFilter().orElse(""),
            ontologyAbbreviations,
            query.getOffset(),
            query.getLimit(),
            sortOrders);
    return ontologyClassEntities.stream().map(OntologyClassDTO::from).distinct();
  }

}
