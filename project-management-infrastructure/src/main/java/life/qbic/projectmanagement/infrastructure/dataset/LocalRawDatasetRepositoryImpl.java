package life.qbic.projectmanagement.infrastructure.dataset;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import life.qbic.application.commons.SortOrder;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.api.AsyncProjectService.RawDataset;
import life.qbic.projectmanagement.application.api.AsyncProjectService.RawDatasetInformationPxP;
import life.qbic.projectmanagement.application.dataset.LocalRawDatasetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Repository
public class LocalRawDatasetRepositoryImpl implements LocalRawDatasetRepository {

  private static final Logger log = logger(LocalRawDatasetRepositoryImpl.class);

  private final LocalRawDatasetJpaRepository jpaRepository;

  @Autowired
  public LocalRawDatasetRepositoryImpl(LocalRawDatasetJpaRepository jpaRepository) {
    this.jpaRepository = Objects.requireNonNull(jpaRepository);
  }

  @Override
  public void saveAll(List<RawDataset> rawDataset) {
    log.debug("Saving raw datasets to local repository");

    List<LocalRawDatasetEntry> entriesToSave = new ArrayList<>();

    for (RawDataset dataset : rawDataset) {
      jpaRepository.findById(dataset.measurementId()).ifPresentOrElse(existingEntry -> {
        existingEntry.setDeleted(false);
        existingEntry.setFileCount(dataset.numberOfFiles());
        existingEntry.setFileTypes(dataset.fileTypes());
        existingEntry.setRegistrationDate(Date.from(dataset.registrationDate()));
        existingEntry.setTotalFileSizeBytes(dataset.totalSizeBytes());
        existingEntry.setLastSyncAt(Date.from(Instant.now()));
        existingEntry.setUpdatedAt(Date.from(Instant.now()));
        entriesToSave.add(existingEntry);
      }, () -> {
        var newEntry = new LocalRawDatasetEntry();
        newEntry.setMeasurementId(dataset.measurementId());
        newEntry.setFileCount(dataset.numberOfFiles());
        newEntry.setFileTypes(dataset.fileTypes());
        newEntry.setRegistrationDate(Date.from(dataset.registrationDate()));
        newEntry.setTotalFileSizeBytes(dataset.totalSizeBytes());
        newEntry.setLastSyncAt(Date.from(Instant.now()));
        newEntry.setUpdatedAt(Date.from(Instant.now()));
        newEntry.setDeleted(false);
        entriesToSave.add(newEntry);
      });
    }

    jpaRepository.saveAllAndFlush(entriesToSave);
  }

  @Override
  public List<RawDatasetInformationPxP> findAllPxP(String experimentId, int offset, int limit,
      List<SortOrder> sortOrders, String filter) {
    return List.of();
  }


}
