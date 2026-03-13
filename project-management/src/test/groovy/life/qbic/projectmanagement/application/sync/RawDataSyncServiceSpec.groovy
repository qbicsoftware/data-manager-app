package life.qbic.projectmanagement.application.sync

import life.qbic.projectmanagement.application.api.AsyncProjectService.RawDataset
import life.qbic.projectmanagement.application.dataset.LocalRawDatasetCache
import life.qbic.projectmanagement.application.dataset.RemoteRawDataService
import life.qbic.projectmanagement.application.sync.WatermarkRepo.Watermark
import spock.lang.Specification
import spock.lang.Subject

import java.time.Instant

/**
 * Unit tests for {@link RawDataSyncService}, focusing on watermark update logic.
 *
 * <p>The outer {@code sync()} method runs a time-boxed while-loop calling {@code runSync()}.
 * Tests that need to observe a single iteration set up the mocks so that the first call returns
 * a partial/empty page (causing {@code runSync()} to return {@code false} and the loop to break
 * immediately).  Tests that need to observe two iterations chain mock responses.</p>
 */
class RawDataSyncServiceSpec extends Specification {

  static final int MAX_QUERY_SIZE = 1_000
  static final String JOB_NAME = "RAW_DATA_SYNC_EXTERNAL"

  RemoteRawDataService remoteRawDataServiceMock = Mock()
  WatermarkRepo watermarkRepoMock = Mock()
  LocalRawDatasetCache localRawDatasetCacheMock = Mock()

  @Subject
  RawDataSyncService service = new RawDataSyncService(
      remoteRawDataServiceMock,
      watermarkRepoMock,
      localRawDatasetCacheMock,
      MAX_QUERY_SIZE,
      60_000
  )

  def setup() {
    service.setSelf(service)
  }

  // ------------------------------------------------------------------
  // Helpers
  // ------------------------------------------------------------------

  private static RawDataset dataset(Instant registrationDate) {
    new RawDataset("QABCD001", 1024L, 1, Set.of(".fastq"), registrationDate)
  }

  private static List<RawDataset> fullPage() {
    (1..MAX_QUERY_SIZE).collect { dataset(Instant.EPOCH.plusSeconds(it)) }
  }

  private static List<RawDataset> partialPage(int size) {
    assert size > 0 && size < MAX_QUERY_SIZE
    (1..size).collect { dataset(Instant.EPOCH.plusSeconds(it)) }
  }

  // ------------------------------------------------------------------
  // Race-condition fix: updatedSince must be captured BEFORE the remote query
  // ------------------------------------------------------------------

  def "race condition fix: updatedSince is before lastSuccessAt, proving it was captured before the query completed"() {
    given: "no prior watermark"
    watermarkRepoMock.fetch(JOB_NAME) >> Optional.empty()

    and: "the remote returns a partial page so the loop terminates after one iteration"
    remoteRawDataServiceMock.registeredSince(_, _, _) >> partialPage(3)

    Watermark savedWatermark = null
    watermarkRepoMock.save(_ as Watermark) >> { Watermark w -> savedWatermark = w }

    when: "we capture the time just before the sync runs"
    Instant beforeSync = Instant.now()
    service.sync()
    Instant afterSync = Instant.now()

    then: "updatedSince falls within the [beforeSync, afterSync] window"
    // updatedSince must be >= beforeSync: it was captured inside runSync(), after entry
    !savedWatermark.updatedSince().isBefore(beforeSync)
    // updatedSince must be <= afterSync: it was captured before sync() returned
    !savedWatermark.updatedSince().isAfter(afterSync)

    and: "updatedSince before lastSuccessAt, proving it was captured before the save"
    // queryTime is captured before the remote call; lastSuccessAt is captured after.
    // Therefore updatedSince < lastSuccessAt always holds when the fix is in place.
    savedWatermark.updatedSince().isBefore(savedWatermark.lastSuccessAt())
  }

  // ------------------------------------------------------------------
  // Bug regression: partial page must update watermark to now
  // ------------------------------------------------------------------

  def "when the last page is partial, the saved watermark updatedSince is close to now (not a remote entity timestamp)"() {
    given: "no prior watermark"
    watermarkRepoMock.fetch(JOB_NAME) >> Optional.empty()

    and: "the remote returns a partial page (< MAX_QUERY_SIZE) — loop will break after one iteration"
    def items = partialPage(5)
    remoteRawDataServiceMock.registeredSince(_, _, _) >> items

    and: "capture the saved watermark"
    Watermark savedWatermark = null
    watermarkRepoMock.save(_ as Watermark) >> { Watermark w -> savedWatermark = w }

    when:
    service.sync()

    then: "updatedSince is NOT one of the ancient remote registration timestamps (EPOCH + a few seconds)"
    savedWatermark != null
    // All item registrationDates are <= EPOCH + 5s (effectively year 1970).
    // The saved updatedSince must be close to the current time (within 10s of now).
    savedWatermark.updatedSince().isAfter(Instant.now().minusSeconds(10))

    and: "the offset is reset to 0 so the next run starts fresh"
    savedWatermark.syncOffset() == 0
  }

