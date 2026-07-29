package life.qbic.projectmanagement.application.associated_dataset;

import static java.util.Objects.requireNonNull;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import life.qbic.projectmanagement.domain.model.associated_dataset.CredentialStatus;
import life.qbic.projectmanagement.domain.model.associated_dataset.SourceType;
import life.qbic.projectmanagement.domain.model.associated_dataset.UserExternalCredential;
import life.qbic.projectmanagement.domain.model.associated_dataset.repository.UserExternalCredentialRepository;

/**
 * Default implementation of {@link ExternalCredentialService}.
 *
 * <p>Orchestrates token validation, encryption, and persistence.
 * The plaintext token is accepted as a {@code char[]} from the view
 * layer and is zeroed in a {@code finally} block before the method
 * returns — regardless of success or failure.</p>
 *
 * @since 1.12.0
 */
public class DefaultExternalCredentialService implements ExternalCredentialService {

  private final ExternalCredentialValidator validator;
  private final UserExternalCredentialRepository credentialRepository;
  private final CredentialEncryptor encryptor;
  private final SourceInstanceRegistry instanceRegistry;

  public DefaultExternalCredentialService(
      ExternalCredentialValidator validator,
      UserExternalCredentialRepository credentialRepository,
      CredentialEncryptor encryptor,
      SourceInstanceRegistry instanceRegistry) {
    this.validator = requireNonNull(validator, "validator must not be null");
    this.credentialRepository = requireNonNull(credentialRepository,
        "credentialRepository must not be null");
    this.encryptor = requireNonNull(encryptor, "encryptor must not be null");
    this.instanceRegistry = requireNonNull(instanceRegistry,
        "instanceRegistry must not be null");
  }

  @Override
  public AddCredentialResult addCredential(String userId, String instanceId,
      char[] token) {
    requireNonNull(userId, "userId must not be null");
    requireNonNull(instanceId, "instanceId must not be null");
    requireNonNull(token, "token must not be null");

    try {
      // 1. Resolve the instance + source type from the registry
      Optional<SourceInstanceDescriptor> descriptor =
          instanceRegistry.find(instanceId);
      if (descriptor.isEmpty()) {
        return new UnknownInstance(instanceId);
      }
      SourceInstanceDescriptor instance = descriptor.get();
      InstanceConfig config = instance.toInstanceConfig();
      SourceType sourceType = instance.sourceType();

      // 2. Validate the token against the external instance
      boolean valid;
      try {
        valid = validator.validateToken(sourceType, config, token);
      } catch (CredentialValidationException e) {
        return new ServiceError(
            "Token validation could not be completed: " + e.getMessage());
      }
      if (!valid) {
        return new InvalidToken(
            "The token was rejected by " + instance.displayName());
      }

      // 3. Encrypt and persist
      byte[] encryptedToken = encryptor.encrypt(token);
      Optional<UserExternalCredential> existing =
          credentialRepository.findByUserIdAndSourceTypeAndInstanceId(
              userId, sourceType, instanceId);

      if (existing.isPresent()) {
        // Replace: update the existing credential
        UserExternalCredential cred = existing.get();
        cred.transitionTo(CredentialStatus.VALID);
        // The entity holds encryptedToken as a field — create a new
        // credential with updated token blob
        credentialRepository.deleteByUserIdAndSourceTypeAndInstanceId(
            userId, sourceType, instanceId);
      }

      UserExternalCredential newCredential = new UserExternalCredential(
          userId, sourceType, instanceId, encryptedToken,
          CredentialStatus.VALID);
      credentialRepository.save(newCredential);

      return new Success();
    } finally {
      Arrays.fill(token, '\0');
    }
  }

  @Override
  public boolean removeCredential(String userId, String instanceId) {
    requireNonNull(userId, "userId must not be null");
    requireNonNull(instanceId, "instanceId must not be null");

    // Check if the credential exists for any source type at this instance
    Optional<SourceInstanceDescriptor> descriptor =
        instanceRegistry.find(instanceId);
    if (descriptor.isEmpty()) {
      return false;
    }
    SourceType sourceType = descriptor.get().sourceType();

    Optional<UserExternalCredential> existing =
        credentialRepository.findByUserIdAndSourceTypeAndInstanceId(
            userId, sourceType, instanceId);
    if (existing.isEmpty()) {
      return false;
    }
    credentialRepository.deleteByUserIdAndSourceTypeAndInstanceId(
        userId, sourceType, instanceId);
    return true;
  }

  @Override
  public List<CredentialStatusView> listCredentialStatuses(String userId) {
    requireNonNull(userId, "userId must not be null");

    List<SourceInstanceDescriptor> allInstances =
        instanceRegistry.findBySourceType(SourceType.INVENIO_RDM);

    return allInstances.stream()
        .map(instance -> {
          boolean configured = credentialRepository
              .findByUserIdAndSourceTypeAndInstanceId(
                  userId, instance.sourceType(), instance.id())
              .isPresent();

          String status;
          if (!configured) {
            status = "NOT_CONFIGURED";
          } else {
            // Find the actual status
            status = credentialRepository
                .findByUserIdAndSourceTypeAndInstanceId(
                    userId, instance.sourceType(), instance.id())
                .map(c -> c.getStatus().name())
                .orElse("NOT_CONFIGURED");
          }

          return new CredentialStatusView(
              instance.sourceType(),
              instance.id(),
              instance.displayName(),
              configured,
              status);
        })
        .toList();
  }

}
