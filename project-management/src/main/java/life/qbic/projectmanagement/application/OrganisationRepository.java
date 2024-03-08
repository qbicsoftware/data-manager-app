package life.qbic.projectmanagement.application;

import java.util.Optional;
import life.qbic.projectmanagement.domain.Organisation;

/**
 * <b><interface short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public interface OrganisationRepository {

  Optional<Organisation> resolve(String iri);

}
