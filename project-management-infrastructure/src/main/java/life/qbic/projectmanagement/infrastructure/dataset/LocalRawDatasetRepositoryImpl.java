package life.qbic.projectmanagement.infrastructure.dataset;

import static life.qbic.logging.service.LoggerFactory.logger;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import life.qbic.application.commons.OffsetBasedRequest;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.api.AsyncProjectService;
import life.qbic.projectmanagement.application.api.AsyncProjectService.BasicSampleInformation;
import life.qbic.projectmanagement.application.api.AsyncProjectService.RawDataSortingKey;
import life.qbic.projectmanagement.application.api.AsyncProjectService.RawDataset;
import life.qbic.projectmanagement.application.api.AsyncProjectService.RawDatasetFilter;
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
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

/**
 * <b>Local Raw Dataset Repository Impl</b>
 *
 * <p>Implementation of the {@link LocalRawDatasetRepository} interface.</p>
 *
 * @since 1.11.0
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
        existingEntry.setRegistrationDate(dataset.registrationDate());
        existingEntry.setTotalFileSizeBytes(dataset.totalSizeBytes());
        existingEntry.setLastSyncAt(Instant.now());
        existingEntry.setUpdatedAt(Instant.now());
        entriesToSave.add(existingEntry);
      }, () -> {
        var newEntry = new LocalRawDatasetEntry();
        newEntry.setMeasurementId(dataset.measurementId());
        newEntry.setFileCount(dataset.numberOfFiles());
        newEntry.setFileTypes(dataset.fileTypes());
        newEntry.setRegistrationDate(dataset.registrationDate());
        newEntry.setTotalFileSizeBytes(dataset.totalSizeBytes());
        newEntry.setLastSyncAt(Instant.now());
        newEntry.setUpdatedAt(Instant.now());
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
    return pxpInfoRepository.findAllByExperimentId(experimentId,
            PageRequest.of(offset, limit, sortOrder))
        .stream()
        .map(LocalRawDatasetRepositoryImpl::convert)
        .toList();
  }

  @Override
  public List<RawDatasetInformationNgs> findAllNgs(String experimentId, int offset, int limit,
      SortRawData sorting, String filter) {
    var sortOrder = mapSorting(Objects.requireNonNull(sorting));
    return ngsInfoRepository.findAllByExperimentId(experimentId,
            PageRequest.of(offset, limit, sortOrder))
        .stream()
        .map(LocalRawDatasetRepositoryImpl::convert)
        .toList();
  }

  private static final Map<RawDataSortingKey, String> rawDataSortingMap = new EnumMap<>(
      RawDataSortingKey.class);

  static {
    rawDataSortingMap.put(RawDataSortingKey.SAMPLE_NAME, "sampleName");
    rawDataSortingMap.put(RawDataSortingKey.MEASUREMENT_ID, "measurementCode");
    rawDataSortingMap.put(RawDataSortingKey.UPLOAD_DATE, "registrationDate");
  }

  @Override
  public List<RawDatasetInformationNgs> findAllNgs(String experimentId, int offset, int limit,
      RawDatasetFilter filter) throws LookupException {
    List<Order> orders;

    try {
      orders = filter.sortOrders().stream().map(LocalRawDatasetRepositoryImpl::fromAPItoJpaRawData)
          .toList();
    } catch (IllegalArgumentException e) {
      throw new LookupException("Lookup for raw datasets failed", e);
    }

    Specification<LocalRawDatasetNgsEntry> sampleNameSpec = (root, query, builder) -> {
      if (filter.filterTerm().isBlank()) {
        return builder.conjunction();
      }
      Expression<String> function = builder.function("JSON_SEARCH", String.class, root.get("measuredSamples"),
          builder.literal("one"),
          builder.literal("%" + filter.filterTerm() + "%"),
          builder.nullLiteral(String.class),
          builder.literal("$[*].label")
      );

      return builder.isNotNull(function);
    };

    Specification<LocalRawDatasetNgsEntry> experimentIdSpec = (root, query, builder) -> {
      if  (experimentId == null) {
        return builder.conjunction();
      }
      return builder.equal(root.get("experimentId").as(String.class), experimentId);
    };

    Specification<LocalRawDatasetNgsEntry> fullSpec = Specification.unrestricted();
    fullSpec = fullSpec.and(sampleNameSpec).and(experimentIdSpec);


    return ngsInfoRepository.findAll(fullSpec,
            new OffsetBasedRequest(offset, limit, Sort.by(orders))).getContent()
        .stream().map(LocalRawDatasetRepositoryImpl::convert).toList();
  }

  private static Order fromAPItoJpaRawData(
      AsyncProjectService.SortOrder<RawDataSortingKey> sortOrder) {
    var mappedJpaProperty = propertyFromAPItoJpaRawDataset(sortOrder.key()).orElseThrow(
        () -> new IllegalArgumentException(
            "Cannot map key to JPA raw dataset property: " + sortOrder.key()
        ));
    if (sortOrder.direction() == SortDirection.ASC) {
      return Order.asc(mappedJpaProperty);
    } else {
      return Order.desc(mappedJpaProperty);
    }
  }

  private static Optional<String> propertyFromAPItoJpaRawDataset(RawDataSortingKey key) {
    return Optional.ofNullable(rawDataSortingMap.getOrDefault(key, null));
  }

  @Override
  public List<RawDatasetInformationPxP> findAllPxP(String experimentId, int offset, int limit,
      RawDatasetFilter filter) throws LookupException {
    List<Order> orders;

    try {
      orders = filter.sortOrders().stream().map(LocalRawDatasetRepositoryImpl::fromAPItoJpaRawData)
          .toList();
    } catch (IllegalArgumentException e) {
      throw new LookupException("Lookup for raw datasets failed", e);
    }

    Specification<LocalRawDatasetPxpEntry> sampleNameSpec = (root, query, builder) -> {
      if (filter.filterTerm().isBlank()) {
        return builder.conjunction();
      }
      Expression<String> function = builder.function("JSON_SEARCH", String.class, root.get("measuredSamples"),
          builder.literal("one"),
          builder.literal("%" + filter.filterTerm() + "%"),
          builder.nullLiteral(String.class),
          builder.literal("$[*].label")
          );

      return builder.isNotNull(function);
    };

    Specification<LocalRawDatasetPxpEntry> experimentIdSpec = (root, query, builder) -> {
      if  (experimentId == null) {
        return builder.conjunction();
      }
      return builder.equal(root.get("experimentId").as(String.class), experimentId);
    };

    Specification<LocalRawDatasetPxpEntry> fullSpec = Specification.unrestricted();
    fullSpec = fullSpec.and(sampleNameSpec).and(experimentIdSpec);

    return pxpInfoRepository.findAll(fullSpec,
            new OffsetBasedRequest(offset, limit, Sort.by(orders))).getContent()
        .stream().map(LocalRawDatasetRepositoryImpl::convert).toList();
  }

  @Override
  public Integer countNGS(String experimentId, RawDatasetFilter filter) throws LookupException {

    Specification<LocalRawDatasetNgsEntry> sampleNameSpec = (root, query, builder) -> {
      if (filter == null) {
        return builder.conjunction();
      }
      Expression<String> function = builder.function("JSON_SEARCH", String.class, root.get("measuredSamples"),
          builder.literal("one"),
          builder.literal("%" + filter.filterTerm() + "%"),
          builder.nullLiteral(String.class),
          builder.literal("$[*].label")
      );

      return builder.isNotNull(function);
    };

    Specification<LocalRawDatasetNgsEntry> experimentIdSpec = (root, query, builder) -> {
      if  (experimentId == null) {
        return builder.conjunction();
      }
      return builder.equal(root.get("experimentId").as(String.class), experimentId);
    };

    Specification<LocalRawDatasetNgsEntry> fullSpec = Specification.unrestricted();
    fullSpec = fullSpec.and(sampleNameSpec).and(experimentIdSpec);


    return Math.toIntExact(ngsInfoRepository.count(fullSpec));
  }

  @Override
  public Integer countPxP(String experimentId, RawDatasetFilter filter) throws LookupException {

    Specification<LocalRawDatasetPxpEntry> sampleNameSpec = (root, query, builder) -> {
      if (filter == null) {
        return builder.conjunction();
      }
      Expression<String> function = builder.function("JSON_SEARCH", String.class, root.get("measuredSamples"),
          builder.literal("one"),
          builder.literal("%" + filter.filterTerm() + "%"),
          builder.nullLiteral(String.class),
          builder.literal("$[*].label")
      );

      return builder.isNotNull(function);
    };

    Specification<LocalRawDatasetPxpEntry> experimentIdSpec = (root, query, builder) -> {
      if  (experimentId == null) {
        return builder.conjunction();
      }
      return builder.equal(root.get("experimentId").as(String.class), experimentId);
    };

    Specification<LocalRawDatasetPxpEntry> fullSpec = Specification.unrestricted();
    fullSpec = fullSpec.and(sampleNameSpec).and(experimentIdSpec);


    return Math.toIntExact(pxpInfoRepository.count(fullSpec));
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
            entry.getRegistrationDate()),
        entry.getMeasuredSamples().stream().map(LocalRawDatasetRepositoryImpl::convert).toList());
  }

  private static RawDatasetInformationPxP convert(LocalRawDatasetPxpEntry entry) {
    return new RawDatasetInformationPxP(
        new RawDataset(entry.getMeasurementCode(),
            entry.getTotalFileSizeBytes(),
            entry.getNumberOfFiles(),
            entry.getFileTypes(),
            entry.getRegistrationDate().toInstant()),
        entry.getMeasuredSamples().stream().map(LocalRawDatasetRepositoryImpl::convert).toList());
  }

  private static BasicSampleInformation convert(MeasuredSample sample) {
    return new BasicSampleInformation(sample.sampleId(), sample.sampleName());
  }


}
