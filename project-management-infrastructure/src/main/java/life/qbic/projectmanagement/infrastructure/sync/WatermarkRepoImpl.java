package life.qbic.projectmanagement.infrastructure.sync;

import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import life.qbic.projectmanagement.application.sync.WatermarkRepo;
import org.springframework.stereotype.Repository;

/**
 * <b>Watermark Repo Impl</b>
 *
 * <p>Implementation of the {@link WatermarkRepo}.</p>
 *
 * @since 1.110.0
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
        entry.updatedSince, entry.lastSuccessAt));
  }

  private Optional<WatermarkEntry> findById(String id) {
    return jpaRepository.findById(id);
  }

  @Override
  public void save(Watermark latestWatermark) {
    WatermarkEntry entryForUpdate = findById(latestWatermark.jobName()).orElse(
        WatermarkEntry.create(latestWatermark.jobName(), latestWatermark.syncOffset(),
            latestWatermark.updatedSince(), latestWatermark.lastSuccessAt()));
    entryForUpdate.syncOffset = latestWatermark.syncOffset();
    entryForUpdate.updatedSince = latestWatermark.updatedSince();
    entryForUpdate.lastSuccessAt = latestWatermark.lastSuccessAt();

    jpaRepository.save(entryForUpdate);
  }
}
