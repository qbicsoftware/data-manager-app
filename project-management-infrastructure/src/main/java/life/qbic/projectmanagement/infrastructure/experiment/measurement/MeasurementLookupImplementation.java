package life.qbic.projectmanagement.infrastructure.experiment.measurement;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.Collection;
import java.util.List;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.SortOrder;
import life.qbic.projectmanagement.application.measurement.MeasurementLookup;
import life.qbic.projectmanagement.application.measurement.MeasurementMetadata;
import life.qbic.projectmanagement.domain.model.measurement.NGSMeasurement;
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsMeasurement;
import life.qbic.projectmanagement.domain.model.sample.SampleId;
import life.qbic.projectmanagement.infrastructure.OffsetBasedRequest;
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

  private static final Logger log = logger(MeasurementLookupImplementation.class);
  private final NGSMeasurementJpaRepo ngsMeasurementJpaRepo;
  private final ProteomicsMeasurementJpaRepo pxpMeasurementJpaRepo;
  private final MeasurementDataRepo measurementDataRepo;

  public MeasurementLookupImplementation(NGSMeasurementJpaRepo ngsMeasurementJpaRepo,
      ProteomicsMeasurementJpaRepo pxpMeasurementJpaRepo,
      MeasurementDataRepo measurementDataRepo) {
    this.ngsMeasurementJpaRepo = ngsMeasurementJpaRepo;
    this.pxpMeasurementJpaRepo = pxpMeasurementJpaRepo;
    this.measurementDataRepo = measurementDataRepo;
  }

  @Override
  public List<ProteomicsMeasurement> queryProteomicsMeasurementsBySampleIds(String filter,
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
    Specification<ProteomicsMeasurement> filterSpecification = generateProteomicsFilterSpecification(
        sampleIds, filter);
    return pxpMeasurementJpaRepo.findAll(filterSpecification,
        new OffsetBasedRequest(offset, limit, Sort.by(orders))).getContent();
  }

  private Specification<ProteomicsMeasurement> generateProteomicsFilterSpecification(
      Collection<SampleId> sampleIds, String filter) {
    Specification<ProteomicsMeasurement> isBlankSpec = ProteomicsMeasurementSpec.isBlank(filter);
    Specification<ProteomicsMeasurement> isDistinctSpec = ProteomicsMeasurementSpec.isDistinct();
//    Specification<ProteomicsMeasurement> containsSampleId = ProteomicsMeasurementSpec.containsSampleId(
//        sampleIds);
    Specification<ProteomicsMeasurement> measurementCodeContains = ProteomicsMeasurementSpec.isMeasurementCode(
        filter);
    Specification<ProteomicsMeasurement> organisationLabelContains = ProteomicsMeasurementSpec.isOrganisationLabel(
        filter);
    Specification<ProteomicsMeasurement> ontologyNameContains = ProteomicsMeasurementSpec.isOntologyTermName(
        filter);
    Specification<ProteomicsMeasurement> ontologyDescriptionContains = ProteomicsMeasurementSpec.isOntologyTermDescription(
        filter);
    Specification<ProteomicsMeasurement> filterSpecification = Specification.anyOf(
        measurementCodeContains,
        organisationLabelContains, ontologyNameContains, ontologyDescriptionContains);
    return Specification.where(isBlankSpec)
//        .and(containsSampleId)
        .and(filterSpecification)
        .and(isDistinctSpec);
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
    Specification<NGSMeasurement> filterSpecification = generateNGSFilterSpecification(
        sampleIds, filter);
    return ngsMeasurementJpaRepo.findAll(filterSpecification,
        new OffsetBasedRequest(offset, limit, Sort.by(orders))).getContent();
  }

  private Specification<NGSMeasurement> generateNGSFilterSpecification(
      Collection<SampleId> sampleIds, String filter) {
    Specification<NGSMeasurement> isBlankSpec = NgsMeasurementSpec.isBlank(filter);
    Specification<NGSMeasurement> isDistinctSpec = NgsMeasurementSpec.isDistinct();
    Specification<NGSMeasurement> containsSampleId = NgsMeasurementSpec.containsSampleId(
        sampleIds);
    Specification<NGSMeasurement> measurementCodeContains = NgsMeasurementSpec.isMeasurementCode(
        filter);
    //ToDo Extend with required ngs property specs
    Specification<NGSMeasurement> filterSpecification = Specification.anyOf(
        measurementCodeContains);
    return Specification.where(isBlankSpec).and(containsSampleId).and(filterSpecification)
        .and(isDistinctSpec);
  }


  private static class ProteomicsMeasurementSpec {

    //We need to ensure that we only count and retrieve unique samplePreviews
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
        } else {
          return root.join("measuredSamples").in(sampleIds);
        }
      };
    }

    //If no filter was provided return all proteomicsMeasurement
    public static Specification<ProteomicsMeasurement> isBlank(String filter) {
      return (root, query, builder) -> {
        if (filter != null && filter.isBlank()) {
          return builder.conjunction();
        }
        return null;
      };
    }

    public static Specification<ProteomicsMeasurement> isOrganisationLabel(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("organisation").get("label"), "%" + filter + "%");
    }

    public static Specification<ProteomicsMeasurement> isOntologyTermName(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("instrument").get("className"), "%" + filter + "%");
    }

    public static Specification<ProteomicsMeasurement> isOntologyTermDescription(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("instrument").get("description"), "%" + filter + "%");
    }

    public static Specification<ProteomicsMeasurement> isMeasurementCode(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("measurementCode").get("measurementCode"), "%" + filter + "%");
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
        } else {
          return root.join("measuredSamples").in(sampleIds);
        }
      };
    }

    //If no filter was provided return all proteomicsMeasurement
    public static Specification<NGSMeasurement> isBlank(String filter) {
      return (root, query, builder) -> {
        if (filter != null && filter.isBlank()) {
          return builder.conjunction();
        }
        return null;
      };
    }

    public static Specification<NGSMeasurement> isMeasurementCode(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("measurementCode").get("measurementCode"), "%" + filter + "%");
    }
    //ToDo extend with required property filters
  }
}
