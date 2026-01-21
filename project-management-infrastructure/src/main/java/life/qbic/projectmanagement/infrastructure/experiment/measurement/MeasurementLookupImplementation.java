package life.qbic.projectmanagement.infrastructure.experiment.measurement;

import jakarta.persistence.criteria.Join;
import java.util.Collection;
import java.util.List;
import life.qbic.application.commons.OffsetBasedRequest;
import life.qbic.application.commons.SortOrder;
import life.qbic.projectmanagement.application.measurement.MeasurementLookup;
import life.qbic.projectmanagement.application.measurement.MeasurementMetadata;
import life.qbic.projectmanagement.domain.model.measurement.NGSMeasurement;
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsMeasurement;
import life.qbic.projectmanagement.domain.model.sample.SampleId;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

/**
 * Basic implementation to query measurement information
 * <p></p>
 * Employs JPA based {@link Specification} to provide the ability to filter each
 * {@link MeasurementMetadata} with the provided string based searchTerm
 */
@Repository
public class MeasurementLookupImplementation implements MeasurementLookup {

  private final NGSMeasurementJpaRepo ngsMeasurementJpaRepo;
  private final ProteomicsMeasurementJpaRepo pxpMeasurementJpaRepo;

  public MeasurementLookupImplementation(NGSMeasurementJpaRepo ngsMeasurementJpaRepo,
      ProteomicsMeasurementJpaRepo pxpMeasurementJpaRepo) {
    this.ngsMeasurementJpaRepo = ngsMeasurementJpaRepo;
    this.pxpMeasurementJpaRepo = pxpMeasurementJpaRepo;
  }

  @Override
  public long countProteomicsMeasurementsBySampleIds(Collection<SampleId> sampleIds) {
    Specification<ProteomicsMeasurement> isDistinctSpec = ProteomicsMeasurementSpec.isDistinct();
    Specification<ProteomicsMeasurement> containsSampleId = ProteomicsMeasurementSpec.containsSampleId(
        sampleIds);
    Specification<ProteomicsMeasurement> distinct = containsSampleId
        .and(isDistinctSpec);
    return pxpMeasurementJpaRepo.count(distinct);
  }

  @Override
  public long countNgsMeasurementsBySampleIds(Collection<SampleId> sampleIds) {
    Specification<NGSMeasurement> isDistinctSpec = NgsMeasurementSpec.isDistinct();
    Specification<NGSMeasurement> containsSampleId = NgsMeasurementSpec.containsSampleId(
        sampleIds);
    Specification<NGSMeasurement> distinct = containsSampleId
        .and(isDistinctSpec);
    return ngsMeasurementJpaRepo.count(distinct);
  }

  @Override
  public List<ProteomicsMeasurement> findProteomicsMeasurementsBySampleIds(String filter,
      Collection<SampleId> sampleIds, int offset,
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
    //FIXME ignores the filter
    return pxpMeasurementJpaRepo.findAll(ProteomicsMeasurementSpec.containsSampleId(sampleIds),
        new OffsetBasedRequest(offset, limit, Sort.by(orders))).getContent();
  }

  @Override
  public List<NGSMeasurement> queryNGSMeasurementsBySampleIds(String filter,
      Collection<SampleId> sampleIds, int offset,
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
    //FIXME ignores the filter
    return ngsMeasurementJpaRepo.findAll(NgsMeasurementSpec.containsSampleId(sampleIds),
        new OffsetBasedRequest(offset, limit, Sort.by(orders))).getContent();
  }

  @Override
  public List<ProteomicsMeasurement> findProteomicsMeasurementsBySampleIds(
      Collection<SampleId> sampleIds) {
    return pxpMeasurementJpaRepo.findAll(ProteomicsMeasurementSpec.containsSampleId(sampleIds));
  }

  @Override
  public List<NGSMeasurement> findNGSMeasurementsBySampleIds(
      Collection<SampleId> sampleIds) {
    return ngsMeasurementJpaRepo.findAll(NgsMeasurementSpec.containsSampleId(sampleIds));
  }


  private static class ProteomicsMeasurementSpec {

    //We need to ensure that we only count and retrieve unique ProteomicsMeasuerements
    public static Specification<ProteomicsMeasurement> isDistinct() {
      return (root, query, builder) -> {
        query.distinct(true);
        return null;
      };
    }

    //We are only interested in measurements which contain at least one of the provided sampleIds
    public static Specification<ProteomicsMeasurement> containsSampleId(
        Collection<SampleId> sampleIds) {
      return (root, query, builder) -> {
        if (sampleIds.isEmpty()) {
          //If no sampleId is in the experiment then there can also be no measurement
          return builder.disjunction();
        }
        Join<?, ?> sampleSpecificMetadata = root.join("specificMetadata");
        return sampleSpecificMetadata.get("measuredSample").in(sampleIds);
      };
    }

  }

  private static class NgsMeasurementSpec {

    //We need to ensure that we only count and retrieve unique ngsMeasurements
    public static Specification<NGSMeasurement> isDistinct() {
      return (root, query, builder) -> {
        query.distinct(true);
        return null;
      };
    }

    //We are only interested in measurements which contain at least one of the provided sampleIds
    public static Specification<NGSMeasurement> containsSampleId(
        Collection<SampleId> sampleIds) {
      return (root, query, builder) -> {
        if (sampleIds.isEmpty()) {
          //If no sampleId is in the experiment then there can also be no measurement
          return builder.disjunction();
        }
        Join<?, ?> sampleSpecificMetadata = root.join("specificMetadata");
        return sampleSpecificMetadata.get("measuredSample").in(sampleIds);
      };
    }
  }
}
