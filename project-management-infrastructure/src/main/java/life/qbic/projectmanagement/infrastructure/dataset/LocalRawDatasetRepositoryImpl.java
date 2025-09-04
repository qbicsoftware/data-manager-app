package life.qbic.projectmanagement.infrastructure.dataset;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.api.AsyncProjectService.RawDataset;
import life.qbic.projectmanagement.application.api.AsyncProjectService.RawDatasetInformationNgs;
import life.qbic.projectmanagement.application.api.AsyncProjectService.RawDatasetInformationPxP;
import life.qbic.projectmanagement.application.api.AsyncProjectService.SortDirection;
import life.qbic.projectmanagement.application.api.AsyncProjectService.SortFieldRawData;
import life.qbic.projectmanagement.application.api.AsyncProjectService.SortRawData;
import life.qbic.projectmanagement.application.dataset.LocalRawDatasetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
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

  private static final Map<SortFieldRawData, String> SORT_FIELD_MAPPINGS = new HashMap<>();

  static {
    SORT_FIELD_MAPPINGS.put(SortFieldRawData.REGISTRATION_DATE, "registrationDate");
  }

  private final LocalRawDatasetJpaRepository jpaRepository;
  private final LocalRawDatasetInformationPxPJpaRepository pxpInfoRepository;
  private final LocalRawDatasetInformationNgsJpaRepository ngsInfoRepository;

  @Autowired
  public LocalRawDatasetRepositoryImpl(
      LocalRawDatasetJpaRepository jpaRepository,
      LocalRawDatasetInformationPxPJpaRepository infoPxpJpaRepository,
      LocalRawDatasetInformationNgsJpaRepository infoNgsJpaRepository) {
    this.jpaRepository = Objects.requireNonNull(jpaRepository);
    this.ngsInfoRepository = Objects.requireNonNull(infoNgsJpaRepository);
    this.pxpInfoRepository = Objects.requireNonNull(infoPxpJpaRepository);
  }

  @Override
  public void saveAll(List<RawDataset> rawDatasets) {
    log.debug("Saving raw datasets to local repository");

    List<LocalRawDatasetEntry> entriesToSave = new ArrayList<>();

    for (RawDataset dataset : rawDatasets) {
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
      SortRawData sorting, String filter) {
    var sortOrder = mapSorting(Objects.requireNonNull(sorting));
    return pxpInfoRepository.findAllByExperimentId(experimentId, PageRequest.of(offset, limit, sortOrder))
        .stream()
        .map(LocalRawDatasetRepositoryImpl::convert)
        .toList();
  }

  @Override
  public List<RawDatasetInformationNgs> findAllNgs(String experimentId, int offset, int limit,
      SortRawData sorting, String filter) {
    var sortOrder = mapSorting(Objects.requireNonNull(sorting));
    return ngsInfoRepository.findAllByExperimentId(experimentId, PageRequest.of(offset, limit, sortOrder))
        .stream()
        .map(LocalRawDatasetRepositoryImpl::convert)
        .toList();
  }

  private static Sort mapSorting(SortRawData sorting) {
    return Sort.by(mapSortOrder(sorting.sortDirection()), mapSortField(sorting.sortField()));
  }

  private static String mapSortField(SortFieldRawData sortField) {
    return SORT_FIELD_MAPPINGS.getOrDefault(sortField,
        SORT_FIELD_MAPPINGS.get(SortFieldRawData.REGISTRATION_DATE));
  }

  private static Sort.Direction mapSortOrder(SortDirection sortDirection) {
    if (sortDirection == SortDirection.ASC) {
      return Direction.ASC;
    }
    return Direction.DESC;
  }

  private static RawDatasetInformationNgs convert(LocalRawDatasetNgsEntry entry) {
    return new RawDatasetInformationNgs(
        new RawDataset(entry.getMeasurementCode(),
            entry.getTotalFileSizeBytes(),
            entry.getNumberOfFiles(),
            entry.getFileTypes(),
            entry.getRegistrationDate().toInstant()),
        List.of());
  }

  private static RawDatasetInformationPxP convert(LocalRawDatasetPxpEntry entry) {
    return new RawDatasetInformationPxP(
        new RawDataset(entry.getMeasurementCode(),
            entry.getTotalFileSizeBytes(),
            entry.getNumberOfFiles(),
            entry.getFileTypes(),
            entry.getRegistrationDate().toInstant()),
        List.of());
  }


}
