package life.qbic.datamanager.views.projects.project.experiments;

import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.SortDirection;
import java.util.List;
import java.util.stream.Stream;
import life.qbic.application.commons.SortOrder;
import life.qbic.projectmanagement.application.ontology.OntologyClass;
import life.qbic.projectmanagement.application.ontology.SpeciesLookupService;
import life.qbic.projectmanagement.application.ontology.TerminologyService;
import life.qbic.projectmanagement.domain.model.Ontology;
import life.qbic.projectmanagement.domain.model.OntologyTerm;

/**
 * Connects the OntologyTermInformationService to a Combobox of variable type, setting up a user-
 * provided filter converter between OntologyClass and the Combobox type
 */
public class OntologyFilterConnector {

  private OntologyFilterConnector() {

  }

  public static Stream<OntologyTerm> loadOntologyTerms(Query<OntologyTerm, String> query,
      TerminologyService terminologyService) {
    return terminologyService.query(query.getFilter().orElse(""), query.getOffset(),
        query.getLimit()).stream().distinct();
  }

  public static Stream<OntologyTerm> loadOntologyTerms(
      Query<OntologyTerm, String> query,
      SpeciesLookupService ontologyTermInformationService) {
    List<SortOrder> sortOrders = query.getSortOrders().stream()
        .map(querySortOrder -> new SortOrder(querySortOrder.getSorted(),
            querySortOrder.getDirection().equals(SortDirection.DESCENDING)))
        .toList();
    List<OntologyClass> ontologyClassEntities = ontologyTermInformationService
        .queryOntologyTerm(query.getFilter().orElse(""),
            query.getOffset(),
            query.getLimit(),
            sortOrders);
    return ontologyClassEntities.stream().map(OntologyTerm::from).distinct();
  }

}
