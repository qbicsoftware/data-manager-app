package life.qbic.datamanager.profilepicture;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.ApplicationException.ErrorCode;
import life.qbic.application.commons.ApplicationException.ErrorParameters;
import life.qbic.application.commons.Result;
import life.qbic.datamanager.profilepicture.ProfilePictureRepository.ProfilePictureEntity;
import life.qbic.usergroups.api.GroupAdministrationPermission;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for profile pictures of users and user groups.
 *
 * <p>Owns validation, normalization, persistence, the audit trail and authorization. The image is
 * a presentation concern, so it never enters the identity {@code User} or user-groups
 * {@code UserGroup} aggregates.</p>
 *
 * <p><b>Authorization.</b></p>
 * <ul>
 *   <li>User picture: the user themselves only (never an admin, never another user).</li>
 *   <li>Group picture: delegated to {@link GroupPictureAuthorization} — ad-hoc OWNER/MANAGER, or
 *   a system administrator for organisational groups.</li>
 *   <li>Read: any authenticated user (enforced by the delivery endpoint, not here).</li>
 *   <li>Force-remove and audit list: system administrators only.</li>
 * </ul>
 *
 * <p>Expected failures are returned as {@link Result} errors, never thrown, per
 * {@code ExceptionHandling.md}. Validation failures carry the
 * {@link ImageNormalizationException.Reason} name as their single error parameter so the UI can
 * translate it into a specific message.</p>
 *
 * @since 1.22.0
 */
@Service
public class ProfilePictureService {

  private final ProfilePictureRepository repository;
  private final ProfilePictureAuditRepository auditRepository;
  private final GroupPictureAuthorization groupPictureAuthorization;
  private final GroupAdministrationPermission adminPermission;
  private final ImageNormalizer normalizer;

  @org.springframework.beans.factory.annotation.Autowired
  public ProfilePictureService(ProfilePictureRepository repository,
      ProfilePictureAuditRepository auditRepository,
      GroupPictureAuthorization groupPictureAuthorization,
      GroupAdministrationPermission adminPermission) {
    this(repository, auditRepository, groupPictureAuthorization, adminPermission,
        new ImageNormalizer());
  }

  ProfilePictureService(ProfilePictureRepository repository,
      ProfilePictureAuditRepository auditRepository,
      GroupPictureAuthorization groupPictureAuthorization,
      GroupAdministrationPermission adminPermission, ImageNormalizer normalizer) {
    this.repository = Objects.requireNonNull(repository, "repository must not be null");
    this.auditRepository = Objects.requireNonNull(auditRepository,
        "auditRepository must not be null");
    this.groupPictureAuthorization = Objects.requireNonNull(groupPictureAuthorization,
        "groupPictureAuthorization must not be null");
    this.adminPermission = Objects.requireNonNull(adminPermission,
        "adminPermission must not be null");
    this.normalizer = Objects.requireNonNull(normalizer, "normalizer must not be null");
  }

  /**
   * Sets or replaces the current user's own profile picture.
   *
   * @param actingUserId the authenticated user; must not be blank
   * @param upload       the raw upload; validated and normalized here
   * @return an empty success result, or an error
   */
  @Transactional
  public Result<Void, ApplicationException> setUserPicture(String actingUserId, byte[] upload) {
    if (isBlank(actingUserId)) {
      return Result.fromError(missingActor());
    }
    return store(ProfilePictureOwnerType.USER, actingUserId, actingUserId, upload);
  }

  /**
   * Removes the current user's own profile picture.
   *
   * @param actingUserId the authenticated user; must not be blank
   * @return an empty success result, or an error
   */
  @Transactional
  public Result<Void, ApplicationException> removeUserPicture(String actingUserId) {
    if (isBlank(actingUserId)) {
      return Result.fromError(missingActor());
    }
    delete(ProfilePictureOwnerType.USER, actingUserId);
    return Result.fromValue(null);
  }

