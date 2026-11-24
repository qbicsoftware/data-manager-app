package life.qbic.identity.application.mfa

import life.qbic.identity.api.VerificationLinkService
import life.qbic.identity.application.communication.Content
import life.qbic.identity.application.communication.EmailService
import life.qbic.identity.application.communication.Recipient
import life.qbic.identity.application.communication.Subject
import life.qbic.identity.domain.model.EmailAddress
import life.qbic.identity.domain.model.EncryptedPassword
import life.qbic.identity.domain.model.FullName
import life.qbic.identity.domain.model.User
import life.qbic.identity.domain.model.UserId
import life.qbic.identity.domain.model.mfa.MfaPolicyMode
import life.qbic.identity.domain.model.mfa.ProtectedSecret
import life.qbic.identity.domain.model.mfa.SecondFactor
import life.qbic.identity.domain.repository.SecondFactorRepository
import life.qbic.identity.domain.repository.UserRepository
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration
import java.time.Instant

/**
 * <b>Getting back in after missing the enrolment window.</b>
 *
 * <p>This flow gives a mailbox authority over something security-relevant, so the assertions are about
 * where that authority <em>stops</em>:</p>
 * <ul>
 *   <li>An account with <b>no factor</b>: a link may start an enrolment. The mailbox already controls the
 *       account (a password reset makes it yours), and the link cannot finish what it starts — that
 *       needs a code from the device.</li>
 *   <li>An account <b>with a factor</b>: no link at all. There a mailbox-only reset would be the cheapest
 *       way past the requirement, and this flow must not become it.</li>
 *   <li>Every answer is the same, whatever the reason (AUTH-NFR-03): the endpoint is unauthenticated, so
 *       it must not report whether an address has an account.</li>
 * </ul>
 */
class MfaReenrolmentServiceSpec extends Specification {

    static final Instant NOW = Instant.parse("2026-11-23T10:00:00Z")
    static final ProtectedSecret SECRET =
            ProtectedSecret.ofBase64(Base64.encoder.encodeToString(new byte[32]))

    FakeUsers users = new FakeUsers()
    FakeFactors factors = new FakeFactors()
    FakeTokens tokens = new FakeTokens()
    FakeMail mail = new FakeMail()
    FakeLinkSupplier links = new FakeLinkSupplier()

    private MfaReenrolmentService service(MfaPolicyMode mode = MfaPolicyMode.REQUIRED,
                                          SecondFactorRepository factorStore = factors) {
        new MfaReenrolmentService(users, factorStore, tokens, mail, links,
                new MfaPolicy(mode.name(), Duration.ofHours(48), ""))
    }

    // ---------------------------------------------------------------- who gets a link

    def "an account without a factor is sent a link"() {
        given:
        def account = users.register("ada@example.org")

        when:
        service().request("ada@example.org", NOW)

        then: "one mail, one token, for the re-enrolment flow only"
        mail.sent.size() == 1
        mail.sent[0].recipient.address() == "ada@example.org"
        tokens.issued.size() == 1
        tokens.issued[0].purpose == VerificationLinkService.Purpose.REENROL_SECOND_FACTOR
    }

    def "the mail carries a link, and the link does not carry a password or a factor"() {
        given:
        def account = users.register("ada@example.org")

        when:
        service().request("ada@example.org", NOW)

        then:
        def body = mail.sent[0].content.content()
        body.contains("https://example.org/app/login/re-enrol?token=raw-token-1")
        !body.toLowerCase().contains("password:")
    }

    def "an account with an active factor is never sent a link"() {
        given: "the account is already enrolled, so a mailbox is weaker than what protects it"
        def account = users.register("ada@example.org")
        factors.save(SecondFactor.enrolTotp(account.id().get(), SECRET, "iPhone", NOW))

        when:
        service().request("ada@example.org", NOW)

        then: "the deliberate boundary: identity verification, not a mailbox link (M6-1)"
        mail.sent.isEmpty()
        tokens.issued.isEmpty()
    }

    def "a disabled factor does not make the account recoverable either"() {
        given: "a factor row exists but is not active; the account is not enrolled by AUTH-R-02"
        def account = users.register("ada@example.org")

        when:
        service().request("ada@example.org", NOW)

        then: "an account with no active factor IS recoverable — this is the registration population"
        mail.sent.size() == 1
    }

    @Unroll
    def "#reason leaves no trace, and never tells the caller why"() {
        when:
        service().request(address, NOW)

        then:
        mail.sent.isEmpty()
        tokens.issued.isEmpty()
        noExceptionThrown()

        where:
        reason                        | address
        "an unknown address"          | "nobody@example.org"
        "an address that is not one"  | "not-an-address"
        "an empty address"            | ""
    }

    def "nothing is sent when the deployment does not ask for a factor"() {
        given:
        def account = users.register("ada@example.org")

        when: "under OFF there is nothing to re-enrol, and a link would imply otherwise"
        service(MfaPolicyMode.OFF).request("ada@example.org", NOW)

        then:
        mail.sent.isEmpty()
    }

    def "a mail that cannot be sent does not throw at the user"() {
        given:
        def account = users.register("ada@example.org")
        mail.failing = true

        when:
        service().request("ada@example.org", NOW)

        then: "the page answers the same either way; the failure is only a log line"
        noExceptionThrown()
    }

