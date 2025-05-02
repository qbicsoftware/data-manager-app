package life.qbic.projectmanagement.application.contact;

import java.util.List;


/**
 * <b>API for querying the publicly available orcid entries via the orcid API</b>
 *
 */
public interface PersonSelect {

  List<OrcidEntry> findAll(String query, int limit, int offset);

}
