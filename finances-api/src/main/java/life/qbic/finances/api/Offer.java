package life.qbic.finances.api;

/**
 * <b>Offer</b>
 *
 * <p>Holds offer information such as:</p>
 *
 * <ul>
 *   <li>offer id</li>
 *   <li>title</li>
 *   <li>objective</li>
 *   <li>experimental design</li>
 * </ul>
 *
 * @since 1.0.0
 */
public record Offer(String id, String title, String objective, String experimentDescription) {
}
