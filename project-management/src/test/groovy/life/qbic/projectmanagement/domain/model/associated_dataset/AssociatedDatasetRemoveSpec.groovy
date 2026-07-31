package life.qbic.projectmanagement.domain.model.associated_dataset

import life.qbic.domain.concepts.DomainEvent
import life.qbic.domain.concepts.DomainEventSubscriber
import life.qbic.domain.concepts.LocalDomainEventDispatcher
import life.qbic.projectmanagement.domain.model.associated_dataset.event.AssociatedDatasetRemovedEvent
import life.qbic.projectmanagement.domain.model.project.ProjectId
import spock.lang.Specification

/**
 * Unit tests for {@link AssociatedDataset#remove(String removedByUserId)}.
 *
 * Verifies that the aggregate transitions to {@link ConnectionState#REMOVED}
 * and dispatches an {@link AssociatedDatasetRemovedEvent} via the
 * local domain event dispatcher.
 *
 * @since 1.12.0
 */
class AssociatedDatasetRemoveSpec extends Specification {

  private static final String VALID_PROJECT_ID = "0270ce7f-4092-40e3-9c4c-ce7adb688bf5"

  def setup() {
    LocalDomainEventDispatcher.instance().reset()
  }

  def "remove sets state to REMOVED and emits AssociatedDatasetRemovedEvent"() {
    given:
    def dataset = createConnectedDataset()
    def capturedEvents = new ArrayList<DomainEvent>()
    LocalDomainEventDispatcher.instance().subscribe(
        new RemovalEventCapture(capturedEvents))

    when:
    dataset.remove("remover-user-id")

    then:
    dataset.connectionState() == ConnectionState.REMOVED

    and: "exactly one AssociatedDatasetRemovedEvent was dispatched"
    capturedEvents.size() == 1
    capturedEvents[0] instanceof AssociatedDatasetRemovedEvent
    def event = capturedEvents[0] as AssociatedDatasetRemovedEvent
    event.actorUserId() == "remover-user-id"
    event.associatedDatasetId() == dataset.id()
    event.projectId() == dataset.projectId()
    event.datasetTitle() == "Test Dataset"
    event.datasetPid() == "10.1234/test"
  }

  def "remove throws IllegalStateException when dataset is already removed"() {
    given:
    def dataset = createConnectedDataset()
    dataset.remove("user-1")

    when:
    dataset.remove("user-2")

    then:
    def thrown = thrown(IllegalStateException)
    thrown.message.contains("already removed")
  }

  def "remove throws NullPointerException on null removedByUserId"() {
    given:
    def dataset = createConnectedDataset()

    when:
    dataset.remove(null)

    then:
    thrown(NullPointerException)
    // Dataset must still be in CONNECTED state — removal was not performed
    dataset.connectionState() == ConnectionState.CONNECTED
  }

  def "remove does not emit event when dataset is already removed"() {
    given:
    def dataset = createConnectedDataset()
    dataset.remove("user-1")

    // Reset dispatcher (clear the event from the first removal) and
    // attach a fresh subscriber to the second (failing) removal attempt.
    LocalDomainEventDispatcher.instance().reset()
    def capturedEvents = new ArrayList<DomainEvent>()
    LocalDomainEventDispatcher.instance().subscribe(
        new RemovalEventCapture(capturedEvents))

    when:
    try {
      dataset.remove("user-2")
    } catch (IllegalStateException ignored) {}

    then:
    capturedEvents.isEmpty()
  }

  // ─ Helpers ───────────────────────────────────────────────────────────────

  /**
   * Captures {@link AssociatedDatasetRemovedEvent} instances dispatched
   * on the local domain event bus.
   *
   * <p>The {@link LocalDomainEventDispatcher} uses exact-type matching
   * ({@code ==}) when filtering subscribers, so this subscriber declares
   * {@code AssociatedDatasetRemovedEvent.class} as its event type.</p>
   */
  private static class RemovalEventCapture
      implements DomainEventSubscriber<AssociatedDatasetRemovedEvent> {

    private final List<DomainEvent> events

    RemovalEventCapture(List<DomainEvent> events) {
      this.events = events
    }

    @Override
    Class<? extends DomainEvent> subscribedToEventType() {
      return AssociatedDatasetRemovedEvent.class
    }

    @Override
    void handleEvent(AssociatedDatasetRemovedEvent event) {
      events.add(event)
    }
  }

  private static AssociatedDataset createConnectedDataset() {
    return AssociatedDataset.connect(
        ProjectId.parse(VALID_PROJECT_ID),
        SourceType.INVENIO_RDM,
        new ExternalHandle("ext-1"),
        new InvenioRdmResourceMetadata(
            "Test Dataset",
            "10.1234/test",
            "v1",
            "https://zenodo.org/records/12345",
            "Zenodo",
            [],
            "Dataset",
            "QBiC",
            java.time.LocalDate.of(2025, 1, 15),
            null,
            InvenioRdmAccessStatus.PUBLIC,
            InvenioRdmAccessStatus.PUBLIC),
        "original-connector",
        null)
  }
}
