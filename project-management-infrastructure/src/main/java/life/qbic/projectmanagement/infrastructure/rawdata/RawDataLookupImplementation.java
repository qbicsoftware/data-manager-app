package life.qbic.projectmanagement.infrastructure.rawdata;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.Collection;
import java.util.List;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.SortOrder;
import life.qbic.projectmanagement.application.rawdata.RawDataLookup;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementId;
import life.qbic.projectmanagement.domain.model.rawdata.RawData;
import life.qbic.projectmanagement.infrastructure.OffsetBasedRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

/**
 * Basic implementation to query raw data information
 * <p></p>
 * Employs JPA based {@link Specification} to provide the ability to filter each
 * {@link RawData} with the provided string based searchTerm
 */
@Repository
public class RawDataLookupImplementation implements RawDataLookup {

  private static final Logger log = logger(RawDataLookupImplementation.class);
  private final RawDataJpaRepo rawDataJpaRepo;

  public RawDataLookupImplementation(RawDataJpaRepo rawDataJpaRepo) {
    this.rawDataJpaRepo = rawDataJpaRepo;
  }

  @Override
  public long countRawDataByMeasurementIds(Collection<MeasurementId> measurementIds) {
    return rawDataJpaRepo.count(RawDataSpec.containsMeasurementIds(measurementIds));
  }

  @Override
  public List<RawData> queryRawDataByMeasurementIds(String filter,
      Collection<MeasurementId> measurementIds, int offset,
      int limit, List<SortOrder> sortOrders) {
    List<Order> orders = sortOrders.stream().map(it -> {
      Order order;
      if (it.isDescending()) {
        order = Order.desc(it.propertyName());
      } else {
        order = Order.asc(it.propertyName());
      }
      return order;
    }).toList();
    Specification<RawData> filterSpecification = generateRawDataFilterSpecification(
        measurementIds, filter);
    return rawDataJpaRepo.findAll(filterSpecification,
        new OffsetBasedRequest(offset, limit, Sort.by(orders))).getContent();
  }

  private Specification<RawData> generateRawDataFilterSpecification(
      Collection<MeasurementId> measurementIds, String filter) {
    Specification<RawData> isBlankSpec = RawDataSpec.isBlank(filter);
    Specification<RawData> isDistinctSpec = RawDataSpec.isDistinct();
    Specification<RawData> containsMeasurementIds = RawDataSpec.containsMeasurementIds(
        measurementIds);
    Specification<RawData> registrationDateContains = RawDataSpec.isRegistrationDate(
        filter);
    Specification<RawData> filterSpecification =
        Specification.anyOf(registrationDateContains);
    return Specification.where(isBlankSpec)
        .and(containsMeasurementIds)
        .and(filterSpecification)
        .and(isDistinctSpec);
  }

  private static class RawDataSpec {

    //We need to ensure that we only count and retrieve unique Raw data entries
    public static Specification<RawData> isDistinct() {
      return (root, query, builder) -> {
        query.distinct(true);
        return null;
      };
    }

    //We are only interested in raw data which contain at least one of the provided measurementIds
    public static Specification<RawData> containsMeasurementIds(
        Collection<MeasurementId> measurementIds) {
      return (root, query, builder) -> {
        if (measurementIds.isEmpty()) {
          //If no sampleId is in the experiment then there can also be no measurement
          return builder.disjunction();
        }
        return root.join("measurements").in(measurementIds);
      };
    }

    //If no filter was provided return all proteomicsMeasurement
    public static Specification<RawData> isBlank(String filter) {
      return (root, query, builder) -> {
        if (filter != null && filter.isBlank()) {
          return builder.conjunction();
        }
        return null;
      };
    }

    public static Specification<RawData> isRegistrationDate(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("registration").as(String.class), "%" + filter + "%");
    }
  }
}