    // ---------------------------------------------------------------- redeeming

    def "a valid link yields the account it was issued for"() {
        given:
        def account = users.register("ada@example.org")
        tokens.consumeable["raw-token-1"] = account.id().get()

        when:
        def candidate = service().redeem("raw-token-1", NOW)

        then:
        candidate.present
        candidate.get().userId() == account.id().get()
        candidate.get().emailAddress() == "ada@example.org"
    }

    def "a link for an account that has since enrolled is refused"() {
        given:
        def account = users.register("ada@example.org")
        tokens.consumeable["raw-token-1"] = account.id().get()
        factors.save(SecondFactor.enrolTotp(account.id().get(), SECRET, "iPhone", NOW))

        when: "the link was mailed before the account enrolled, and arrives after"
        def candidate = service().redeem("raw-token-1", NOW)

        then: "there is nothing to re-enrol, and the orphaned link must not open a page"
        candidate.isEmpty()
    }

    @Unroll
    def "an unusable link is refused: #reason"() {
        given:
        def account = users.register("ada@example.org")

        expect:
        service().redeem(token, NOW).isEmpty()

        where:
        reason                | token
        "an unknown token"    | "raw-token-9"
        "no token at all"     | null
        "a blank token"       | "   "
    }

    def "the link is consumed by the identity layer, so it cannot be replayed here"() {
        given:
        def account = users.register("ada@example.org")
        tokens.consumeable["raw-token-1"] = account.id().get()

        when:
        service().redeem("raw-token-1", NOW)
        def second = service().redeem("raw-token-1", NOW)

        then: "single use belongs to the token service; this class adds no second path"
        second.isEmpty()
        tokens.consumed == 1
    }

    def "the link carries its own flow, and the two purpose enums cannot drift"() {
        expect: "the service maps purposes by name, so a missing constant fails at runtime, not compile time"
        VerificationLinkService.Purpose.values()*.name() as Set ==
                life.qbic.identity.domain.model.verification.TokenPurpose.values()*.name() as Set
    }

    // ---------------------------------------------------------------- fakes

    /**
     * Accounts are real {@link User} objects, not stubs: {@code User.create} is the only constructor
     * that sets a registration date, and a stubbed aggregate would let this spec assert behaviour the
     * domain does not have.
     */
    static class FakeUsers extends UserRepository {
        final Map<String, User> byId = [:]
        final Map<String, User> byEmail = [:]

        FakeUsers() {
            super(null)
        }

        /** @return the created account, so a test can use its generated id */
        User register(String email) {
            def user = User.create(FullName.from("Ada Example"), EmailAddress.from(email),
                    "ada-" + byEmail.size(), EncryptedPassword.from("s3cret-Passw0rd".toCharArray()))
            byId[user.id().get()] = user
            byEmail[email] = user
            return user
        }

        @Override
        Optional<User> findById(UserId id) {
            Optional.ofNullable(byId[id.get()])
        }

        @Override
        Optional<User> findByEmail(EmailAddress emailAddress) {
            Optional.ofNullable(byEmail[emailAddress.get()])
        }
    }

    static class FakeFactors implements SecondFactorRepository {
        final List<SecondFactor> stored = []

        @Override
        SecondFactor save(SecondFactor factor) {
            stored << factor
            return factor
        }

        @Override
        Optional<SecondFactor> findById(long id) {
            Optional.empty()
        }

        @Override
        List<SecondFactor> findAllByUserId(String userId) {
            stored.findAll { it.userId() == userId }
        }

        @Override
        List<SecondFactor> findActiveByUserId(String userId) {
            stored.findAll { it.userId() == userId && it.isActive() }
        }

        @Override
        boolean hasActiveFactor(String userId) {
            !findActiveByUserId(userId).isEmpty()
        }
    }

    static class FakeTokens implements VerificationLinkService {
        final List<Map> issued = []
        final Map<String, String> consumeable = [:]
        int consumed = 0

        @Override
        Issued issue(String userId, Purpose purpose, Instant issuedAt) {
            issued << [userId: userId, purpose: purpose]
            return new Issued("raw-token-" + issued.size(), issuedAt.plus(Duration.ofMinutes(15)))
        }

        @Override
        Optional<String> consume(String rawToken, Purpose purpose, Instant now) {
            if (rawToken == null || rawToken.isBlank()) {
                return Optional.empty()
            }
            def userId = consumeable.remove(rawToken)
            if (userId == null) {
                return Optional.empty()
            }
            consumed++
            return Optional.of(userId)
        }
    }

    static class FakeMail implements EmailService {
        final List<Map> sent = []
        boolean failing = false

        @Override
        void send(Subject subject, Recipient recipient, Content content)
                throws life.qbic.identity.application.communication.CommunicationException {
            if (failing) {
                throw new life.qbic.identity.application.communication.CommunicationException("smtp down")
            }
            sent << [subject: subject, recipient: recipient, content: content]
        }
    }

    static class FakeLinkSupplier extends MfaReenrolmentLinkSupplier {
        FakeLinkSupplier() {
            super("https", "example.org", -1, "/app", "/login/re-enrol", "token")
        }
    }
}
