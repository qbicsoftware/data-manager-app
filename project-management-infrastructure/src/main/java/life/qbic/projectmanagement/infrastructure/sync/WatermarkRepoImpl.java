package life.qbic.projectmanagement.infrastructure.sync;

import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import life.qbic.projectmanagement.application.sync.WatermarkRepo;
import org.springframework.stereotype.Repository;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Repository
public class WatermarkRepoImpl implements WatermarkRepo {

  private final WatermarkJpaRepository jpaRepository;

  public WatermarkRepoImpl(WatermarkJpaRepository watermarkJpaRepository) {
    this.jpaRepository = Objects.requireNonNull(watermarkJpaRepository);
  }

  @Override
  public Optional<Watermark> fetch(String jobName) {
    return findById(jobName).map(entry -> new Watermark(entry.jobName, entry.syncOffset,
        entry.updatedSince.toInstant(), entry.lastSuccessAt.toInstant()));
  }

  private Optional<WatermarkEntry> findById(String id) {
    return jpaRepository.findById(id);
  }

  @Override
  public void upsert(Watermark currentWatermark) {
    WatermarkEntry entryForUpdate = findById(currentWatermark.jobName()).orElse(
        WatermarkEntry.create(currentWatermark.jobName(), currentWatermark.syncOffset(),
            Date.from(currentWatermark.updatedSince()), Date.from(currentWatermark.lastSuccessAt())));
    entryForUpdate.syncOffset = currentWatermark.syncOffset();
    entryForUpdate.updatedSince = Date.from(currentWatermark.updatedSince());
    entryForUpdate.lastSuccessAt = Date.from(currentWatermark.lastSuccessAt());

    jpaRepository.save(entryForUpdate);
  }
}
