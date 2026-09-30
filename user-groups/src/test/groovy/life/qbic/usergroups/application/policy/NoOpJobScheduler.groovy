package life.qbic.usergroups.application.policy

import org.jobrunr.jobs.JobId
import org.jobrunr.jobs.lambdas.JobLambda
import org.jobrunr.scheduling.JobScheduler
import org.jobrunr.storage.InMemoryStorageProvider

/**
 * A {@link JobScheduler} whose {@code enqueue(...)} is a harmless no-op.
 *
 * <p>Used in tests that construct the notification directives/policy but must not actually run
 * the JobRunr pipeline (which requires the byte-buddy-based job-details generator not present in
 * these unit tests). Overriding {@code enqueue} (non-final) keeps the fake dependency-free and
 * makes any leaked subscription to the global {@code DomainEventDispatcher} benign: a dispatched
 * member event only no-ops instead of crashing.</p>
 */
class NoOpJobScheduler extends JobScheduler {

  NoOpJobScheduler() {
    super(new InMemoryStorageProvider())
  }

  @Override
  JobId enqueue(JobLambda jobLambda) {
    return new JobId(java.util.UUID.randomUUID())
  }
}