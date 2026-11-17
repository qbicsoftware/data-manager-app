package life.qbic.identity.application.user.policy

import org.jobrunr.jobs.annotations.Job
import spock.lang.Specification

/**
 * <b>The reset-notification job must not retry.</b>
 *
 * <p>{@code notifyUser} issues a fresh reset token on every invocation, and issuing supersedes the
 * previous token for that account (AUTH-R-05). That is correct for a second <em>user request</em> but
 * wrong for a retry of the <em>same</em> request, because each attempt invalidates the link the
 * previous attempt may already have delivered.</p>
 *
 * <h3>Why retrying is unsafe here</h3>
 *
 * <p>When an SMTP server accepts a message and the connection then fails before the client sees the
 * acknowledgement — the ordinary {@code timeout after DATA} case — the earlier mail is already
 * queued for delivery. A retry issues a new token, superseding the one in the mail that is on its
 * way. The recipient gets two mails and only the newer link works; the first one says the link is
 * no longer valid.</p>
 *
 * <p>Fixing that properly needs the raw token carried across attempts, which means persisting it in
 * plaintext — in the JobRunr job arguments, which are stored as JSON in the database. That would
 * break the property {@code VerificationToken} exists to provide: only a hash is stored and the raw
 * value exists once, inside the email. A durable outbox with a short retention and an ADR is the
 * real answer, and is out of scope for a hotfix.</p>
 *
 * <h3>Why the default is especially bad here</h3>
 *
 * <p>JobRunr's defaults are {@code 10} retries with a {@code 3^n} second backoff: attempts at roughly
 * +3 s, +12 s, +39 s, +2 min, +6 min, +18 min, +55 min … over about <b>24 hours</b>. The reset token
 * lives 5 minutes, so attempts after the fifth cannot produce a working link even in principle.</p>
 *
 * <p>This spec pins the decision rather than the mechanism: it asserts the annotation an operator and
 * a reviewer can read, and it fails if someone later re-enables retries without reconsidering the
 * supersede interaction above.</p>
 */
class PasswordResetJobDoesNotRetrySpec extends Specification {

    def "the password reset notification job declares zero retries"() {
        given: 'the job method that issues a token per invocation'
        def method = WhenPasswordResetNotifyUser.getDeclaredMethod(
                'notifyUserWithInstructions', String)

        when:
        def annotation = method.getAnnotation(Job)

        then: 'it is still a JobRunr job, so it runs in the background as documented'
        annotation != null

        and: 'but it does not retry, because a retry supersedes the link it just delivered'
        annotation.retries() == 0
    }

    def "the confirmation job keeps the default retry budget, for contrast"() {
        // Guards the reasoning above from over-generalisation: only the reset flow issues a token
        // that a retry would invalidate. Reusing the same rule everywhere would silently remove a
        // useful retry from the confirmation mail.
        given:
        def method = life.qbic.identity.application.user.policy.directive
                .WhenUserRegisteredSendConfirmationEmail
                .getDeclaredMethod('notifyUserWithConfirmationInstructions', String)

        when:
        def annotation = method.getAnnotation(Job)

        then: 'its retries are left at the library default (-1 means "not provided")'
        annotation.retries() == Job.NBR_OF_RETRIES_NOT_PROVIDED
    }
}
