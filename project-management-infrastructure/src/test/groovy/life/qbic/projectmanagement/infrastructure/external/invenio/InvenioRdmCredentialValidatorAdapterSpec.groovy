package life.qbic.projectmanagement.infrastructure.external.invenio

import life.qbic.projectmanagement.application.associated_dataset.CredentialValidationException
import life.qbic.projectmanagement.application.associated_dataset.InstanceConfig
import spock.lang.Specification

/**
 * Unit tests for {@link InvenioRdmCredentialValidatorAdapter}.
 *
 * <p>Verifies the {@link life.qbic.projectmanagement.infrastructure.external.CredentialValidatorAdapter}
 * contract: the adapter copies the token into a local array for the
 * auth header and zeroes only that copy. The original token array is
 * <em>not</em> zeroed by the adapter — the caller retains ownership
 * of the token lifecycle.</p>
 *
 * @since 1.12.0
 */
class InvenioRdmCredentialValidatorAdapterSpec extends Specification {

    static final InstanceConfig CONFIG = new InstanceConfig(
        "zenodo", "Zenodo (zenodo.org)", "https://zenodo.org")

    // ── Token ownership contract ──────────────────────────────────────
    // The adapter copies the token into a local array for the auth
    // header and zeroes only that copy. The original token array is
    // NOT zeroed by the adapter — the caller retains ownership and is
    // responsible for zeroing it after all operations are complete.

    def "original token array is preserved after successful validation"() {
        given: "a client that returns 200 (valid token)"
        def client = Mock(InvenioRdmClient)
        client.getAuthenticatedUser(CONFIG.baseUrl(), _) >>
            new InvenioRdmClient.AuthenticatedUserResponse("1", "user", "user@example.org")
        def adapter = new InvenioRdmCredentialValidatorAdapter(client)
        def token = "my-secret-token".toCharArray()
        def expected = token.clone()

        when:
        def result = adapter.validate(CONFIG, token)

        then:
        result
        token == expected  // original token is untouched
    }

    def "original token array is preserved after invalid token (401)"() {
        given: "a client that returns 401 (invalid token)"
        def client = Mock(InvenioRdmClient)
        client.getAuthenticatedUser(CONFIG.baseUrl(), _) >> {
            throw new InvenioRdmClient.InvenioRdmPermanentException(
                "Unauthorized", 401, CONFIG.baseUrl() + "/api/users")
        }
        def adapter = new InvenioRdmCredentialValidatorAdapter(client)
        def token = "expired-token-xyz".toCharArray()
        def expected = token.clone()

        when:
        def result = adapter.validate(CONFIG, token)

        then:
        !result
        token == expected  // original token is untouched
    }

    def "original token array is preserved after forbidden token (403)"() {
        given: "a client that returns 403"
        def client = Mock(InvenioRdmClient)
        client.getAuthenticatedUser(CONFIG.baseUrl(), _) >> {
            throw new InvenioRdmClient.InvenioRdmPermanentException(
                "Forbidden", 403, CONFIG.baseUrl() + "/api/users")
        }
        def adapter = new InvenioRdmCredentialValidatorAdapter(client)
        def token = "forbidden-token".toCharArray()
        def expected = token.clone()

        when:
        def result = adapter.validate(CONFIG, token)

        then:
        !result
        token == expected  // original token is untouched
    }

    def "original token array is preserved after transient error"() {
        given: "a client that throws a transient error (5xx after retries)"
        def client = Mock(InvenioRdmClient)
        client.getAuthenticatedUser(CONFIG.baseUrl(), _) >> {
            throw new InvenioRdmClient.InvenioRdmTransientException(
                "Server error", 500, 3, null,
                CONFIG.baseUrl() + "/api/users")
        }
        def adapter = new InvenioRdmCredentialValidatorAdapter(client)
        def token = "token-on-failing-server".toCharArray()
        def expected = token.clone()

        when:
        adapter.validate(CONFIG, token)

        then:
        thrown(CredentialValidationException)
        token == expected  // original token is untouched
    }

    def "original token array is preserved after interrupted exception"() {
        given: "a client that throws an interrupted exception"
        def client = Mock(InvenioRdmClient)
        client.getAuthenticatedUser(CONFIG.baseUrl(), _) >> {
            throw new InvenioRdmClient.InvenioRdmInterruptedException(
                "Interrupted", new InterruptedException(),
                CONFIG.baseUrl() + "/api/users")
        }
        def adapter = new InvenioRdmCredentialValidatorAdapter(client)
        def token = "token-on-interrupted".toCharArray()
        def expected = token.clone()

        when:
        adapter.validate(CONFIG, token)

        then:
        thrown(CredentialValidationException)
        token == expected  // original token is untouched
        Thread.interrupted() // clear the interrupt flag for test hygiene
    }

