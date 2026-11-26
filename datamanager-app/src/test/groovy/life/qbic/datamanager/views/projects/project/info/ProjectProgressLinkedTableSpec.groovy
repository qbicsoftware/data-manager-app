package life.qbic.datamanager.views.projects.project.info

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Span
import life.qbic.projectmanagement.application.associated_dataset.ConnectedDatasetView
import spock.lang.Specification

import java.time.Instant

/**
 * Covers the "Linked datasets" card of the project page's progress band.
 *
 * <p>Two things were wrong with it relative to the card beside it. It stated its size ("6 records")
 * while the measurement card did not, so the pair read as two different kinds of block; and its
 * table had no header row, so "3 days ago" and "Zenodo" ran on from the title as if they belonged to
 * it. Both are pinned here.</p>
 *
 * <p>Asserts the row builders directly — they read only their argument, and building the section
 * would need collaborators this classpath cannot mock.</p>
 */
class ProjectProgressLinkedTableSpec extends Specification {

  // ── The header row ─────────────────────────────────────────────────────

  def "the linked table has a header row naming its columns"() {
    expect:
    headerLabels(true) == ["Type", "Name", "Last activity", "Source"]
    headerLabels(false) == ["Type", "Name", "Last activity"]
  }

  /**
   * The header and the data rows must emit the same number of cells or the grid drops a column.
   * The provider column appears only when records disagree on their source, so both shapes are
   * checked against a matching data row.
   */
  def "the header row has the same cell count as a data row, in both shapes"() {
    given:
    def withProvider = asset("Zenodo")
    def withoutProvider = asset("Zenodo")

    expect:
    headerCellCount(true) == dataCellCount(withProvider, true)
    headerCellCount(false) == dataCellCount(withoutProvider, false)
  }

  // ── The activity column ────────────────────────────────────────────────

  /**
   * The column reports when the record last changed. "Synced" vs "connected" distinguishes two
   * events this card does not act on, and the verb made the cell wider than the fact it carried.
   */
  def "the activity cell is the bare relative time, without the event verb"() {
    given: "one record synced this morning and one only ever connected"
    def synced = asset("Zenodo", Instant.now().minusSeconds(3600), Instant.now().minusSeconds(7200))
    def connectedOnly = asset("Zenodo", null, Instant.now().minusSeconds(7200))

    expect:
    ProjectProgressSection.activityLabel(synced) == "1 hour ago"
    ProjectProgressSection.activityLabel(connectedOnly) == "2 hours ago"

    and: "neither carries a verb"
    !ProjectProgressSection.activityLabel(synced).contains("synced")
    !ProjectProgressSection.activityLabel(connectedOnly).contains("connected")
  }

  // ── The count line both cards share ────────────────────────────────────

  /**
   * Only the linked card stated its size, so the pair read as two different kinds of block: one that
   * tells you how much it holds and one that does not. Both now open their body with it.
   */
  def "both cards state their size with the same construction"() {
    expect: "the measurement card's line"
    summaryText(ProjectProgressSection.cardSummary(3, "dataset", "datasets")) == "3 datasets"

    and: "the linked card's, which additionally names the shared source"
    summaryText(ProjectProgressSection.cardSummary(6, "record", "records", "Zenodo")) ==
        "6 records from Zenodo"

    and: "singular is not pluralised"
    summaryText(ProjectProgressSection.cardSummary(1, "dataset", "datasets")) == "1 dataset"
    summaryText(ProjectProgressSection.cardSummary(1, "record", "records")) == "1 record"
  }

  // ── Row content ────────────────────────────────────────────────────────

  def "a row states its type, title and last activity, in that order"() {
    given:
    def record = asset("Zenodo", null, Instant.now().minusSeconds(2 * 86400))

    when:
    def cells = cellTexts(ProjectProgressSection.connectedAssetRow(record, false))

    then:
    cells.size() == 3
    cells[1] == "A record title"
    cells[2] == "2 days ago"
  }

  def "a record with a link renders its title as an anchor, and one without renders plain text"() {
    given:
    def linked = asset("Zenodo", null, Instant.now(), "https://example.org/record/1")
    def unlinked = asset("Zenodo", null, Instant.now(), null)

    expect:
    containsAnchor(ProjectProgressSection.connectedAssetRow(linked, false))
    !containsAnchor(ProjectProgressSection.connectedAssetRow(unlinked, false))
  }

  // ── Helpers ────────────────────────────────────────────────────────────

  private static String summaryText(Div summary) {
    return summary.getChildren().toList().collect { (it as Span).text }.join(" ")
  }

  private static List<String> headerLabels(boolean showProvider) {
    return cellTexts(ProjectProgressSection.connectedHeaderRow(showProvider))
  }

  private static int headerCellCount(boolean showProvider) {
    return ProjectProgressSection.connectedHeaderRow(showProvider).getChildren().toList().size()
  }

  private static int dataCellCount(ConnectedDatasetView asset, boolean showProvider) {
    return ProjectProgressSection.connectedAssetRow(asset, showProvider).getChildren().toList()
        .size()
  }

  private static List<String> cellTexts(Div row) {
    return row.getChildren().toList().collect { cell ->
      if (cell instanceof Span) {
        return (cell as Span).text ?: ""
      }
      // The title cell may be an Anchor; its text is what the row shows.
      return cell.element.text ?: ""
    }
  }

  private static boolean containsAnchor(Div row) {
    return row.getChildren().toList().any { it instanceof com.vaadin.flow.component.html.Anchor }
  }

  private static ConnectedDatasetView asset(String provider) {
    return asset(provider, null, Instant.now(), "https://example.org/record/1")
  }

  private static ConnectedDatasetView asset(String provider, Instant lastSyncedAt,
      Instant connectedOn, String accessLink = "https://example.org/record/1") {
    // 19 components, in the record's declared order: id, title, pid, isPublic, version, accessLink,
    // publicationDate, resourceProvider, creators, resourceType, community, accessDetail,
    // connectedByUserId, connectedByDisplayName, connectedOn, lastSyncedAt, experimentId,
    // experimentName, sourceType. The last three are the record's own optional links.
    return new ConnectedDatasetView(
        "id-1", "A record title", "10.5281/zenodo.1", true, "v1", accessLink,
        java.time.LocalDate.now(), provider, List.of("Stanger, Anna"), "Dataset", "QBiC",
        null, "user-1", "Sven Admin", connectedOn, lastSyncedAt,
        null, null, "INVENIO_RDM")
  }
}
