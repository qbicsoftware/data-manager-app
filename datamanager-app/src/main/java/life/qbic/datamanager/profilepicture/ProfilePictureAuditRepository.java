package life.qbic.datamanager.profilepicture;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import life.qbic.datamanager.profilepicture.ProfilePictureAuditRepository.ProfilePictureAuditEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

/**
 * Append-only audit trail for profile-picture changes.
 *
 * <p>Only <b>set</b> and <b>replace</b> events are recorded (as agreed for the feature); removal
 * and administrative force-removal do not write audit rows. The list is exposed to system
 * administrators only, enforced at the application service.</p>
 *
 * @since 1.22.0
 */
@org.springframework.stereotype.Repository
public interface ProfilePictureAuditRepository extends
    Repository<ProfilePictureAuditEntity, Long> {

  ProfilePictureAuditEntity save(ProfilePictureAuditEntity entity);

  Page<ProfilePictureAuditEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

  List<ProfilePictureAuditEntity> findByOwnerTypeAndOwnerIdOrderByCreatedAtDesc(
      ProfilePictureOwnerType ownerType, String ownerId);

  /**
   * One recorded profile-picture set/replace event.
   */
  @Entity
  @Table(name = "profile_picture_audit")
  class ProfilePictureAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 16)
    private ProfilePictureOwnerType ownerType;

    @Column(name = "owner_id", nullable = false, length = 255)
    private String ownerId;

    @Column(name = "action", nullable = false, length = 16)
    private String action;

    @Column(name = "actor_id", nullable = false, length = 255)
    private String actorId;

    @Column(name = "previous_content_hash", length = 64)
    private String previousContentHash;

    @Column(name = "new_content_hash", nullable = false, length = 64)
    private String newContentHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ProfilePictureAuditEntity() {
      // for JPA
    }

    public ProfilePictureAuditEntity(ProfilePictureOwnerType ownerType, String ownerId,
        String action, String actorId, String previousContentHash, String newContentHash,
        Instant createdAt) {
      this.ownerType = ownerType;
      this.ownerId = ownerId;
      this.action = action;
      this.actorId = actorId;
      this.previousContentHash = previousContentHash;
      this.newContentHash = newContentHash;
      this.createdAt = createdAt;
    }

    public Long getId() {
      return id;
    }

    public ProfilePictureOwnerType getOwnerType() {
      return ownerType;
    }

    public String getOwnerId() {
      return ownerId;
    }

    public String getAction() {
      return action;
    }

    public String getActorId() {
      return actorId;
    }

    public String getPreviousContentHash() {
      return previousContentHash;
    }

    public String getNewContentHash() {
      return newContentHash;
    }

    public Instant getCreatedAt() {
      return createdAt;
    }
  }
}