  /**
   * Sets or replaces a group's profile picture on behalf of an authorized actor.
   *
   * @param groupId      the group id; must not be blank
   * @param actingUserId the acting user; must be allowed to manage the group's picture
   * @param upload       the raw upload; validated and normalized here
   * @return an empty success result, or an error
   */
  @Transactional
  public Result<Void, ApplicationException> setGroupPicture(String groupId, String actingUserId,
      byte[] upload) {
    Result<Void, ApplicationException> authorization = authorizeGroup(groupId, actingUserId);
    if (authorization.isError()) {
      return authorization;
    }
    return store(ProfilePictureOwnerType.GROUP, groupId, actingUserId, upload);
  }

  /**
   * Removes a group's profile picture on behalf of an authorized actor.
   *
   * @param groupId      the group id; must not be blank
   * @param actingUserId the acting user; must be allowed to manage the group's picture
   * @return an empty success result, or an error
   */
  @Transactional
  public Result<Void, ApplicationException> removeGroupPicture(String groupId, String actingUserId) {
    Result<Void, ApplicationException> authorization = authorizeGroup(groupId, actingUserId);
    if (authorization.isError()) {
      return authorization;
    }
    delete(ProfilePictureOwnerType.GROUP, groupId);
    return Result.fromValue(null);
  }

  /**
   * Force-removes any owner's picture. System administrators only.
   *
   * @param actingAdminUserId the acting administrator; must hold the admin role
   * @param ownerType         the owner kind
   * @param ownerId           the owner id
   * @return an empty success result, or an error
   */
  @Transactional
  public Result<Void, ApplicationException> forceRemove(String actingAdminUserId,
      ProfilePictureOwnerType ownerType, String ownerId) {
    if (isBlank(actingAdminUserId) || !adminPermission.isAdmin(actingAdminUserId)) {
      return Result.fromError(accessDenied(actingAdminUserId));
    }
    if (ownerType == null || isBlank(ownerId)) {
      return Result.fromError(generalError("ownerType and ownerId are required"));
    }
    delete(ownerType, ownerId);
    return Result.fromValue(null);
  }

  /**
   * Reads a stored picture. Read access is broad by design; callers must be authenticated.
   *
   * @param ownerType the owner kind
   * @param ownerId   the owner id
   * @return the stored picture, or empty when none exists
   */
  @Transactional(readOnly = true)
  public Optional<ProfilePicture> find(ProfilePictureOwnerType ownerType, String ownerId) {
    if (ownerType == null || isBlank(ownerId)) {
      return Optional.empty();
    }
    return repository.findByOwnerTypeAndOwnerId(ownerType, ownerId).map(ProfilePictureService::toReadModel);
  }

  /**
   * Reads the content hash of a stored picture without loading the blob. Used by the avatar URL
   * resolver on hot render paths.
   *
   * @param ownerType the owner kind
   * @param ownerId   the owner id
   * @return the SHA-256 content hash, or empty when no picture exists
   */
  @Transactional(readOnly = true)
  public Optional<String> findContentHash(ProfilePictureOwnerType ownerType, String ownerId) {
    if (ownerType == null || isBlank(ownerId)) {
      return Optional.empty();
    }
    return repository.findContentHashByOwnerTypeAndOwnerId(ownerType, ownerId);
  }

  /**
   * Returns the audit trail (set/replace) for administrators, newest first.
   *
   * @param actingAdminUserId the acting administrator; must hold the admin role
   * @param pageable          the requested page
   * @return the audit entries, or an error if the caller is not an administrator
   */
  @Transactional(readOnly = true)
  public Result<List<ProfilePictureAuditEntry>, ApplicationException> listAudit(
      String actingAdminUserId, Pageable pageable) {
    if (isBlank(actingAdminUserId) || !adminPermission.isAdmin(actingAdminUserId)) {
      return Result.fromError(accessDenied(actingAdminUserId));
    }
    List<ProfilePictureAuditEntry> entries =
        auditRepository.findAllByOrderByCreatedAtDesc(pageable).stream()
            .map(ProfilePictureService::toAuditEntry)
            .toList();
    return Result.fromValue(entries);
  }

