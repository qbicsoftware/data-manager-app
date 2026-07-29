package life.qbic.projectmanagement.infrastructure.dataset.associated;

import java.util.List;
import java.util.Optional;
import life.qbic.projectmanagement.domain.model.associated_dataset.SourceType;
import life.qbic.projectmanagement.domain.model.associated_dataset.UserExternalCredential;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link UserExternalCredential} entities.
 *
 * @since 1.12.0
 */
interface UserExternalCredentialJpaRepository
    extends JpaRepository<UserExternalCredential, String> {

  Optional<UserExternalCredential> findByUserIdAndSourceTypeAndInstanceId(
      String userId, SourceType sourceType, String instanceId);

  List<UserExternalCredential> findByUserId(String userId);

  List<UserExternalCredential> findByUserIdAndSourceType(
      String userId, SourceType sourceType);

  void deleteByUserIdAndSourceTypeAndInstanceId(
      String userId, SourceType sourceType, String instanceId);

}
