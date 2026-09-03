package life.qbic.projectmanagement.infrastructure.external.invenio

import life.qbic.projectmanagement.application.associated_dataset.CredentialEncryptor
import life.qbic.projectmanagement.application.associated_dataset.DatasetAccessFilter
import life.qbic.projectmanagement.application.associated_dataset.InstanceConfig
import life.qbic.projectmanagement.application.associated_dataset.SearchQuery
import life.qbic.projectmanagement.domain.model.associated_dataset.CredentialStatus
import life.qbic.projectmanagement.domain.model.associated_dataset.SourceType
import life.qbic.projectmanagement.domain.model.associated_dataset.UserExternalCredential
import life.qbic.projectmanagement.domain.model.associated_dataset.repository.UserExternalCredentialRepository
import spock.lang.Specification

/**
 * Unit tests for {@link InvenioRdmDatasetSource#hasValidCredential}
 * and the access-filter pass-through in
 * {@link InvenioRdmDatasetSource#search}.
 *
 * @since 1.12.0
 */
class InvenioRdmDatasetSourceSpec extends Specification {

  private static final String USER_ID = "user-1"
  private static final String INSTANCE_ID = "zenodo"
  private static final InstanceConfig CONFIG =
      new InstanceConfig(INSTANCE_ID, "Zenodo", "https://zenodo.org")

  // ── hasValidCredential ──────────────────────────────────────────

  def "hasValidCredential returns true when credential exists and is VALID"() {
    given:
    def credentialRepo = Mock(UserExternalCredentialRepository) {
      findByUserIdAndSourceTypeAndInstanceId(USER_ID, SourceType.INVENIO_RDM, INSTANCE_ID) >>
          Optional.of(createCredential(CredentialStatus.VALID))
    }
    def source = createSource(credentialRepo)

    expect:
    source.hasValidCredential(USER_ID, CONFIG)
  }

  def "hasValidCredential returns false when credential is INVALIDATED"() {
    given:
    def credentialRepo = Mock(UserExternalCredentialRepository) {
      findByUserIdAndSourceTypeAndInstanceId(USER_ID, SourceType.INVENIO_RDM, INSTANCE_ID) >>
          Optional.of(createCredential(CredentialStatus.INVALIDATED))
    }
    def source = createSource(credentialRepo)

    expect:
    !source.hasValidCredential(USER_ID, CONFIG)
  }

  def "hasValidCredential returns false when no credential exists"() {
    given:
    def credentialRepo = Mock(UserExternalCredentialRepository) {
      findByUserIdAndSourceTypeAndInstanceId(USER_ID, SourceType.INVENIO_RDM, INSTANCE_ID) >>
          Optional.empty()
    }
    def source = createSource(credentialRepo)

    expect:
    !source.hasValidCredential(USER_ID, CONFIG)
  }

  def "hasValidCredential returns false for null userId"() {
    given:
    def source = createSource(Mock(UserExternalCredentialRepository))

    expect:
    !source.hasValidCredential(null, CONFIG)
  }

  def "hasValidCredential returns false for null config"() {
    given:
    def source = createSource(Mock(UserExternalCredentialRepository))

    expect:
    !source.hasValidCredential(USER_ID, null)
  }

  // ── SearchQuery accessFilter pass-through ───────────────────────

  def "search passes accessFilter from SearchQuery to client SearchParams"() {
    given: "a source with a mock client that captures the SearchParams"
    def capturedParams = new Object() { InvenioRdmClient.SearchParams value }
    def mockClient = Mock(InvenioRdmClient) {
      search(_, _, _) >> { String url, InvenioRdmClient.SearchParams params, char[] auth ->
        capturedParams.value = params
        // Return a minimal valid response
        return new InvenioRdmClient.SearchResultResponse(
            new InvenioRdmClient.SearchResultResponse.Hits(0, []))
      }
    }
    def credentialRepo = Mock(UserExternalCredentialRepository) {
      findByUserIdAndSourceTypeAndInstanceId(_, _, _) >> Optional.empty()
    }
    def source = new InvenioRdmDatasetSource(
        mockClient,
        credentialRepo,
        Mock(CredentialEncryptor))

    def query = new SearchQuery("proteomics", 0, 10, DatasetAccessFilter.RESTRICTED)

    when:
    source.search(query, CONFIG, USER_ID)

    then:
    capturedParams.value != null
    capturedParams.value.accessFilter() == DatasetAccessFilter.RESTRICTED
    capturedParams.value.query() == "proteomics"
  }

