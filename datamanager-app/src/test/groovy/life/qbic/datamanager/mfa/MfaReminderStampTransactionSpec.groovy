package life.qbic.datamanager.mfa

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import life.qbic.identity.domain.model.mfa.MfaGrace
import life.qbic.identity.domain.model.mfa.MfaReminder
import life.qbic.identity.infrastructure.mfa.JpaMfaGraceRepository
import org.hibernate.boot.MetadataSources
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import org.hibernate.cfg.AvailableSettings
import spock.lang.Shared
import spock.lang.Specification

import java.time.Duration
import java.time.Instant

/**
 * <b>The reminder stamp must commit on its own, or the same account is mailed on every pass.</b>
 *
 * <p>Reported from a deployed instance in {@code REQUIRED} mode: testers were "swamped" with
 * enrolment reminders. The reminder design allows at most two mails per grace window (a day before,
 * and two hours before) because each stage records a stamp on the window and
 * {@code MfaReminder.due} will not return a stage whose stamp is set. Two mails cannot swamp
 * anybody, so the stamp was not being written.</p>
 *
 * <p><b>Why.</b> {@code JpaMfaGraceRepository} runs on a container-managed {@code EntityManager}, and
 * {@code markDueReminderSent} / {@code markFinalReminderSent} are JPQL bulk updates. Hibernate refuses
 * a bulk update without an active transaction ({@code TransactionRequiredException}). Nothing supplied
 * one: {@code MfaReminderService.sendDueReminders} carried no {@code @Transactional}, and the
 * application runs with {@code spring.jpa.open-in-view=false}. The <em>read</em> needs no transaction,
 * so the pass selected the window, sent the mail — and then threw when it tried to record that it had.
 *
 * <p>The job catches that exception per pass and logs it, so the failure was invisible from the
 * outside: every five minutes, a mail, and no record of it. This is the same defect shape as
 * {@code AuthAttemptThrottleTransactionSpec}, which exists because the login throttle had the identical
 * hole.</p>
 *
 * <h3>Why the transaction belongs on the stamp, not on the pass</h3>
 * <p>One transaction around the whole pass would be worse than the bug: the stamps of accounts already
 * mailed would roll back together with any later failure, re-mailing all of them. The interface's
 * contract says a stamp is "never rolled back" — only an independent commit can keep that promise, and
 * only {@code REQUIRES_NEW} gives the loop an independent commit without a second bean.</p>
 *
 * @since 1.0.0
 */
class MfaReminderStampTransactionSpec extends Specification {

    @Shared
    EntityManagerFactory emf

    static final Instant DEADLINE = Instant.parse("2026-11-24T09:00:00Z")
    static final Duration WINDOW = Duration.ofHours(48)
    static final Duration SOON = Duration.ofHours(24)
    static final Duration FINAL = Duration.ofHours(2)
    /** The moment the "about a day left" reminder becomes due, minus a little. */
    static final Instant PASS_AT = DEADLINE.minus(Duration.ofHours(12))
    static final String USER = "u-1"

    def setupSpec() {
        def builder = new StandardServiceRegistryBuilder()
                .applySetting(AvailableSettings.JAKARTA_JDBC_URL,
                        "jdbc:h2:mem:reminderstamp;MODE=MySQL;DB_CLOSE_DELAY=-1")
                .applySetting(AvailableSettings.JAKARTA_JDBC_USER, "sa")
                .applySetting(AvailableSettings.JAKARTA_JDBC_PASSWORD, "")
                .applySetting(AvailableSettings.JAKARTA_JDBC_DRIVER, "org.h2.Driver")
                .applySetting(AvailableSettings.HBM2DDL_AUTO, "create-drop")
        emf = new MetadataSources(builder.build())
                .addAnnotatedClass(MfaGrace)
                .buildMetadata()
                .buildSessionFactory()
    }

    def cleanupSpec() {
        emf?.close()
    }

    def setup() {
        inTransaction { EntityManager em ->
            em.createNativeQuery("DELETE FROM user_mfa_grace").executeUpdate()
        }
    }

    // ---------------------------------------------------------------- the defect

    def "the read works without a transaction, which is why the mail goes out at all"() {
        // The asymmetry is the whole bug: the query that decides to send needs nothing, and the write
        // that remembers it was sent needs a transaction. A pass that owns neither succeeds at the
        // first half and fails at the second.
        given:
        openWindow(USER)

        expect:
        repositoryWithoutTransaction().findActiveWithOutstandingReminder(PASS_AT).size() == 1
    }

    def "the stamp throws when written without a transaction, so nothing records the mail"() {
        given: 'a window with no stamp — exactly what the deployed pass saw'
        openWindow(USER)
        def repository = repositoryWithoutTransaction()

        when: 'the marking call, with no transaction around it, as the scheduled pass made it'
        repository.markDueReminderSent(USER, PASS_AT)

        then: 'Hibernate refuses the bulk update'
        thrown(Exception)

        and: 'so the window is still outstanding — this is the flood'
        repositoryWithoutTransaction().findActiveWithOutstandingReminder(PASS_AT).size() == 1
        reloaded(USER).reminderDueSentAt() == null
    }

