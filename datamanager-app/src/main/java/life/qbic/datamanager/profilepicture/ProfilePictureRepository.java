package life.qbic.datamanager.profilepicture;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import life.qbic.datamanager.profilepicture.ProfilePictureRepository.ProfilePictureEntity;
import org.springframework.data.repository.Repository;

/**
 * Persistence port for normalized profile-picture derivatives.
 *
 * <p>The entity is colocated with its repository, mirroring
 * {@code life.qbic.datamanager.announcements.AnnouncementRepository}. The blob is intentionally
 * isolated in its own table ({@code profile_picture}) so it never inflates the hot
 * {@code users} / {@code user_group} rows.</p>
 *
 * <p>Keyed by {@code (owner_type, owner_id)} with <b>no foreign keys</b> into {@code users} or
 * {@code user_group}: profile pictures are a presentation concern shared by two bounded contexts
 * and must not create a schema dependency between them.</p>
 *
 * @since 1.22.0
 */
@org.springframework.stereotype.Repository
public interface ProfilePictureRepository extends Repository<ProfilePictureEntity, Long> {

  java.util.Optional<ProfilePictureEntity> findByOwnerTypeAndOwnerId(
      ProfilePictureOwnerType ownerType, String ownerId);

  /**
   * Reads only the content hash for an owner, without loading the blob. Used by the avatar URL
   * resolver on hot render paths.
   *
   * <p>A (closed) projection derived query is used instead of a JPQL {@code @Query}, because the
   * entity is a nested class and its Hibernate entity name is not the simple class name.</p>
   */
  java.util.Optional<ContentHashProjection> findContentHashByOwnerTypeAndOwnerId(
      ProfilePictureOwnerType ownerType, String ownerId);

  ProfilePictureEntity save(ProfilePictureEntity entity);

  void deleteByOwnerTypeAndOwnerId(ProfilePictureOwnerType ownerType, String ownerId);

  /**
   * Closed projection exposing only the content hash.
   */
  interface ContentHashProjection {

    String getContentHash();
  }

  /**
   * The stored, normalized derivative of one owner's profile picture.
   */
  @Entity
  @Table(name = "profile_picture",
      uniqueConstraints = @UniqueConstraint(name = "uk_profile_picture_owner",
          columnNames = {"owner_type", "owner_id"}))
  class ProfilePictureEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 16)
    private ProfilePictureOwnerType ownerType;

    @Column(name = "owner_id", nullable = false, length = 255)
    private String ownerId;

    @Column(name = "content_type", nullable = false, length = 32)
    private String contentType;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "width", nullable = false)
    private int width;

    @Column(name = "height", nullable = false)
    private int height;

    @Column(name = "data", nullable = false, columnDefinition = "mediumblob")
    private byte[] data;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", nullable = false, length = 255)
    private String updatedBy;
    protected ProfilePictureEntity() {
      // for JPA
    }

    public ProfilePictureEntity(ProfilePictureOwnerType ownerType, String ownerId,
        String contentType, String contentHash, int width, int height, byte[] data,
        Instant updatedAt, String updatedBy) {
      this.ownerType = ownerType;
      this.ownerId = ownerId;
      this.contentType = contentType;
      this.contentHash = contentHash;
      this.width = width;
      this.height = height;
      this.data = data;
      this.updatedAt = updatedAt;
      this.updatedBy = updatedBy;
    }

    public Long getId() {
      return id;
    }

    /**
     * Replaces the stored derivative in place (keeps the row id, so the 1:1 owner key is stable).
     */
    public void update(String contentType, String contentHash, int width, int height, byte[] data,
        Instant updatedAt, String updatedBy) {
      this.contentType = contentType;
      this.contentHash = contentHash;
      this.width = width;
      this.height = height;
      this.data = data;
      this.updatedAt = updatedAt;
      this.updatedBy = updatedBy;
    }

    public ProfilePictureOwnerType getOwnerType() {
      return ownerType;
    }

    public String getOwnerId() {
      return ownerId;
    }

    public String getContentType() {
      return contentType;
    }

    public String getContentHash() {
      return contentHash;
    }

    public int getWidth() {
      return width;
    }

    public int getHeight() {
      return height;
    }

    public byte[] getData() {
      return data;
    }

    public Instant getUpdatedAt() {
      return updatedAt;
    }

    public String getUpdatedBy() {
      return updatedBy;
    }
  }
}