  def "search passes null accessFilter when not set"() {
    given:
    def capturedParams = new Object() { InvenioRdmClient.SearchParams value }
    def mockClient = Mock(InvenioRdmClient) {
      search(_, _, _) >> { String url, InvenioRdmClient.SearchParams params, char[] auth ->
        capturedParams.value = params
        return new InvenioRdmClient.SearchResultResponse(
            new InvenioRdmClient.SearchResultResponse.Hits(0, []))
      }
    }
    def credentialRepo = Mock(UserExternalCredentialRepository) {
      findByUserIdAndSourceTypeAndInstanceId(_, _, _) >> Optional.empty()
    }
    def source = new InvenioRdmDatasetSource(
        mockClient,
        credentialRepo,
        Mock(CredentialEncryptor))

    def query = new SearchQuery("test", 0, 10)

    when:
    source.search(query, CONFIG, USER_ID)

    then:
    capturedParams.value != null
    capturedParams.value.accessFilter() == null
  }

  // ── createAccessLink ────────────────────────────────────────────

  def "createAccessLink builds full record URL and carries the link id"() {
    given:
    def accessToken = "secret-token-123"
    def mockClient = Mock(InvenioRdmClient) {
      createAccessLink(_, _, _) >>
          new InvenioRdmClient.AccessLinkResponse("link-id", accessToken)
    }
    def credentialRepo = Mock(UserExternalCredentialRepository) {
      findByUserIdAndSourceTypeAndInstanceId(USER_ID, SourceType.INVENIO_RDM, INSTANCE_ID) >>
          Optional.of(createCredential(CredentialStatus.VALID))
    }
    def encryptor = Mock(CredentialEncryptor) {
      decrypt(_) >> "pat-token".toCharArray()
    }
    def source = new InvenioRdmDatasetSource(mockClient, credentialRepo, encryptor)

    when:
    def link = source.createAccessLink("abc-123", CONFIG, USER_ID)

    then:
    link.url() == "https://zenodo.org/records/abc-123?token=secret-token-123"
    link.linkId() == "link-id"
  }

  def "createAccessLink throws when no credential is configured"() {
    given:
    def credentialRepo = Mock(UserExternalCredentialRepository) {
      findByUserIdAndSourceTypeAndInstanceId(USER_ID, SourceType.INVENIO_RDM, INSTANCE_ID) >>
          Optional.empty()
    }
    def source = new InvenioRdmDatasetSource(
        Mock(InvenioRdmClient), credentialRepo, Mock(CredentialEncryptor))

    when:
    source.createAccessLink("abc-123", CONFIG, USER_ID)

    then:
    thrown(life.qbic.projectmanagement.application.associated_dataset.AccessLinkCreationException)
  }

  // ── revokeAccessLink ───────────────────────────────────────────────

  def "revokeAccessLink delegates to the client with the record and link id"() {
    given:
    def mockClient = Mock(InvenioRdmClient)
    def credentialRepo = Mock(UserExternalCredentialRepository) {
      findByUserIdAndSourceTypeAndInstanceId(USER_ID, SourceType.INVENIO_RDM, INSTANCE_ID) >>
          Optional.of(createCredential(CredentialStatus.VALID))
    }
    def encryptor = Mock(CredentialEncryptor) {
      decrypt(_) >> "pat-token".toCharArray()
    }
    def source = new InvenioRdmDatasetSource(mockClient, credentialRepo, encryptor)

    when:
    source.revokeAccessLink("link-id", "abc-123", CONFIG, USER_ID)

    then:
    1 * mockClient.revokeAccessLink("https://zenodo.org", "abc-123",
        "link-id", { char[] t -> new String(t) == 'pat-token' })
  }

  def "revokeAccessLink throws when no credential is configured"() {
    given:
    def credentialRepo = Mock(UserExternalCredentialRepository) {
      findByUserIdAndSourceTypeAndInstanceId(USER_ID, SourceType.INVENIO_RDM, INSTANCE_ID) >>
          Optional.empty()
    }
    def source = new InvenioRdmDatasetSource(
        Mock(InvenioRdmClient), credentialRepo, Mock(CredentialEncryptor))

    when:
    source.revokeAccessLink("link-id", "abc-123", CONFIG, USER_ID)

    then:
    thrown(life.qbic.projectmanagement.application.associated_dataset.AccessLinkRevocationException)
  }

  // ── Helpers ──────────────────────────────────────────────────────

  private InvenioRdmDatasetSource createSource(
      UserExternalCredentialRepository credentialRepo) {
    new InvenioRdmDatasetSource(
        Mock(InvenioRdmClient),
        credentialRepo,
        Mock(CredentialEncryptor))
  }

  private static UserExternalCredential createCredential(CredentialStatus status) {
    new UserExternalCredential(
        USER_ID, SourceType.INVENIO_RDM, INSTANCE_ID,
        new byte[0], status)
  }
}
