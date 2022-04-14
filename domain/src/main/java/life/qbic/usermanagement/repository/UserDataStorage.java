package life.qbic.usermanagement.repository;

import java.util.List;
import java.util.Optional;
import life.qbic.usermanagement.User;

public interface UserDataStorage {

  List<User> findUsersByEmail(String email);

  void save(User user);

  Optional<User> findUserById(String id);
}
