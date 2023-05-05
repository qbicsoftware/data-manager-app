package life.qbic.projectmanagement.application.policy;

import life.qbic.domain.concepts.DomainEvent;
import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.domain.concepts.DomainEventSubscriber;
import life.qbic.projectmanagement.domain.project.repository.BatchRepository;
import life.qbic.projectmanagement.domain.project.sample.BatchId;
import life.qbic.projectmanagement.domain.project.sample.event.SampleRegistered;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Component
public class WhenSampleRegisteredUpdateBatch implements DomainEventSubscriber<SampleRegistered> {

  private final BatchRepository batchRepository;

  @Autowired
  public WhenSampleRegisteredUpdateBatch(BatchRepository batchRepository) {
    this.batchRepository = batchRepository;
    DomainEventDispatcher.instance().subscribe(this);
  }

  @Override
  public Class<? extends DomainEvent> subscribedToEventType() {
    return SampleRegistered.class;
  }

  @Override
  public void handleEvent(SampleRegistered event) {
    BatchId affectedBatch = event.assignedBatch();
    batchRepository.find(affectedBatch).ifPresent(batch -> {
      batch.addSample(event.registeredSample());
      batchRepository.update(batch);
    });
  }
}
