package life.qbic.usermanagement.persistence;

import java.util.List;
import life.qbic.usermanagement.User;
import org.springframework.data.repository.CrudRepository;

public interface QbicUserRepo extends CrudRepository<User, String> {
  List<User> findUsersByEmail(String email);

  User findUserById(String id);
}
