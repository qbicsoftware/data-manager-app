package life.qbic.projectmanagement.domain.project.sample.event;

import java.io.Serial;
import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.sample.BatchId;
import life.qbic.projectmanagement.domain.project.sample.Sample;

/**
 * <b>Batch Registered Event</b>
 * <p>
 * An event that indicates that a new batch of samples has been registered
 *
 * @since 1.0.0
 */
public class SampleBatchRegistered extends DomainEvent {

  @Serial
  private static final long serialVersionUID = 123578812365826973L;

  private final Project project;
  private final Instant occurredOn;
  private final Collection<Sample> samples;

  private SampleBatchRegistered(Instant occurredOn, Project project, Collection<Sample> samples) {
    this.occurredOn = Objects.requireNonNull(occurredOn);
    this.project = Objects.requireNonNull(project);
    this.samples = Objects.requireNonNull(samples);
  }

  public static SampleBatchRegistered create(Project project, Collection<Sample> samples) {
    return new SampleBatchRegistered(Instant.now(), project, samples);
  }

  @Override
  public Instant occurredOn() {
    return occurredOn;
  }

  public Project project() { return project; }

  public Collection<Sample> samples() { return samples; }

}
