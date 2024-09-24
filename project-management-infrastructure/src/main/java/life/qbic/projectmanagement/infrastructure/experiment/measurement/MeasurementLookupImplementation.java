package life.qbic.projectmanagement.infrastructure.experiment.measurement;

import jakarta.persistence.criteria.Expression;
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
  private final MeasurementDataRepo measurementDataRepo;

  public MeasurementLookupImplementation(NGSMeasurementJpaRepo ngsMeasurementJpaRepo,
      ProteomicsMeasurementJpaRepo pxpMeasurementJpaRepo,
      MeasurementDataRepo measurementDataRepo) {
    this.ngsMeasurementJpaRepo = ngsMeasurementJpaRepo;
    this.pxpMeasurementJpaRepo = pxpMeasurementJpaRepo;
    this.measurementDataRepo = measurementDataRepo;
  }

  @Override
  public long countProteomicsMeasurementsBySampleIds(Collection<SampleId> sampleIds) {
    Specification<ProteomicsMeasurement> isDistinctSpec = ProteomicsMeasurementSpec.isDistinct();
    Specification<ProteomicsMeasurement> containsSampleId = ProteomicsMeasurementSpec.containsSampleId(
        sampleIds);
    Specification<ProteomicsMeasurement> distinct = Specification.where(containsSampleId)
        .and(isDistinctSpec);
    return pxpMeasurementJpaRepo.count(distinct);
  }

  @Override
  public long countNgsMeasurementsBySampleIds(Collection<SampleId> sampleIds) {
    Specification<NGSMeasurement> isDistinctSpec = NgsMeasurementSpec.isDistinct();
    Specification<NGSMeasurement> containsSampleId = NgsMeasurementSpec.containsSampleId(
        sampleIds);
    Specification<NGSMeasurement> distinct = Specification.where(containsSampleId)
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
    Specification<ProteomicsMeasurement> filterSpecification = generateProteomicsFilterSpecification(
        sampleIds, filter);
    return pxpMeasurementJpaRepo.findAll(filterSpecification,
        new OffsetBasedRequest(offset, limit, Sort.by(orders))).getContent();
  }

  private Specification<ProteomicsMeasurement> generateProteomicsFilterSpecification(
      Collection<SampleId> sampleIds, String filter) {
    Specification<ProteomicsMeasurement> isBlankSpec = ProteomicsMeasurementSpec.isBlank(filter);
    Specification<ProteomicsMeasurement> isDistinctSpec = ProteomicsMeasurementSpec.isDistinct();
    Specification<ProteomicsMeasurement> containsSampleId = ProteomicsMeasurementSpec.containsSampleId(
        sampleIds);
    Specification<ProteomicsMeasurement> measurementCodeContains = ProteomicsMeasurementSpec.isMeasurementCode(
        filter);
    Specification<ProteomicsMeasurement> measurementLabelContains = ProteomicsMeasurementSpec.isMeasurementLabel(
        filter);
    Specification<ProteomicsMeasurement> measurementLabelingTypeContains = ProteomicsMeasurementSpec.isMeasurementLabelingType(
        filter);
    Specification<ProteomicsMeasurement> samplePoolGroupContains = ProteomicsMeasurementSpec.isSamplePoolGroup(
        filter);
    Specification<ProteomicsMeasurement> organisationLabelContains = ProteomicsMeasurementSpec.isOrganisationLabel(
        filter);
    Specification<ProteomicsMeasurement> ontologyNameContains = ProteomicsMeasurementSpec.isOntologyTermName(
        filter);
    Specification<ProteomicsMeasurement> ontologyLabelContains = ProteomicsMeasurementSpec.isOntologyTermLabel(
        filter);
    Specification<ProteomicsMeasurement> facilityContains = ProteomicsMeasurementSpec.isFacility(
        filter);
    Specification<ProteomicsMeasurement> fractionContains = ProteomicsMeasurementSpec.isFraction(
        filter);
    Specification<ProteomicsMeasurement> digestionMethodContains = ProteomicsMeasurementSpec.isDigestionMethod(
        filter);
    Specification<ProteomicsMeasurement> digestionEnzymeContains = ProteomicsMeasurementSpec.isDigestionEnzyme(
        filter);
    Specification<ProteomicsMeasurement> enrichmentMethodContains = ProteomicsMeasurementSpec.isEnrichmentMethod(
        filter);
    Specification<ProteomicsMeasurement> injectionVolumeContains = ProteomicsMeasurementSpec.isInjectionVolume(
        filter);
    Specification<ProteomicsMeasurement> lcColumnContains = ProteomicsMeasurementSpec.isLcColumn(
        filter);
    Specification<ProteomicsMeasurement> lcmsMethodContains = ProteomicsMeasurementSpec.isLcmsMethod(
        filter);
    Specification<ProteomicsMeasurement> registrationDateContains = ProteomicsMeasurementSpec.isRegistrationDate(
        filter);
    Specification<ProteomicsMeasurement> commentContains = ProteomicsMeasurementSpec.isComment(
        filter);

    Specification<ProteomicsMeasurement> filterSpecification =
        Specification.anyOf(
            commentContains,
            digestionMethodContains,
            digestionEnzymeContains,
            enrichmentMethodContains,
            facilityContains,
            fractionContains,
            injectionVolumeContains,
            lcColumnContains,
            lcmsMethodContains,
            measurementCodeContains,
            measurementLabelContains,
            measurementLabelingTypeContains,
            ontologyNameContains,
            ontologyLabelContains,
            organisationLabelContains,
            registrationDateContains,
            samplePoolGroupContains
        );
    return Specification.where(isBlankSpec)
        .and(containsSampleId)
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

  private Specification<NGSMeasurement> generateNGSFilterSpecification(
      Collection<SampleId> sampleIds, String filter) {
    Specification<NGSMeasurement> isBlankSpec = NgsMeasurementSpec.isBlank(filter);
    Specification<NGSMeasurement> isDistinctSpec = NgsMeasurementSpec.isDistinct();
    Specification<NGSMeasurement> containsSampleId = NgsMeasurementSpec.containsSampleId(
        sampleIds);
    Specification<NGSMeasurement> measurementCodeContains = NgsMeasurementSpec.isMeasurementCode(
        filter);
    Specification<NGSMeasurement> sequencingReadTypeContains = NgsMeasurementSpec.isSequencingReadType(
        filter);
    Specification<NGSMeasurement> facilityContains = NgsMeasurementSpec.isFacility(filter);
    Specification<NGSMeasurement> libraryKitContains = NgsMeasurementSpec.isLibraryKit(filter);
    Specification<NGSMeasurement> flowCellContains = NgsMeasurementSpec.isFlowCell(filter);
    Specification<NGSMeasurement> sequencingRunProtocolContains = NgsMeasurementSpec.isSequencingRunProtocol(
        filter);
    Specification<NGSMeasurement> indexI7Contains = NgsMeasurementSpec.isIndexI7(filter);
    Specification<NGSMeasurement> indexI5Contains = NgsMeasurementSpec.isIndexI5(filter);
    Specification<NGSMeasurement> registrationDateContains = NgsMeasurementSpec.isRegistrationDate(
        filter);
    Specification<NGSMeasurement> commentContains = NgsMeasurementSpec.isComment(
        filter);
    Specification<NGSMeasurement> filterSpecification = Specification.anyOf(
        measurementCodeContains,
        sequencingReadTypeContains,
        facilityContains,
        libraryKitContains,
        flowCellContains,
        sequencingRunProtocolContains,
        indexI7Contains,
        indexI5Contains,
        registrationDateContains,
        commentContains);
    return Specification.where(isBlankSpec).and(containsSampleId).and(filterSpecification)
        .and(isDistinctSpec);
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
      return (root, query, builder) -> {
        Expression<String> function = builder.function("JSON_EXTRACT", String.class,
            root.get("msDevice"),
            builder.literal("$.name"));
        return builder.like(function,
            "%" + filter + "%");
      };
    }

    public static Specification<ProteomicsMeasurement> isOntologyTermLabel(String filter) {
      return (root, query, builder) ->
      {
        Expression<String> function = builder.function("JSON_EXTRACT", String.class,
            root.get("msDevice"), builder.literal("$.label"));
        return builder.like(function,
            "%" + filter + "%");
      };
    }

    public static Specification<ProteomicsMeasurement> isMeasurementCode(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("measurementCode").as(String.class), "%" + filter + "%");
    }

    public static Specification<ProteomicsMeasurement> isFacility(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("facility"), "%" + filter + "%");
    }

    public static Specification<ProteomicsMeasurement> isFraction(String filter) {
      return (root, query, builder) -> {
        Join<?, ?> sampleSpecificMetadata = root.join("specificMetadata");
        return builder.like(sampleSpecificMetadata.get("fractionName").as(String.class),
            "%" + filter + "%");
      };
    }

    public static Specification<ProteomicsMeasurement> isDigestionMethod(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("digestionMethod"), "%" + filter + "%");
    }

    public static Specification<ProteomicsMeasurement> isDigestionEnzyme(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("digestionEnzyme"), "%" + filter + "%");
    }

    public static Specification<ProteomicsMeasurement> isEnrichmentMethod(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("enrichmentMethod"), "%" + filter + "%");
    }

    public static Specification<ProteomicsMeasurement> isInjectionVolume(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("injectionVolume").as(String.class), "%" + filter + "%");
    }

    public static Specification<ProteomicsMeasurement> isLcColumn(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("lcColumn"), "%" + filter + "%");
    }

    public static Specification<ProteomicsMeasurement> isLcmsMethod(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("lcmsMethod"), "%" + filter + "%");
    }

    public static Specification<ProteomicsMeasurement> isComment(String filter) {
      return (root, query, builder) -> {
        Join<?, ?> sampleSpecificMetadata = root.join("specificMetadata");
        return builder.like(sampleSpecificMetadata.get("comment").as(String.class),
            "%" + filter + "%");
      };
    }

    public static Specification<ProteomicsMeasurement> isMeasurementLabel(String filter) {
      return (root, query, builder) -> {
        Join<?, ?> sampleSpecificMetadata = root.join("specificMetadata");
        return builder.like(sampleSpecificMetadata.get("label").as(String.class),
            "%" + filter + "%");
      };
    }

    public static Specification<ProteomicsMeasurement> isMeasurementLabelingType(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("labelType"), "%" + filter + "%");
    }

    public static Specification<ProteomicsMeasurement> isSamplePoolGroup(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("samplePool"), "%" + filter + "%");
    }

    public static Specification<ProteomicsMeasurement> isRegistrationDate(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("registration").as(String.class), "%" + filter + "%");
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
          builder.like(root.get("measurementCode").as(String.class), "%" + filter + "%");
    }

    public static Specification<NGSMeasurement> isFacility(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("facility"), "%" + filter + "%");
    }

    public static Specification<NGSMeasurement> isSequencingReadType(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("sequencingReadType"), "%" + filter + "%");
    }

    public static Specification<NGSMeasurement> isLibraryKit(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("libraryKit"), "%" + filter + "%");
    }

    public static Specification<NGSMeasurement> isFlowCell(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("flowCell"), "%" + filter + "%");
    }

    public static Specification<NGSMeasurement> isSequencingRunProtocol(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("sequencingRunProtocol"), "%" + filter + "%");
    }

    public static Specification<NGSMeasurement> isIndexI7(String filter) {
      return (root, query, builder) -> {
        Join<?, ?> sampleSpecificMetadata = root.join("specificMetadata");
        return builder.like(sampleSpecificMetadata.get("indexI7").as(String.class),
            "%" + filter + "%");
      };
    }

    public static Specification<NGSMeasurement> isIndexI5(String filter) {
      return (root, query, builder) -> {
        Join<?, ?> sampleSpecificMetadata = root.join("specificMetadata");
        return builder.like(sampleSpecificMetadata.get("indexI5").as(String.class),
            "%" + filter + "%");
      };
    }

    public static Specification<NGSMeasurement> isRegistrationDate(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("registration").as(String.class), "%" + filter + "%");
    }

    public static Specification<NGSMeasurement> isComment(String filter) {
      return (root, query, builder) -> {
        Join<?, ?> sampleSpecificMetadata = root.join("specificMetadata");
        return builder.like(sampleSpecificMetadata.get("comment").as(String.class),
            "%" + filter + "%");
      };
    }
  }
}