  /**
   * Deletes the stored picture for an owner. Intended for the (future) user-deletion flow; group
   * deactivation must <b>not</b> call this.
   *
   * @param ownerType the owner kind
   * @param ownerId   the owner id
   */
  @Transactional
  public void deleteByOwner(ProfilePictureOwnerType ownerType, String ownerId) {
    if (ownerType != null && !isBlank(ownerId)) {
      delete(ownerType, ownerId);
    }
  }

  private Result<Void, ApplicationException> authorizeGroup(String groupId, String actingUserId) {
    if (isBlank(groupId) || isBlank(actingUserId)) {
      return Result.fromError(missingActor());
    }
    if (!groupPictureAuthorization.canManage(groupId, actingUserId)) {
      return Result.fromError(accessDenied(actingUserId));
    }
    return Result.fromValue(null);
  }

  private Result<Void, ApplicationException> store(ProfilePictureOwnerType ownerType, String ownerId,
      String actingUserId, byte[] upload) {
    NormalizedImage normalized;
    try {
      normalized = normalizer.normalize(upload);
    } catch (ImageNormalizationException e) {
      return Result.fromError(invalidImage(e));
    }

    Optional<ProfilePictureEntity> existing =
        repository.findByOwnerTypeAndOwnerId(ownerType, ownerId);
    String action = existing.isPresent()
        ? ProfilePictureAuditEntry.ACTION_REPLACE
        : ProfilePictureAuditEntry.ACTION_SET;
    String previousHash = existing.map(ProfilePictureEntity::getContentHash).orElse(null);
    Instant now = Instant.now();

    if (existing.isPresent()) {
      ProfilePictureEntity entity = existing.get();
      entity.update(ImageNormalizer.PNG_CONTENT_TYPE, normalized.contentHash(),
          normalized.width(), normalized.height(), normalized.png(), now, actingUserId);
      repository.save(entity);
    } else {
      repository.save(new ProfilePictureEntity(ownerType, ownerId,
          ImageNormalizer.PNG_CONTENT_TYPE, normalized.contentHash(), normalized.width(),
          normalized.height(), normalized.png(), now, actingUserId));
    }

    auditRepository.save(new ProfilePictureAuditRepository.ProfilePictureAuditEntity(ownerType,
        ownerId, action, actingUserId, previousHash, normalized.contentHash(), now));
    return Result.fromValue(null);
  }

  private void delete(ProfilePictureOwnerType ownerType, String ownerId) {
    repository.deleteByOwnerTypeAndOwnerId(ownerType, ownerId);
  }

  private static ProfilePicture toReadModel(ProfilePictureEntity entity) {
    return new ProfilePicture(entity.getOwnerType(), entity.getOwnerId(), entity.getContentType(),
        entity.getContentHash(), entity.getWidth(), entity.getHeight(), entity.getData(),
        entity.getUpdatedAt());
  }

  private static ProfilePictureAuditEntry toAuditEntry(
      ProfilePictureAuditRepository.ProfilePictureAuditEntity entity) {
    return new ProfilePictureAuditEntry(entity.getOwnerType(), entity.getOwnerId(),
        entity.getAction(), entity.getActorId(), entity.getPreviousContentHash(),
        entity.getNewContentHash(), entity.getCreatedAt());
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private static ApplicationException missingActor() {
    return generalError("missing acting user id");
  }

  private static ApplicationException generalError(String message) {
    return new ApplicationException(message, ErrorCode.GENERAL, ErrorParameters.empty());
  }

  private static ApplicationException accessDenied(String userId) {
    return new ApplicationException("User " + userId + " is not allowed to manage this profile "
        + "picture", ErrorCode.ACCESS_DENIED, ErrorParameters.empty());
  }

  private static ApplicationException invalidImage(ImageNormalizationException e) {
    return new ApplicationException(e.getMessage(), ErrorCode.GENERAL,
        ErrorParameters.of(e.reason().name()));
  }
}
