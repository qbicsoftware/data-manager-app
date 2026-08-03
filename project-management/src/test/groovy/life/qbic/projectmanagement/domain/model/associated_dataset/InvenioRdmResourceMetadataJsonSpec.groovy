package life.qbic.projectmanagement.domain.model.associated_dataset

import spock.lang.Specification
import tools.jackson.databind.ObjectMapper

/**
 * Regression coverage for the {@code resource_metadata} JSON column.
 *
 * <p>Rows written by earlier application versions of
 * {@link InvenioRdmResourceMetadata} may carry fields that have since
 * been removed (e.g. {@code embargoUntil}). The converter that reads
 * {@link ResourceMetadata} back from JSON must tolerate those stale
 * rows — otherwise the page listing connected datasets fails to load
 * with a silent {@code AttributeConverter} error.</p>
 *
 * <p>Mirrors the {@code ObjectMapper} configuration on
 * {@link AssociatedDataset.ResourceMetadataConverter}.</p>
 */
class InvenioRdmResourceMetadataJsonSpec extends Specification {

  ObjectMapper mapper

  def setup() {
    mapper = new ObjectMapper()
  }

  def "legacy JSON containing 'embargoUntil' is deserialized without error"() {
    given: "a JSON blob written by an app version that persisted embargoUntil"
    def legacyJson = """
    {
      "type": "INVENIO_RDM",
      "title": "Test dataset",
      "pid": "10.5281/zenodo.1234567",
      "version": "v1",
      "accessLink": null,
      "resourceProvider": "Zenodo",
      "creators": [],
      "resourceType": "Dataset",
      "community": null,
      "publicationDate": "2025-01-15",
      "description": null,
      "recordAccess": "PUBLIC",
      "fileAccess": "PUBLIC",
      "embargoUntil": null
    }
    """.stripIndent().trim()

    when:
    def metadata = mapper.readValue(legacyJson, ResourceMetadata)

    then: "the record is reconstructed; the removed field is silently dropped"
    metadata instanceof InvenioRdmResourceMetadata
    metadata.title() == "Test dataset"
    metadata.pid() == "10.5281/zenodo.1234567"
    metadata.publicationDate().toString() == "2025-01-15"
    metadata.recordAccess() == InvenioRdmAccessStatus.PUBLIC
    metadata.fileAccess() == InvenioRdmAccessStatus.PUBLIC
  }

  def "current-schema JSON round-trips correctly"() {
    given: "a freshly constructed record (current schema, no embargoUntil)"
    def fresh = new InvenioRdmResourceMetadata(
        "Test", "10.5281/zenodo.999", "v2", "https://zenodo.org/records/999",
        "Zenodo", ["Smith, Alice"], "Dataset", null,
        java.time.LocalDate.of(2025, 6, 1), null,
        InvenioRdmAccessStatus.PUBLIC, InvenioRdmAccessStatus.PUBLIC)

    when: "serialized to JSON then deserialized back via the polymorphic interface"
    def json = mapper.writeValueAsString(fresh)
    def roundTrip = mapper.readValue(json, ResourceMetadata)

    then:
    roundTrip instanceof InvenioRdmResourceMetadata
    roundTrip.title() == "Test"
    roundTrip.publicationDate().toString() == "2025-06-01"
    json.contains('"type":"INVENIO_RDM"')
  }

  def "unknown field of any kind is silently ignored"() {
    given: "a JSON blob with an entirely unknown future field"
    def json = """
    {
      "type": "INVENIO_RDM",
      "title": "T",
      "pid": "10.5281/x",
      "publicationDate": "2025-01-01",
      "resourceProvider": "Zenodo",
      "recordAccess": "PUBLIC",
      "fileAccess": "PUBLIC",
      "someFutureFieldWeDontKnowAboutYet": 42
    }
    """.stripIndent().trim()

    when:
    def metadata = mapper.readValue(json, ResourceMetadata)

    then: "no exception; the unknown field is dropped"
    metadata instanceof InvenioRdmResourceMetadata
    metadata.title() == "T"
  }
}
