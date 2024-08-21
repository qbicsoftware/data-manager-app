package life.qbic.projectmanagement.application.ontology;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.ApplicationException.ErrorCode;
import life.qbic.application.commons.ApplicationException.ErrorParameters;
import life.qbic.projectmanagement.domain.model.OntologyTerm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <b>Terminology Service</b>
 *
 * <p>Alternative terminology lookup that is supposed to integrate external resource.</p>
 *
 * @since 1.4.0
 */
@Service
public class TerminologyService {

  private final TerminologySelect terminologySelect;

  @Autowired
  public TerminologyService(TerminologySelect terminologySelect) {
    this.terminologySelect = Objects.requireNonNull(terminologySelect);
  }

  /**
   * Queries possible matching ontology terms given the provided search term. This method should be
   * used by clients that want to support an auto-suggest use case. The implementation needs to
   * optimise for fast lookup times.
   * <p>
   * <b>NOTE!</b>
   * <p>
   * The resulting {@link OntologyTerm} objects are not guaranteed to contain values for properties
   * like "description". This depends highly on the implementation, use the
   * {@link TerminologySelect#search(String, int, int)} method for a rich search.
   *
   * @param searchTerm a full or partial term that the client wants to select from potentials search
   *                   hits
   * @param offset     0 for starting the listing from the beginning of all possible matches, or
   *                   slice through the results with an offset
   * @param limit      the maximum number of matches returned per search
   * @return a list of matching terms given the provided search term
   * @since 1.4.0
   */
  public List<OntologyTerm> query(String searchTerm, int offset, int limit)
      throws ApplicationException {
    try {
      return terminologySelect.query(searchTerm, offset, limit).stream().map(OntologyTerm::from)
          .toList();
    } catch (LookupException e) {
      throw new ApplicationException(ErrorCode.SERVICE_FAILED, ErrorParameters.empty());
    }

  }

  /**
   * Queries possible matching single ontology term given a provided CURIE, such as the OBO ID.
   *
   * @param curie the CURIE of the term to search for
   * @return a list of matching terms given the provided CURIE
   * @since 1.4.0
   */
  public Optional<OntologyTerm> findByCurie(String curie) throws ApplicationException {
    try {
      return terminologySelect.searchByCurie(curie).map(OntologyTerm::from);
    } catch (LookupException e) {
      throw new ApplicationException(ErrorCode.SERVICE_FAILED, ErrorParameters.empty());
    }
  }

  /**
   * Searches for possible matching ontology terms. This search returns rich {@link OntologyTerm}
   * objects, and should be used when information for properties like "description" needs to be
   * used.
   *
   * @param searchTerm a full or partial term that the client wants to select from potentials search
   *                   hits
   * @param offset     0 for starting the listing from the beginning of all possible matches, or
   *                   slice through the results with an offset
   * @param limit      the maximum number of matches returned per search
   * @return a list of matching terms given the provided search term.
   * @since 1.4.0
   */
  public List<OntologyTerm> search(String searchTerm, int offset, int limit)
      throws ApplicationException {
    try {
      return terminologySelect.search(searchTerm, offset, limit).stream().map(OntologyTerm::from)
          .toList();
    } catch (LookupException e) {
      throw new ApplicationException(ErrorCode.SERVICE_FAILED, ErrorParameters.empty());
    }
  }

}