    def "every pass therefore re-sends the same stage to the same account"() {
        // The symptom as the tester saw it: not one mail, not two, but one per pass for as long as the
        // window is open. With the job's default interval that is twelve mails an hour.
        given:
        openWindow(USER)
        def repository = repositoryWithoutTransaction()

        when: 'the job runs three times and the stamp fails each time'
        3.times {
            try {
                repository.markDueReminderSent(USER, PASS_AT)
            } catch (Exception ignored) {
                // MfaReminderJob logs and swallows per pass; that is why nobody saw this in the log
                // as anything but three one-line errors.
            }
        }

        then: 'all three passes would have sent, because the stage is still due'
        (1..3).every {
            MfaReminder.due(reloaded(USER), PASS_AT.plus(Duration.ofMinutes(5 * it)), SOON, FINAL)
                    == MfaReminder.Stage.SOON
        }
    }

    // ---------------------------------------------------------------- the contract that fixes it

    def "both stamps declare an independent transaction"() {
        // Reflection rather than a Spring context, matching AuthAttemptThrottleTransactionSpec: the
        // annotation is the fix, and a rename that drops it must fail here rather than in production.
        expect:
        requiresNew('markDueReminderSent')
        requiresNew('markFinalReminderSent')
    }

    def "the stamp is not merely transactional, it is REQUIRES_NEW"() {
        // REQUIRED would join the pass's transaction — or start one that the caller's failure rolls
        // back. Only REQUIRES_NEW commits the record of a mail that has already left the building.
        expect:
        propagationOf('markDueReminderSent') == org.springframework.transaction.annotation.Propagation.REQUIRES_NEW
        propagationOf('markFinalReminderSent') == org.springframework.transaction.annotation.Propagation.REQUIRES_NEW
    }

    // ---------------------------------------------------------------- the behaviour after the fix

    def "once the stamp commits, the next pass has nothing to send"() {
        given: 'a stamp written the way the annotation makes it happen — in its own transaction'
        openWindow(USER)
        markDueInOwnTransaction(USER, PASS_AT)

        expect: 'the committed stamp is readable, so the record survives the pass that wrote it'
        reloaded(USER).reminderDueSentAt() != null

        and: 'the decision sends nothing, which is what a user experiences'
        MfaReminder.due(reloaded(USER), PASS_AT.plus(Duration.ofMinutes(5)), SOON, FINAL)
                == MfaReminder.Stage.NONE

        and: 'the window is still a candidate — the final reminder is still outstanding'
        // Stated deliberately, because it looks like a contradiction and is not: the query is a cheap
        // pre-filter ("something might be owed here"), not the authority. Requiring both stamps to be
        // set before selecting would drop every window whose final reminder is due, and the mail that
        // matters most — the one two hours before the deadline — would never be sent.
        repositoryWithoutTransaction().findActiveWithOutstandingReminder(PASS_AT).size() == 1
    }

    def "the final stamp is recorded independently of the first"() {
        given:
        openWindow(USER)
        markDueInOwnTransaction(USER, PASS_AT)
        def nearDeadline = DEADLINE.minus(Duration.ofMinutes(30))

        when:
        markFinalInOwnTransaction(USER, nearDeadline)

        then: 'both stages are spent, and no pass sends anything again'
        reloaded(USER).reminderDueSentAt() != null
        reloaded(USER).reminderFinalSentAt() != null
        MfaReminder.due(reloaded(USER), nearDeadline, SOON, FINAL) == MfaReminder.Stage.NONE
        repositoryWithoutTransaction().findActiveWithOutstandingReminder(nearDeadline).isEmpty()
    }

    // ---------------------------------------------------------------- helpers

    private void openWindow(String userId) {
        inTransaction { EntityManager em ->
            em.persist(MfaGrace.start(userId, DEADLINE.minus(WINDOW), WINDOW))
        }
    }

    private JpaMfaGraceRepository repositoryWithoutTransaction() {
        new JpaMfaGraceRepository(emf.createEntityManager())
    }

    private void markDueInOwnTransaction(String userId, Instant at) {
        inTransaction { EntityManager em ->
            new JpaMfaGraceRepository(em).markDueReminderSent(userId, at)
        }
    }

    private void markFinalInOwnTransaction(String userId, Instant at) {
        inTransaction { EntityManager em ->
            new JpaMfaGraceRepository(em).markFinalReminderSent(userId, at)
        }
    }

    private MfaGrace reloaded(String userId) {
        inTransaction { EntityManager em -> em.find(MfaGrace, userId) }
    }

    private <T> T inTransaction(Closure<T> work) {
        EntityManager em = emf.createEntityManager()
        def tx = em.transaction
        tx.begin()
        try {
            def result = work.call(em)
            tx.commit()
            return result
        } catch (Throwable t) {
            if (tx.active) tx.rollback()
            throw t
        } finally {
            em.close()
        }
    }

    private static boolean requiresNew(String methodName) {
        JpaMfaGraceRepository.declaredMethods
                .findAll { it.name == methodName }
                .any {
                    it.getAnnotation(org.springframework.transaction.annotation.Transactional) != null
                }
    }

    private static org.springframework.transaction.annotation.Propagation propagationOf(
            String methodName) {
        def annotation = JpaMfaGraceRepository.declaredMethods
                .findAll { it.name == methodName }
                .collect {
                    it.getAnnotation(org.springframework.transaction.annotation.Transactional)
                }
                .find { it != null }
        return annotation?.propagation()
    }
}
