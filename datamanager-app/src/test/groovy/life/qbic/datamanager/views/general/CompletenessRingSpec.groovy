package life.qbic.datamanager.views.general

import spock.lang.Specification

/**
 * Covers the completeness ring's geometry at the two ends of its range.
 *
 * <p>The ring draws its arc as an SVG circle with {@code stroke-dasharray}, and a round line cap
 * softens the ends of a partial arc. At 100% the arc closes on itself, so both caps land on the same
 * point and overdraw each other into a visible bulge -- the arc looked thicker at 12 o'clock than
 * everywhere else. A closed circle is the one case where the cap must be butt.</p>
 */
class CompletenessRingSpec extends Specification {

  def "a partial ring keeps round caps so its ends are softened"() {
    when:
    def ring = new CompletenessRing(1, 3, "1 of 3")

    then:
    arcMarkup(ring).contains("stroke-linecap=\"round\"")
  }

  /**
   * The regression: at 100% the two round caps met at the same point and overdrew into a bulge.
   */
  def "a complete ring closes with butt caps, because it has no end left to soften"() {
    when:
    def ring = new CompletenessRing(6, 6, "all 6")

    then:
    !arcMarkup(ring).contains("stroke-linecap=\"round\"")
    arcMarkup(ring).contains("stroke-linecap=\"butt\"")
  }

  def "the complete state is carried by the class and the arc, not by a halo"() {
    when:
    def ring = new CompletenessRing(6, 6, "all 6")

    then: "the state is still exposed for styling and for assistive technology"
    ring.classNames.contains("completeness-ring--complete")
    ring.element.getAttribute("aria-valuenow") == "100"
    ring.element.getAttribute("aria-valuetext") == "all 6"

    and: "exactly one arc is drawn -- a second ring outside it was the reported bug"
    countOccurrences(arcMarkup(ring), "completeness-ring__value") == 1
  }

  def "the centre states the percentage"() {
    expect:
    centreText(new CompletenessRing(3, 4, "3 of 4")) == "75%"
    centreText(new CompletenessRing(6, 6, "all 6")) == "100%"
    centreText(new CompletenessRing(0, 0, "none")) == "\u2014"
  }

  /** The inline SVG the component writes into its dial, as a string. */
  private static String arcMarkup(CompletenessRing ring) {
    return ring.getChildren()
        .filter { it.classNames.contains("completeness-ring__dial") }
        .findFirst().orElseThrow()
        .element.getProperty("innerHTML", "")
  }

  private static String centreText(CompletenessRing ring) {
    return ring.getChildren()
        .filter { it.classNames.contains("completeness-ring__centre") }
        .findFirst().orElseThrow()
        .element.text
  }

  private static int countOccurrences(String haystack, String needle) {
    int count = 0
    int index = haystack.indexOf(needle)
    while (index >= 0) {
      count++
      index = haystack.indexOf(needle, index + needle.length())
    }
    return count
  }
}
