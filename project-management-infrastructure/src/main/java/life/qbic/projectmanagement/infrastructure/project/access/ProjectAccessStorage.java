package life.qbic.projectmanagement.infrastructure.project.access;

import java.util.Objects;
import life.qbic.projectmanagement.application.authorization.SidDataStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Component
public class ProjectAccessStorage implements SidDataStorage {

  private final SidRepository sidRepository;

  @Autowired
  public ProjectAccessStorage(SidRepository sidRepository) {
    this.sidRepository = Objects.requireNonNull(sidRepository);
  }

  @Override
  public void addSid(String sid, boolean principal) {
    if (sidRepository.existsBySidEqualsIgnoreCaseAndPrincipalEquals(sid, principal)) {
      return;
    }
    sidRepository.save(new QBiCSid(principal, sid));
  }
}
