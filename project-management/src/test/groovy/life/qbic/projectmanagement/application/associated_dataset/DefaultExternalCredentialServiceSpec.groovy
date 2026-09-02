package life.qbic.projectmanagement.application.associated_dataset

import life.qbic.projectmanagement.domain.model.associated_dataset.CredentialStatus
import life.qbic.projectmanagement.domain.model.associated_dataset.SourceType
import life.qbic.projectmanagement.domain.model.associated_dataset.UserExternalCredential
import life.qbic.projectmanagement.domain.model.associated_dataset.repository.UserExternalCredentialRepository
import spock.lang.Specification

/**
 * Unit tests for
 * {@link DefaultExternalCredentialService#credentialStatusForInstance}.
 *
 * @since 1.12.0
 */
class DefaultExternalCredentialServiceSpec extends Specification {

  private static final String USER_ID = "user-1"
  private static final String INSTANCE_ID = "zenodo"

  def "credentialStatusForInstance returns VALID when credential is VALID"() {
    given:
    def credentialRepo = Mock(UserExternalCredentialRepository) {
      findByUserIdAndSourceTypeAndInstanceId(USER_ID, SourceType.INVENIO_RDM, INSTANCE_ID) >>
          Optional.of(createCredential(CredentialStatus.VALID))
    }
    def service = createService(credentialRepo)

    expect:
    service.credentialStatusForInstance(USER_ID, INSTANCE_ID) == CredentialStatus.VALID
  }

  def "credentialStatusForInstance returns INVALIDATED when credential is INVALIDATED"() {
    given:
    def credentialRepo = Mock(UserExternalCredentialRepository) {
      findByUserIdAndSourceTypeAndInstanceId(USER_ID, SourceType.INVENIO_RDM, INSTANCE_ID) >>
          Optional.of(createCredential(CredentialStatus.INVALIDATED))
    }
    def service = createService(credentialRepo)

    expect:
    service.credentialStatusForInstance(USER_ID, INSTANCE_ID) == CredentialStatus.INVALIDATED
  }

  def "credentialStatusForInstance returns NOT_CONFIGURED when no credential exists"() {
    given:
    def credentialRepo = Mock(UserExternalCredentialRepository) {
      findByUserIdAndSourceTypeAndInstanceId(USER_ID, SourceType.INVENIO_RDM, INSTANCE_ID) >>
          Optional.empty()
    }
    def service = createService(credentialRepo)

    expect:
    service.credentialStatusForInstance(USER_ID, INSTANCE_ID) == CredentialStatus.NOT_CONFIGURED
  }

  def "credentialStatusForInstance returns NOT_CONFIGURED for unknown instance"() {
    given:
    def registry = Mock(SourceInstanceRegistry) {
      find("unknown-instance") >> Optional.empty()
    }
    def service = new DefaultExternalCredentialService(
        Mock(ExternalCredentialValidator),
        Mock(UserExternalCredentialRepository),
        Mock(CredentialEncryptor),
        registry)

    expect:
    service.credentialStatusForInstance(USER_ID, "unknown-instance") == CredentialStatus.NOT_CONFIGURED
  }

  def "credentialStatusForInstance throws NullPointerException for null userId"() {
    given:
    def service = createService(Mock(UserExternalCredentialRepository))

    when:
    service.credentialStatusForInstance(null, INSTANCE_ID)

    then:
    thrown(NullPointerException)
  }

  def "credentialStatusForInstance throws NullPointerException for null instanceId"() {
    given:
    def service = createService(Mock(UserExternalCredentialRepository))

    when:
    service.credentialStatusForInstance(USER_ID, null)

    then:
    thrown(NullPointerException)
  }

  // ── Helpers ──────────────────────────────────────────────────────

  private DefaultExternalCredentialService createService(
      UserExternalCredentialRepository credentialRepo) {
    def registry = Mock(SourceInstanceRegistry) {
      find(INSTANCE_ID) >> Optional.of(
          new SourceInstanceDescriptor(INSTANCE_ID, "Zenodo",
              "https://zenodo.org", SourceType.INVENIO_RDM))
    }
    new DefaultExternalCredentialService(
        Mock(ExternalCredentialValidator),
        credentialRepo,
        Mock(CredentialEncryptor),
        registry)
  }

  private static UserExternalCredential createCredential(CredentialStatus status) {
    new UserExternalCredential(
        USER_ID, SourceType.INVENIO_RDM, INSTANCE_ID,
        new byte[0], status)
  }
}
