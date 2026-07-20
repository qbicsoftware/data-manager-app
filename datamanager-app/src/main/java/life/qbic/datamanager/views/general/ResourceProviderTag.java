package life.qbic.datamanager.views.general;

/**
 * Factory for resource provider {@link Tag} components.
 *
 * <p>All provider tags use a single neutral color ({@link Tag.TagColor#CONTRAST})
 * regardless of which provider they represent. The provider name is
 * distinguishable by the badge text itself — color would imply a
 * hierarchy or status that providers don't carry, and per-provider
 * coloring does not scale as more repositories are added.</p>
 *
 * <p>Centralized here so the connected-resources list and the
 * connect-datasets sidebar (and any future surfaces) share the same
 * visual language without duplicating the logic.</p>
 *
 * @since 1.12.0
 */
public final class ResourceProviderTag {

  private ResourceProviderTag() {}

  /**
   * Creates a provider tag for the given repository name.
   *
   * @param providerName the display name of the provider (e.g.
   *     "Zenodo", "FDAT")
   * @return a styled {@link Tag} ready to add to the DOM
   */
  public static Tag of(String providerName) {
    var tag = new Tag(providerName != null ? providerName : "");
    tag.setTagColor(Tag.TagColor.CONTRAST);
    return tag;
  }
}
