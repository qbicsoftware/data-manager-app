package life.qbic.projectmanagement.persistence.person;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface PersonRepo extends PagingAndSortingRepository<Person, Long> {

  Page<Person> findByFirstNameContainingIgnoreCaseOrLastNameIgnoreCase(
      String firstName, String lastName, Pageable pageable);

}
