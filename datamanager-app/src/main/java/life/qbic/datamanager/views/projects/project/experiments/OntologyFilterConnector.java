package life.qbic.datamanager.views.projects.project.experiments;

import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.SortDirection;
import java.util.List;
import java.util.stream.Stream;
import life.qbic.application.commons.SortOrder;
import life.qbic.projectmanagement.application.ontology.OntologyClass;
import life.qbic.projectmanagement.application.ontology.SpeciesLookupService;
import life.qbic.projectmanagement.application.ontology.TerminologyService;
import life.qbic.projectmanagement.domain.model.OntologyTerm;

/**
 * Connects the OntologyTermInformationService to a Combobox of variable type, setting up a user-
 * provided filter converter between OntologyClass and the Combobox type
 *
 * <p><b>Minimum query length.</b> Both backing services resolve the query term themselves, and one
 * of them ({@link TerminologyService}) is <em>cross-ontology</em>: it does not know which ontology
 * the user means, so the query term is the only scoping there is. A blank query therefore does not
 * mean "show everything" — it means an unbounded scan across all of OLS, whose first page is
 * whichever terms happen to sort first rather than anything the user asked for. Species is the
 * exception: {@link SpeciesLookupService} pins its query to NCBITaxon, so it has a scope of its own.
 *
 * <p>The user-facing copy has always asked for at least two letters ("Type at least two letters to
 * search"). That was documentation, not enforcement: an empty or one-letter filter was passed
 * straight through. Enforcing it here makes the copy true and keeps the cost of opening a field to
 * a keystroke instead of a request, which in turn lets the field show its dropdown affordance
 * without inviting a wasted round-trip.
 *
 * <p><b>Contract note.</b> Vaadin's {@code DataCommunicator} verifies that a fetch callback read
 * the query's limit and offset, whatever the callback returns — the trace is on the <em>calls</em>,
 * not on the result. Returning an empty stream before touching them throws
 * {@code IllegalStateException: The data provider hasn't ever called getLimit() or getPageSize()
 * method on the provided query}. So both bounds are read up front, before the guard can return
 * early; they are then used for the real query as well.
 */
public class OntologyFilterConnector {

  /**
   * Fewer characters than this are not a query. Matches the "at least two letters" the form copy
   * promises; keep the two in step if either changes.
   */
  public static final int MINIMUM_QUERY_LENGTH = 2;

  private OntologyFilterConnector() {

  }

  public static Stream<OntologyTerm> loadOntologyTerms(Query<OntologyTerm, String> query,
      TerminologyService terminologyService) {
    // Read both bounds before any early return: the data provider contract requires these calls to
    // happen on every fetch, even one that yields nothing.
    int offset = query.getOffset();
    int limit = query.getLimit();
    if (!isSearchable(query)) {
      return Stream.empty();
    }
    return terminologyService.query(query.getFilter().orElse(""), offset, limit).stream()
        .distinct();
  }

  public static Stream<OntologyTerm> loadOntologyTerms(
      Query<OntologyTerm, String> query,
      SpeciesLookupService ontologyTermInformationService) {
    // See the contract note on this class: both bounds are read before the guard can return.
    int offset = query.getOffset();
    int limit = query.getLimit();
    if (!isSearchable(query)) {
      return Stream.empty();
    }
    List<SortOrder> sortOrders = query.getSortOrders().stream()
        .map(querySortOrder -> new SortOrder(querySortOrder.getSorted(),
            querySortOrder.getDirection().equals(SortDirection.DESCENDING)))
        .toList();
    List<OntologyClass> ontologyClassEntities = ontologyTermInformationService
        .queryOntologyTerm(query.getFilter().orElse(""), offset, limit, sortOrders);
    return ontologyClassEntities.stream().map(OntologyTerm::from).distinct();
  }

  /**
   * Whether the query term is long enough to be worth sending. A blank filter is what
   * {@code Query#getFilter()} yields for an untouched or cleared field, so both cases are covered.
   */
  private static boolean isSearchable(Query<?, String> query) {
    return query.getFilter()
        .map(String::trim)
        .filter(term -> term.length() >= MINIMUM_QUERY_LENGTH)
        .isPresent();
  }
}
