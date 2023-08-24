package life.qbic.authentication.persistence;

import org.springframework.data.repository.CrudRepository;

/**
 * Repository to manipulate sid entries
 */
public interface SidRepository extends CrudRepository<QBiCSid, Long> {

  boolean existsBySidEqualsIgnoreCaseAndPrincipalEquals(String sid, boolean principal);

}
