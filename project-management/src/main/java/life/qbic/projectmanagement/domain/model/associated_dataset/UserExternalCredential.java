package life.qbic.projectmanagement.domain.model.associated_dataset;

import static java.util.Objects.requireNonNull;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

/**
 * Per-user, per-instance credential for an external data provider.
 *
 * <p>This entity owns the encrypted token blob and the credential
 * status. The plaintext token never exists at this layer — it exists
 * only transiently in the infrastructure adapter during HTTP calls
 * (ADR-0002 D1 decryption boundary).</p>
 *
 * <p>The entity is source-agnostic at the domain boundary: it carries
 * a {@link SourceType} and an {@code instanceId}, consistent with the
 * {@link AssociatedDataset} aggregate design.</p>
 *
 * <p>Persistence convention: in this codebase, aggregate roots are
 * annotated directly with JPA annotations (see {@link AssociatedDataset}
 * for the reference pattern).</p>
 *
 * @since 1.12.0
 */
@Entity
@Table(name = "user_external_credential",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_user_src_instance",
        columnNames = {"user_id", "source_type", "instance_id"}))
public class UserExternalCredential {

  @Id
  @Column(name = "id", nullable = false, length = 36)
  private String id;

  @Column(name = "user_id", nullable = false, length = 255)
  private String userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "source_type", nullable = false, length = 32)
  private SourceType sourceType;

  @Column(name = "instance_id", nullable = false, length = 64)
  private String instanceId;

  @Column(name = "encrypted_token", nullable = false,
      columnDefinition = "varbinary(512)")
  private byte[] encryptedToken;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 16)
  private CredentialStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /**
   * No-arg constructor required by JPA. Not for application use.
   */
  protected UserExternalCredential() {
  }

  /**
   * Creates a new credential.
   *
   * @param userId         the DM user ID
   * @param sourceType     the external source type
   * @param instanceId     the instance identifier (matches
   *                       {@code InstanceConfig.id})
   * @param encryptedToken the AES-GCM-encrypted token blob
   *                       (nonce ‖ ciphertext ‖ tag)
   * @param status         the credential status
   */
  public UserExternalCredential(
      String userId,
      SourceType sourceType,
      String instanceId,
      byte[] encryptedToken,
      CredentialStatus status) {
    this.id = UUID.randomUUID().toString();
    this.userId = requireNonNull(userId, "userId must not be null");
    this.sourceType = requireNonNull(sourceType,
        "sourceType must not be null");
    this.instanceId = requireNonNull(instanceId,
        "instanceId must not be null");
    this.encryptedToken = requireNonNull(encryptedToken,
        "encryptedToken must not be null");
    this.status = requireNonNull(status, "status must not be null");
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  @PrePersist
  void onPrePersist() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
    updatedAt = Instant.now();
  }

  @PreUpdate
  void onPreUpdate() {
    updatedAt = Instant.now();
  }

  public String getId() {
    return id;
  }

  public String getUserId() {
    return userId;
  }

  public SourceType getSourceType() {
    return sourceType;
  }

  public String getInstanceId() {
    return instanceId;
  }

  /**
   * Returns a defensive copy of the encrypted token blob.
   * The plaintext token never exists at this layer.
   */
  public byte[] getEncryptedToken() {
    return Arrays.copyOf(encryptedToken, encryptedToken.length);
  }

  public CredentialStatus getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  /**
   * Transitions the credential status and updates the
   * {@code updatedAt} timestamp.
   *
   * @param newStatus the new status
   */
  public void transitionTo(CredentialStatus newStatus) {
    requireNonNull(newStatus, "newStatus must not be null");
    this.status = newStatus;
    this.updatedAt = Instant.now();
  }

}
