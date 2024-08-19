package life.qbic.projectmanagement.application.ontology;

import java.util.List;
import java.util.Objects;
import life.qbic.projectmanagement.domain.model.OntologyTerm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Service
public class TerminologyService {

  private final TerminologySelect terminologySelect;

  @Autowired
  public TerminologyService(TerminologySelect terminologySelect) {
    this.terminologySelect = Objects.requireNonNull(terminologySelect);
  }

  public List<OntologyTerm> query(String searchTerm, int offset, int limit) {
    return terminologySelect.query(searchTerm, offset, limit);
  }

}
