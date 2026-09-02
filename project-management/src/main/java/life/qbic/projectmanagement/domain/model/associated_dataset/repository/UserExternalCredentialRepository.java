package life.qbic.projectmanagement.domain.model.associated_dataset.repository;

import java.util.List;
import java.util.Optional;
import life.qbic.projectmanagement.domain.model.associated_dataset.SourceType;
import life.qbic.projectmanagement.domain.model.associated_dataset.UserExternalCredential;

/**
 * Repository port for per-user external provider credentials.
 *
 * <p>Implemented by the infrastructure layer (JPA). The application layer
 * depends only on this interface.</p>
 *
 * <p>Queries are scoped by {@link SourceType} so that the application
 * service can look up credentials without knowing the storage details.</p>
 *
 * @since 1.12.0
 */
public interface UserExternalCredentialRepository {

  /**
   * Finds a credential for the given user, source type, and instance.
   *
   * @param userId     the DM user ID
   * @param sourceType the source type
   * @param instanceId the instance identifier
   * @return the credential, or empty if none is stored
   */
  Optional<UserExternalCredential> findByUserIdAndSourceTypeAndInstanceId(
      String userId, SourceType sourceType, String instanceId);

  /**
   * Finds all credentials for the given user, regardless of source type.
   *
   * @param userId the DM user ID
   * @return all stored credentials; never null (may be empty)
   */
  List<UserExternalCredential> findByUserId(String userId);

  /**
   * Finds all credentials for the given user and source type.
   *
   * @param userId     the DM user ID
   * @param sourceType the source type
   * @return all stored credentials of that type; never null (may be empty)
   */
  List<UserExternalCredential> findByUserIdAndSourceType(
      String userId, SourceType sourceType);

  /**
   * Persists a credential (insert or update).
   *
   * @param credential the credential to persist
   */
  void save(UserExternalCredential credential);

  /**
   * Removes a credential for the given user, source type, and instance.
   *
   * @param userId     the DM user ID
   * @param sourceType the source type
   * @param instanceId the instance identifier
   */
  void deleteByUserIdAndSourceTypeAndInstanceId(
      String userId, SourceType sourceType, String instanceId);

}