  def "bug regression: watermark does NOT stay stuck at the remote entity timestamp after a full sync completes"() {
    given: "an initial watermark stuck at EPOCH (simulating the bug state)"
    def priorWatermark = new Watermark(JOB_NAME, 0, Instant.EPOCH, Instant.EPOCH)
    watermarkRepoMock.fetch(JOB_NAME) >> Optional.of(priorWatermark)

    and: "the remote returns 10 items with very old registrationDates (EPOCH + 1..10 s)"
    def oldItems = (1..10).collect { dataset(Instant.EPOCH.plusSeconds(it)) }
    remoteRawDataServiceMock.registeredSince(_, _, _) >> oldItems

    Watermark savedWatermark = null
    watermarkRepoMock.save(_ as Watermark) >> { Watermark w -> savedWatermark = w }

    when:
    service.sync()

    then: "the saved watermark's updatedSince is NOT stuck at an ancient remote timestamp"
    savedWatermark.updatedSince() != Instant.EPOCH
    savedWatermark.updatedSince().isAfter(Instant.now().minusSeconds(10))

    and: "offset is 0 so we do not re-paginate the old window"
    savedWatermark.syncOffset() == 0
  }

  // ------------------------------------------------------------------
  // Empty page: watermark also advances to now
  // ------------------------------------------------------------------

  def "when the remote returns an empty page, the saved watermark updatedSince is close to now"() {
    given: "a prior watermark with a non-zero offset (previous page was exactly full)"
    def priorWatermark = new Watermark(JOB_NAME, MAX_QUERY_SIZE, Instant.EPOCH, Instant.EPOCH)
    watermarkRepoMock.fetch(JOB_NAME) >> Optional.of(priorWatermark)

    and: "the remote returns an empty page — loop will break after one iteration"
    remoteRawDataServiceMock.registeredSince(_, _, _) >> []

    Watermark savedWatermark = null
    watermarkRepoMock.save(_ as Watermark) >> { Watermark w -> savedWatermark = w }

    when:
    service.sync()

    then:
    savedWatermark != null
    savedWatermark.updatedSince().isAfter(Instant.now().minusSeconds(10))
    savedWatermark.syncOffset() == 0
  }

  // ------------------------------------------------------------------
  // Full page: offset advances, updatedSince stays unchanged
  // ------------------------------------------------------------------

  def "when a full page is returned, offset advances and updatedSince is preserved until the last page"() {
    given: "a prior watermark at offset 0 with a specific updatedSince"
    Instant originalUpdatedSince = Instant.parse("2025-01-01T00:00:00Z")
    def initialWatermark = new Watermark(JOB_NAME, 0, originalUpdatedSince, Instant.EPOCH)

    // First fetch returns the initial watermark; the second fetch (after offset advances)
    // simulates the stored watermark being reloaded by Spock's iterator stub behaviour.
    // We give the second fetch a watermark with the advanced offset so runSync can terminate.
    def advancedWatermark = new Watermark(JOB_NAME, MAX_QUERY_SIZE, originalUpdatedSince, Instant.now())
    watermarkRepoMock.fetch(JOB_NAME) >>> [
        Optional.of(initialWatermark),   // 1st iteration
        Optional.of(advancedWatermark)   // 2nd iteration
    ]

    and: "first remote call returns a full page; second call (advanced offset) returns a partial page"
    remoteRawDataServiceMock.registeredSince(_, 0, _) >> fullPage()
    remoteRawDataServiceMock.registeredSince(_, MAX_QUERY_SIZE, _) >> partialPage(3)

    List<Watermark> savedWatermarks = []
    watermarkRepoMock.save(_ as Watermark) >> { Watermark w -> savedWatermarks << w }

    when:
    service.sync()

    then: "exactly two watermarks were saved"
    savedWatermarks.size() == 2

    and: "the first saved watermark advances the offset and keeps updatedSince unchanged"
    savedWatermarks[0].syncOffset() == MAX_QUERY_SIZE
    savedWatermarks[0].updatedSince() == originalUpdatedSince

    and: "the second saved watermark (last page) resets the offset and advances updatedSince to now"
    savedWatermarks[1].syncOffset() == 0
    savedWatermarks[1].updatedSince().isAfter(Instant.now().minusSeconds(10))
  }

  // ------------------------------------------------------------------
  // Dataset persistence
  // ------------------------------------------------------------------

  def "datasets from a partial page are persisted"() {
    given:
    watermarkRepoMock.fetch(JOB_NAME) >> Optional.empty()
    def items = partialPage(7)
    remoteRawDataServiceMock.registeredSince(_, _, _) >> items
    watermarkRepoMock.save(_ as Watermark) >> {}

    when:
    service.sync()

    then:
    1 * localRawDatasetCacheMock.saveAll(items)
  }

  def "datasets from both pages are persisted during multi-page sync"() {
    given:
    def initialWatermark = new Watermark(JOB_NAME, 0, Instant.EPOCH, Instant.EPOCH)
    def advancedWatermark = new Watermark(JOB_NAME, MAX_QUERY_SIZE, Instant.EPOCH, Instant.now())
    watermarkRepoMock.fetch(JOB_NAME) >>> [
        Optional.of(initialWatermark),
        Optional.of(advancedWatermark)
    ]

    def page1 = fullPage()
    def page2 = partialPage(1)
    remoteRawDataServiceMock.registeredSince(_, 0, _) >> page1
    remoteRawDataServiceMock.registeredSince(_, MAX_QUERY_SIZE, _) >> page2
    watermarkRepoMock.save(_ as Watermark) >> {}

    when:
    service.sync()

    then:
    1 * localRawDatasetCacheMock.saveAll(page1)
    1 * localRawDatasetCacheMock.saveAll(page2)
  }

  def "when the remote returns an empty page, no datasets are persisted"() {
    given:
    watermarkRepoMock.fetch(JOB_NAME) >> Optional.empty()
    remoteRawDataServiceMock.registeredSince(_, _, _) >> []
    watermarkRepoMock.save(_ as Watermark) >> {}

    when:
    service.sync()

    then:
    0 * localRawDatasetCacheMock.saveAll(_)
  }
}
