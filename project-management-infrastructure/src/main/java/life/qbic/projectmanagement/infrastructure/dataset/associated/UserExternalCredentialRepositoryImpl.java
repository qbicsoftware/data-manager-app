package life.qbic.projectmanagement.infrastructure.dataset.associated;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import life.qbic.projectmanagement.domain.model.associated_dataset.SourceType;
import life.qbic.projectmanagement.domain.model.associated_dataset.UserExternalCredential;
import life.qbic.projectmanagement.domain.model.associated_dataset.repository.UserExternalCredentialRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Domain repository adapter for {@link UserExternalCredential}.
 *
 * <p>Wraps {@link UserExternalCredentialJpaRepository} and delegates
 * persistence, exposing the domain-level semantics defined by
 * {@link UserExternalCredentialRepository}.</p>
 *
 * @since 1.12.0
 */
@Repository
public class UserExternalCredentialRepositoryImpl
    implements UserExternalCredentialRepository {

  private final UserExternalCredentialJpaRepository jpaRepository;

  public UserExternalCredentialRepositoryImpl(
      UserExternalCredentialJpaRepository jpaRepository) {
    this.jpaRepository = Objects.requireNonNull(jpaRepository,
        "jpaRepository must not be null");
  }

  @Override
  public Optional<UserExternalCredential> findByUserIdAndSourceTypeAndInstanceId(
      String userId, SourceType sourceType, String instanceId) {
    Objects.requireNonNull(userId, "userId must not be null");
    Objects.requireNonNull(sourceType, "sourceType must not be null");
    Objects.requireNonNull(instanceId, "instanceId must not be null");
    return jpaRepository.findByUserIdAndSourceTypeAndInstanceId(
        userId, sourceType, instanceId);
  }

  @Override
  public List<UserExternalCredential> findByUserId(String userId) {
    Objects.requireNonNull(userId, "userId must not be null");
    return jpaRepository.findByUserId(userId);
  }

  @Override
  public List<UserExternalCredential> findByUserIdAndSourceType(
      String userId, SourceType sourceType) {
    Objects.requireNonNull(userId, "userId must not be null");
    Objects.requireNonNull(sourceType, "sourceType must not be null");
    return jpaRepository.findByUserIdAndSourceType(userId, sourceType);
  }

  @Override
  @Transactional
  public void save(UserExternalCredential credential) {
    Objects.requireNonNull(credential, "credential must not be null");
    jpaRepository.save(credential);
  }

  @Override
  @Transactional
  public void deleteByUserIdAndSourceTypeAndInstanceId(
      String userId, SourceType sourceType, String instanceId) {
    Objects.requireNonNull(userId, "userId must not be null");
    Objects.requireNonNull(sourceType, "sourceType must not be null");
    Objects.requireNonNull(instanceId, "instanceId must not be null");
    jpaRepository.deleteByUserIdAndSourceTypeAndInstanceId(
        userId, sourceType, instanceId);
  }

}
