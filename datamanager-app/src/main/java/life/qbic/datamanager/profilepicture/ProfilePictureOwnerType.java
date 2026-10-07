package life.qbic.datamanager.profilepicture;

/**
 * The kind of owner a {@link ProfilePicture} belongs to.
 *
 * <p>Profile pictures are a presentation concern shared by two bounded contexts: the identity
 * context owns user profiles and the user-groups context owns group profiles. The owner type is
 * therefore part of the picture's identity — a user id and a group id share no id space.</p>
 *
 * @since 1.19.0
 */
public enum ProfilePictureOwnerType {
  USER,
  GROUP
}
