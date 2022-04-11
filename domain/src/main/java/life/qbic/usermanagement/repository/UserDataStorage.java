package life.qbic.usermanagement.repository;

import java.util.List;
import life.qbic.usermanagement.User;

public interface UserDataStorage {

  List<User> findUsersByEmail(String email);

}
