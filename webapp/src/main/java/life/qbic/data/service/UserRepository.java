package life.qbic.data.service;

import java.util.UUID;
import life.qbic.data.entity.TestUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<TestUser, UUID> {

    TestUser findByUsername(String username);
}
