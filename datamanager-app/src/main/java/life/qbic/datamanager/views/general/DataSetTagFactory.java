package life.qbic.datamanager.views.general;

/**
 * Factory for dataset-related {@link Tag} components.
 *
 * <p>Centralized here so the connected-resources list and the
 * connect-datasets sidebar (and any future surfaces) share the same
 * visual language without duplicating the logic.</p>
 *
 * <ul>
 *   <li><strong>Provider</strong> tags use a single neutral color
 *       ({@link Tag.TagColor#CONTRAST}) regardless of which provider they
 *       represent — color would imply a hierarchy or status that providers
 *       don't carry, and per-provider coloring does not scale as more
 *       repositories are added.</li>
 *   <li><strong>Dataset type</strong> tags also use {@link Tag.TagColor#CONTRAST}
 *       so they share the neutral visual weight of the provider badge.</li>
 *   <li><strong>Access type</strong> tags encode the public/restricted status
 *       with semantic color: {@link Tag.TagColor#SUCCESS} for public,
 *       {@link Tag.TagColor#WARNING} for restricted.</li>
 * </ul>
 *
 * <p>This factory is purely a UI-layer concern and does not depend on
 * any domain or application classes. The caller is responsible for
 * resolving domain-level representations (e.g. {@code AccessLevel})
 * into simple values (e.g. a boolean) before delegating here.</p>
 *
 * @since 1.12.0
 */
public final class DataSetTagFactory {

  public enum TagType {
    DATA_SET_TYPE,
    PROVIDER,
    ACCESS_TYPE
  }

  private DataSetTagFactory() {}

  /**
   * Creates a tag for the given type and display text.
   *
   * @param type the tag category
   * @param text the display text (e.g. provider name, resource type)
   * @return a styled {@link Tag} ready to add to the DOM
   */
  public static Tag create(TagType type, String text) {
    var tag = new Tag(text != null ? text : "");
    tag.setTagColor(Tag.TagColor.CONTRAST);
    return tag;
  }

  /**
   * Creates an access-level tag.
   *
   * <p>{@code isPublic == true} renders with {@link Tag.TagColor#SUCCESS}
   * (label "Public"); {@code false} renders with
   * {@link Tag.TagColor#WARNING} (label "Restricted").</p>
   *
   * @param type     must be {@link TagType#ACCESS_TYPE}
   * @param isPublic {@code true} for public access, {@code false} for
   *     restricted
   * @return a styled {@link Tag} ready to add to the DOM
   * @throws IllegalArgumentException if {@code type} is not
   *     {@link TagType#ACCESS_TYPE}
   */
  public static Tag create(TagType type, boolean isPublic) {
    if (type != TagType.ACCESS_TYPE) {
      throw new IllegalArgumentException(
          "boolean overload only supports TagType.ACCESS_TYPE");
    }
    String label = isPublic ? "Public" : "Restricted";
    Tag.TagColor color = isPublic ? Tag.TagColor.SUCCESS : Tag.TagColor.WARNING;
    var tag = new Tag(label);
    tag.setTagColor(color);
    return tag;
  }
}