    def "original token array is preserved after unexpected permanent error (e.g. 404)"() {
        given: "a client that throws a non-auth permanent error"
        def client = Mock(InvenioRdmClient)
        client.getAuthenticatedUser(CONFIG.baseUrl(), _) >> {
            throw new InvenioRdmClient.InvenioRdmPermanentException(
                "Not Found", 404, CONFIG.baseUrl() + "/api/users")
        }
        def adapter = new InvenioRdmCredentialValidatorAdapter(client)
        def token = "token-on-bad-endpoint".toCharArray()
        def expected = token.clone()

        when:
        adapter.validate(CONFIG, token)

        then:
        thrown(CredentialValidationException)
        token == expected  // original token is untouched
    }

    // ── Validation result mapping ────────────────────────────────────

    def "200 response returns true"() {
        given:
        def client = Mock(InvenioRdmClient)
        client.getAuthenticatedUser(CONFIG.baseUrl(), _) >>
            new InvenioRdmClient.AuthenticatedUserResponse("42", "alice", "alice@example.org")
        def adapter = new InvenioRdmCredentialValidatorAdapter(client)

        expect:
        adapter.validate(CONFIG, "valid-token".toCharArray())
    }

    def "401 response returns false"() {
        given:
        def client = Mock(InvenioRdmClient)
        client.getAuthenticatedUser(CONFIG.baseUrl(), _) >> {
            throw new InvenioRdmClient.InvenioRdmPermanentException(
                "Unauthorized", 401, CONFIG.baseUrl() + "/api/users")
        }
        def adapter = new InvenioRdmCredentialValidatorAdapter(client)

        expect:
        !adapter.validate(CONFIG, "bad-token".toCharArray())
    }

    def "403 response returns false"() {
        given:
        def client = Mock(InvenioRdmClient)
        client.getAuthenticatedUser(CONFIG.baseUrl(), _) >> {
            throw new InvenioRdmClient.InvenioRdmPermanentException(
                "Forbidden", 403, CONFIG.baseUrl() + "/api/users")
        }
        def adapter = new InvenioRdmCredentialValidatorAdapter(client)

        expect:
        !adapter.validate(CONFIG, "forbidden-token".toCharArray())
    }

    def "transient error throws CredentialValidationException"() {
        given:
        def client = Mock(InvenioRdmClient)
        client.getAuthenticatedUser(CONFIG.baseUrl(), _) >> {
            throw new InvenioRdmClient.InvenioRdmTransientException(
                "Server error", 500, 3, null,
                CONFIG.baseUrl() + "/api/users")
        }
        def adapter = new InvenioRdmCredentialValidatorAdapter(client)

        when:
        adapter.validate(CONFIG, "token".toCharArray())

        then:
        def e = thrown(CredentialValidationException)
        e.message.contains("transient error")
    }

    def "interrupted exception re-sets interrupt flag and throws CredentialValidationException"() {
        given:
        def client = Mock(InvenioRdmClient)
        client.getAuthenticatedUser(CONFIG.baseUrl(), _) >> {
            throw new InvenioRdmClient.InvenioRdmInterruptedException(
                "Interrupted", new InterruptedException(),
                CONFIG.baseUrl() + "/api/users")
        }
        def adapter = new InvenioRdmCredentialValidatorAdapter(client)

        when:
        adapter.validate(CONFIG, "token".toCharArray())

        then:
        thrown(CredentialValidationException)
        Thread.interrupted() // clear the interrupt flag
    }

    def "unexpected permanent error (non-401/403) throws CredentialValidationException"() {
        given:
        def client = Mock(InvenioRdmClient)
        client.getAuthenticatedUser(CONFIG.baseUrl(), _) >> {
            throw new InvenioRdmClient.InvenioRdmPermanentException(
                "Not Found", 404, CONFIG.baseUrl() + "/api/users")
        }
        def adapter = new InvenioRdmCredentialValidatorAdapter(client)

        when:
        adapter.validate(CONFIG, "token".toCharArray())

        then:
        def e = thrown(CredentialValidationException)
        e.message.contains("status 404")
    }

    // ── Auth header construction ─────────────────────────────────────

    def "passes Bearer auth header to client"() {
        given:
        def client = Mock(InvenioRdmClient)
        def adapter = new InvenioRdmCredentialValidatorAdapter(client)
        def token = "my-token-123".toCharArray()

        when:
        adapter.validate(CONFIG, token)

        then:
        1 * client.getAuthenticatedUser(CONFIG.baseUrl(), "Bearer my-token-123") >>
            new InvenioRdmClient.AuthenticatedUserResponse("1", "user", "u@e.org")
    }

    // ── Null safety ──────────────────────────────────────────────────

    def "null config throws NullPointerException"() {
        given:
        def adapter = new InvenioRdmCredentialValidatorAdapter(Mock(InvenioRdmClient))

        when:
        adapter.validate(null, "token".toCharArray())

        then:
        thrown(NullPointerException)
    }

    def "null token throws NullPointerException"() {
        given:
        def adapter = new InvenioRdmCredentialValidatorAdapter(Mock(InvenioRdmClient))

        when:
        adapter.validate(CONFIG, null)

        then:
        thrown(NullPointerException)
    }

    def "null client throws NullPointerException at construction"() {
        when:
        new InvenioRdmCredentialValidatorAdapter(null)

        then:
        thrown(NullPointerException)
    }
}
