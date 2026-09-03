package life.qbic.projectmanagement.application.associated_dataset;

/**
 * Result of creating a shareable access link on an external data source.
 *
 * <p>Carries both the human-usable link {@link #url()} (the full record
 * URL with the access token embedded) and the opaque {@link #linkId()}
 * the source system assigned to the link. The id is required later to
 * {@linkplain DatasetSource#revokeAccessLink revoke} the link, while the
 * URL is what project collaborators actually use to view the dataset.</p>
 *
 * <p>Instances are immutable.</p>
 *
 * @param url    the full access-link URL (record URL + {@code ?token=…})
 * @param linkId the source-system identity of the link, or null if the
 *               source does not expose one; a null id means the link
 *               cannot be revoked later
 * @since 1.12.0
 */
public record CreatedAccessLink(String url, String linkId) {

  public CreatedAccessLink {
    if (url == null || url.isBlank()) {
      throw new IllegalArgumentException("access link url must not be blank");
    }
    // linkId may be null
  }
}