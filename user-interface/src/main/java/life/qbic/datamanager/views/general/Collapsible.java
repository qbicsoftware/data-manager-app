package life.qbic.datamanager.views.general;

/**
 * <b>Collapsible Element Interface</b>
 *
 * <p>Collapsible elements can be collapsed into a concise view representation of an element,
 * such as e.g. a large text box with information.</p>
 *
 * @since 1.7.0
 */
public interface Collapsible {

  /**
   * Collapse the element into its concise view format
   *
   * @since 1.7.0
   */
  void collapse();

  /**
   * Expand the element into its expanded view format
   *
   * @since 1.7.0
   */
  void expand();

}
