package life.qbic.projectmanagement.infrastructure.sync;

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

  @Override
  public Optional<Watermark> fetch(String rawDataSyncJob) {
    // TODO implement
    return Optional.empty();
  }
}
