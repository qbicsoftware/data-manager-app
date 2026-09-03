package life.qbic.projectmanagement.infrastructure.external.invenio

import life.qbic.projectmanagement.application.associated_dataset.CredentialEncryptor
import life.qbic.projectmanagement.application.associated_dataset.DatasetAccessDeniedException
import life.qbic.projectmanagement.application.associated_dataset.DatasetResolveException
import life.qbic.projectmanagement.application.associated_dataset.InstanceConfig
import life.qbic.projectmanagement.application.associated_dataset.ResolvedRecord
import life.qbic.projectmanagement.domain.model.associated_dataset.AccessLevel
import life.qbic.projectmanagement.domain.model.associated_dataset.InvenioRdmAccessStatus
import life.qbic.projectmanagement.domain.model.associated_dataset.InvenioRdmResourceMetadata
import life.qbic.projectmanagement.domain.model.associated_dataset.repository.UserExternalCredentialRepository
import spock.lang.Specification

/**
 * Unit tests for {@link InvenioRdmDatasetSource#resolveLatest} — the
 * version-following resolution used by dataset synchronisation
 * (DATSET-04/08, ADR-0005 V1).
 *
 * <p>Hand-rolled Groovy interface coercions are used instead of Spock
 * {@code Mock()} so the spec runs without the Mockito mock maker.</p>
 *
 * @since 1.13.0
 */
class InvenioRdmDatasetSourceResolveLatestSpec extends Specification {

  private static final InstanceConfig CONFIG =
      new InstanceConfig("zenodo", "Zenodo", "https://zenodo.org")

  private static InvenioRdmClient.RecordResponse record(
      String id, boolean isLatest, String parentId, int index) {
    new InvenioRdmClient.RecordResponse(
        id,
        new InvenioRdmClient.HitLinks("https://zenodo.org/records/" + id),
        new InvenioRdmClient.RecordMetadata("Title " + id, "2025-01-01", "desc", [], null),
        "2025-01-01T10:00:00Z", "2025-01-01T10:00:00Z", true,
        new InvenioRdmClient.RecordAccess("public", "public", "open"),
        new InvenioRdmClient.Pids(new InvenioRdmClient.PidEntry("10.5281/zenodo." + id)),
        new InvenioRdmClient.RecordVersions(isLatest, index),
        parentId == null ? null : new InvenioRdmClient.Parent(parentId, null))
  }

  private static InvenioRdmDatasetSource createSource(InvenioRdmClient client) {
    def credentialRepo = [
        findByUserIdAndSourceTypeAndInstanceId: { u, st, iid -> Optional.empty() }
    ] as UserExternalCredentialRepository
    def encryptor = [decrypt: { token -> new char[0] }] as CredentialEncryptor
    new InvenioRdmDatasetSource(client, credentialRepo, encryptor)
  }

  def "returns the record as-is when it is the latest version"() {
    given:
    def source = createSource([
        getRecord: { url, id, token -> record("111", true, "parent-1", 1) }
    ] as InvenioRdmClient)

    when:
    def resolved = source.resolveLatest("111", CONFIG, "user-1")

    then:
    resolved.isPresent()
    def rr = resolved.get()
    rr.externalHandleValue() == "111"
    rr.metadata().title() == "Title 111"
    rr.metadata().pid() == "10.5281/zenodo.111"
    rr.metadata().version() == "v1"
    rr.metadata() instanceof life.qbic.projectmanagement.domain.model.associated_dataset.InvenioRdmResourceMetadata
  }

  def "follows the parent recid when the stored record is superseded"() {
    given:
    def calls = []
    def source = createSource([
        getRecord: { url, id, token ->
          calls.add(id)
          id == "111"
              ? record("111", false, "parent-1", 1)
              : record("222", true, "parent-1", 2)
        }
    ] as InvenioRdmClient)

    when:
    def resolved = source.resolveLatest("111", CONFIG, "user-1")

    then:
    calls == ["111", "parent-1"]
    def rr = resolved.get()
    rr.externalHandleValue() == "222"
    rr.metadata().pid() == "10.5281/zenodo.222"
    rr.metadata().version() == "v2"
    rr.metadata().title() == "Title 222"
    (rr.metadata() as InvenioRdmResourceMetadata).parentHandle() == "parent-1"
  }

  def "returns empty when the record does not exist (404)"() {
    given:
    def source = createSource([
        getRecord: { url, id, token ->
          throw new InvenioRdmClient.InvenioRdmPermanentException("gone", 404, url)
        }
    ] as InvenioRdmClient)

    expect:
    source.resolveLatest("111", CONFIG, "user-1").isEmpty()
  }

  def "throws DatasetAccessDeniedException on 403"() {
    given:
    def source = createSource([
        getRecord: { url, id, token ->
          throw new InvenioRdmClient.InvenioRdmPermanentException("forbidden", 403, url)
        }
    ] as InvenioRdmClient)

    when:
    source.resolveLatest("111", CONFIG, "user-1")

    then:
    thrown(DatasetAccessDeniedException)
  }

  def "throws DatasetResolveException for transient failures after retries"() {
    given:
    def source = createSource([
        getRecord: { url, id, token ->
          throw new InvenioRdmClient.InvenioRdmTransientException(
              "boom", 503, 3, new RuntimeException("x"), url)
        }
    ] as InvenioRdmClient)

    when:
    source.resolveLatest("111", CONFIG, "user-1")

    then:
    thrown(DatasetResolveException)
  }

  def "metadata access level is derived from the record access block"() {
    given:
    def source = createSource([
        getRecord: { url, id, token ->
          new InvenioRdmClient.RecordResponse(
              "111",
              new InvenioRdmClient.HitLinks("https://zenodo.org/records/111"),
              new InvenioRdmClient.RecordMetadata("T", "2025-01-01", null, [], null),
              "2025-01-01T10:00:00Z", "2025-01-01T10:00:00Z", true,
              new InvenioRdmClient.RecordAccess("restricted", "restricted", "restricted"),
              new InvenioRdmClient.Pids(new InvenioRdmClient.PidEntry("10.5281/zenodo.111")),
              new InvenioRdmClient.RecordVersions(true, 1),
              new InvenioRdmClient.Parent("parent-1", null))
        }
    ] as InvenioRdmClient)

    when:
    def resolved = source.resolveLatest("111", CONFIG, "user-1")

    then:
    (resolved.get().metadata() as InvenioRdmResourceMetadata)
        .deriveAccessLevel() == AccessLevel.RESTRICTED
  }
}