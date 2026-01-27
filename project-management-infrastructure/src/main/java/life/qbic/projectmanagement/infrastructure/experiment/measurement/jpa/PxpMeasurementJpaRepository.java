package life.qbic.projectmanagement.infrastructure.experiment.measurement.jpa;

import static life.qbic.projectmanagement.infrastructure.experiment.measurement.jpa.SpecificationFunctions.containsString;
import static life.qbic.projectmanagement.infrastructure.experiment.measurement.jpa.SpecificationFunctions.extractFormattedLocalDate;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.criteria.Join;
import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementId;
import life.qbic.projectmanagement.infrastructure.PreventAnyUpdateEntityListener;
import life.qbic.projectmanagement.infrastructure.experiment.measurement.jpa.PxpMeasurementJpaRepository.MsDevice.MsDeviceReadConverter;
import life.qbic.projectmanagement.infrastructure.experiment.measurement.jpa.PxpMeasurementJpaRepository.PxpMeasurementInformation;
import org.hibernate.collection.spi.PersistentBag;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

@Repository
public interface PxpMeasurementJpaRepository extends
    PagingAndSortingRepository<PxpMeasurementInformation, MeasurementId>,
    JpaSpecificationExecutor<PxpMeasurementInformation> {

  final class PxpMeasurementFilter {

    private final String experimentId;
    private final String searchTerm;
    private final int timeZoneOffsetMillis;

    public PxpMeasurementFilter(@NonNull String experimentId,
        @NonNull String searchTerm,
        int timeZoneOffsetMillis) {
      this.searchTerm = Objects.requireNonNull(searchTerm);
      this.experimentId = Objects.requireNonNull(experimentId);
      this.timeZoneOffsetMillis = timeZoneOffsetMillis;
    }

    public Specification<PxpMeasurementInformation> asSpecification() {
      return matchesExperiment(experimentId)
          .and(containsSearchTerm(searchTerm, timeZoneOffsetMillis));
    }

    private static Specification<PxpMeasurementInformation> matchesExperiment(String experimentId) {
      return (root, query, criteriaBuilder) ->
      {
        if (Objects.isNull(query)) {
          return criteriaBuilder.disjunction();
        }
        query.distinct(true);
        return criteriaBuilder.equal(root.join("sampleInfos").get("experimentId"), experimentId);
      };
    }

    private static Specification<PxpMeasurementInformation> containsSearchTerm(String searchTerm,
        int clientOffsetMillis) {
      if (Objects.isNull(searchTerm) || searchTerm.isEmpty()) {
        return Specification.unrestricted();
      }
      return (root, query, criteriaBuilder) -> {
        if (Objects.isNull(query)) {
          return criteriaBuilder.disjunction();
        }
        query.distinct(true);
        //join for sample related matching
        Join<Object, String> sampleInfos = root.joinList("sampleInfos");
        return
            criteriaBuilder.or(
                containsString(criteriaBuilder, root.get("measurementCode"), searchTerm),
                containsString(criteriaBuilder, root.get("measurementName"), searchTerm),
                containsString(criteriaBuilder, root.get("technicalReplicateName"), searchTerm),
                containsString(criteriaBuilder, root.get("digestionEnzyme"), searchTerm),
                containsString(criteriaBuilder, root.get("digestionMethod"), searchTerm),
                containsString(criteriaBuilder, root.get("injectionVolume").as(String.class),
                    searchTerm),
                containsString(criteriaBuilder, root.get("lcmsMethod"), searchTerm),
                containsString(criteriaBuilder, root.get("lcColumn"), searchTerm),
                containsString(criteriaBuilder, root.get("enrichmentMethod"), searchTerm),
                containsString(criteriaBuilder, root.get("samplePool"), searchTerm),
                SpecificationFunctions.containsStringInJson(criteriaBuilder, root.get("msDevice"),
                    "$.label", searchTerm),
                containsString(criteriaBuilder, root.get("organisation").get("label"), searchTerm),
                containsString(criteriaBuilder, root.get("organisation").get("iri"), searchTerm),
                containsString(criteriaBuilder,
                    extractFormattedLocalDate(criteriaBuilder, root.get("registeredAt"),
                        clientOffsetMillis, SpecificationFunctions.CUSTOM_DATE_TIME_PATTERN),
                    searchTerm),
                containsString(criteriaBuilder, sampleInfos.get("sampleLabel"), searchTerm),
                containsString(criteriaBuilder, sampleInfos.get("comment"), searchTerm)
            );
      };
    }

    @Override
    public String toString() {
      return new StringJoiner(", ", PxpMeasurementFilter.class.getSimpleName() + "[", "]")
          .add("experimentId='" + experimentId + "'")
          .add("searchTerm='" + searchTerm + "'")
          .toString();
    }
  }

  record MsDevice(String label, String oboId, String iri) implements Serializable {

    @JsonComponent
    static class MsDeviceJsonDeserializer extends JsonDeserializer<MsDevice> {

      @Override
      public MsDevice deserialize(JsonParser jsonParser, DeserializationContext ctxt)
          throws IOException {
        JsonNode tree = jsonParser.readValueAsTree();
        String oboId = Optional.ofNullable(tree.get("name"))
            .map(JsonNode::asText) //e.g. EFO_0008633
            .map(it -> it.replace("_", ":")) //e.g. EFO:0008633
            .orElseThrow(() -> new JsonParseException("Could not parse msDevice oboId."));
        String label = Optional.ofNullable(tree.get("label"))
            .map(JsonNode::asText)
            .orElseThrow(() -> new JsonParseException("Could not parse msDevice label."));
        String iri = Optional.ofNullable(tree.get("classIri"))
            .map(JsonNode::asText)
            .orElseThrow(() -> new JsonParseException("Could not parse msDevice iri."));
        return new MsDevice(label, oboId, iri);
      }
    }

    @ReadingConverter
    static class MsDeviceReadConverter implements AttributeConverter<MsDevice, String> {

      private final ObjectMapper objectMapper;

      public MsDeviceReadConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
      }

      @Override
      public String convertToDatabaseColumn(MsDevice attribute) {
        return "";
      }

      @Override
      public MsDevice convertToEntityAttribute(String dbData) {
        try {
          return objectMapper.readValue(dbData, MsDevice.class);
        } catch (JsonProcessingException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  record Organisation(String label, String iri) {

  }

  @Entity
  @Table(name = "specific_measurement_metadata_pxp")
  @SecondaryTable(name = "sample", pkJoinColumns = @PrimaryKeyJoinColumn(
      name = "sample_id", referencedColumnName = "sample_id"
  ))
  final class PxpSampleInfo {

    @Id
    @Column(name = "sample_id")
    private String sampleId;
    @ManyToOne
    @JoinColumn(name = "measurement_id")
    private PxpMeasurementInformation measurement;

    @Column(table = "sample", name = "experiment_id")
    private String experimentId;
    @Column(table = "sample", name = "code")
    private String sampleCode;
    @Column(table = "sample", name = "label")
    private String sampleLabel;
    @Column(table = "specific_measurement_metadata_pxp", name = "fractionName")
    private String fractionName;
    @Column(table = "specific_measurement_metadata_pxp", name = "label")
    private String measurementLabel;
    @Column(name = "comment")
    private String comment;

    protected PxpSampleInfo() {

    }

    public String sampleId() {
      return sampleId;
    }

    public String sampleCode() {
      return sampleCode;
    }

    public String sampleLabel() {
      return sampleLabel;
    }

    public String fractionName() {
      return fractionName;
    }

    public String measurementLabel() {
      return measurementLabel;
    }

    public String comment() {
      return comment;
    }

    String experimentId() {
      return experimentId;
    }

    @Override
    public String toString() {
      return new StringJoiner(", ", PxpSampleInfo.class.getSimpleName() + "[", "]")
          .add("sampleId='" + sampleId + "'")
          .add("measurement=" + measurement.measurementId)
          .add("sampleCode='" + sampleCode + "'")
          .add("sampleLabel='" + sampleLabel + "'")
          .add("comment='" + comment + "'")
          .toString();
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof PxpSampleInfo that)) {
        return false;
      }

      return Objects.equals(sampleId, that.sampleId) && Objects.equals(measurement.measurementId,
          that.measurement.measurementId) && Objects.equals(sampleCode, that.sampleCode)
          && Objects.equals(sampleLabel, that.sampleLabel) && Objects.equals(
          comment, that.comment);
    }

    @Override
    public int hashCode() {
      int result = Objects.hashCode(sampleId);
      result = 31 * result + Objects.hashCode(measurement.measurementId);
      result = 31 * result + Objects.hashCode(sampleCode);
      result = 31 * result + Objects.hashCode(sampleLabel);
      result = 31 * result + Objects.hashCode(comment);
      return result;
    }
  }


  @Entity(name = "pxp_measurement_metadata")
  @Table(name = "proteomics_measurement")
  @EntityListeners(PreventAnyUpdateEntityListener.class)
  final class PxpMeasurementInformation {

    @Id
    @Column(name = "measurement_id")
    private String measurementId;

    @Column(name = "projectId", nullable = false)
    private String projectId;

    @Column(name = "measurementCode")
    private String measurementCode;

    @Column(name = "measurementName")
    private String measurementName;

    @Column(name = "facility")
    private String facility;

    @Embedded
    @AttributeOverride(name = "label", column = @Column(name = "label"))
    @AttributeOverride(name = "iri", column = @Column(name = "IRI"))
    private Organisation organisation;

    @Column(name = "instrument")
    @Convert(converter = MsDeviceReadConverter.class)
    private MsDevice msDevice;

    @Column(name = "samplePool")
    private String samplePool;

    @Column(name = "registration")
    private Instant registeredAt;

    @Column(name = "digestionEnzyme")
    private String digestionEnzyme;

    @Column(name = "digestionMethod")
    private String digestionMethod;

    @Column(name = "enrichmentMethod")
    private String enrichmentMethod;

    @Column(name = "injectionVolume")
    private double injectionVolume;

    @Column(name = "labelType")
    private String labelType;

    @Column(name = "label", insertable = false, updatable = false)
    private String label;

    @Column(name = "technicalReplicateName")
    private String technicalReplicateName;

    @Column(name = "lcmsMethod")
    private String lcmsMethod;

    @Column(name = "lcColumn")
    private String lcColumn;


    /**
     * Attention: During Hibernate session, this is a
     * {@link PersistentBag}. The equals method of
     * {@link PersistentBag#equals(Object)} does not respect the
     * {@link List#equals(Object)} contract.
     */
    @OneToMany(mappedBy = "measurement", fetch = FetchType.EAGER)
    private List<PxpSampleInfo> sampleInfos;

    protected PxpMeasurementInformation() {

    }

    public String measurementId() {
      return measurementId;
    }

    public String measurementCode() {
      return measurementCode;
    }

    public String measurementName() {
      return measurementName;
    }

    public String facility() {
      return facility;
    }

    public Organisation organisation() {
      return organisation;
    }

    public MsDevice msDevice() {
      return msDevice;
    }

    @Transient
    public String getMsDeviceLabel() {
      return msDevice.label();
    }

    public String samplePool() {
      return samplePool;
    }

    public Instant registeredAt() {
      return registeredAt;
    }

    public String digestionEnzyme() {
      return digestionEnzyme;
    }

    public String digestionMethod() {
      return digestionMethod;
    }

    public String enrichmentMethod() {
      return enrichmentMethod;
    }

    public double injectionVolume() {
      return injectionVolume;
    }

    public String labelType() {
      return labelType;
    }

    public String technicalReplicateName() {
      return technicalReplicateName;
    }

    public String lcmsMethod() {
      return lcmsMethod;
    }

    public String lcColumn() {
      return lcColumn;
    }

    public String label() {
      return label;
    }

    public String experimentId() {
      return sampleInfos().getFirst().experimentId();
    }

    public List<PxpSampleInfo> sampleInfos() {
      return Collections.unmodifiableList(sampleInfos);
    }

    @Override
    public String toString() {
      return new StringJoiner(", ", PxpMeasurementInformation.class.getSimpleName() + "[",
          "]")
          .add("measurementId='" + measurementId + "'")
          .add("projectId='" + projectId + "'")
          .add("measurementCode='" + measurementCode + "'")
          .add("measurementName='" + measurementName + "'")
          .add("facility='" + facility + "'")
          .add("organisation=" + organisation)
          .add("msDevice=" + msDevice)
          .add("samplePool='" + samplePool + "'")
          .add("registeredAt=" + registeredAt)
          .add("digestionEnzyme='" + digestionEnzyme + "'")
          .add("digestionMethod='" + digestionMethod + "'")
          .add("enrichmentMethod='" + enrichmentMethod + "'")
          .add("injectionVolume=" + injectionVolume)
          .add("labelType='" + labelType + "'")
          .add("label='" + label + "'")
          .add("technicalReplicateName='" + technicalReplicateName + "'")
          .add("lcmsMethod='" + lcmsMethod + "'")
          .add("lcColumn='" + lcColumn + "'")
          .add("sampleInfos=" + sampleInfos)
          .toString();
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof PxpMeasurementInformation that)) {
        return false;
      }

      return Double.compare(injectionVolume, that.injectionVolume) == 0
          && Objects.equals(measurementId, that.measurementId) && Objects.equals(
          projectId, that.projectId) && Objects.equals(measurementCode, that.measurementCode)
          && Objects.equals(measurementName, that.measurementName)
          && Objects.equals(facility, that.facility) && Objects.equals(organisation,
          that.organisation) && Objects.equals(msDevice, that.msDevice)
          && Objects.equals(samplePool, that.samplePool) && Objects.equals(
          registeredAt, that.registeredAt) && Objects.equals(digestionEnzyme,
          that.digestionEnzyme) && Objects.equals(digestionMethod, that.digestionMethod)
          && Objects.equals(enrichmentMethod, that.enrichmentMethod)
          && Objects.equals(labelType, that.labelType) && Objects.equals(label,
          that.label) && Objects.equals(technicalReplicateName, that.technicalReplicateName)
          && Objects.equals(lcmsMethod, that.lcmsMethod) && Objects.equals(lcColumn,
          that.lcColumn)
          //take care of breaking interface method in persistance bag
          && Objects.equals(sampleInfos.stream().toList(), that.sampleInfos.stream().toList());
    }

    @Override
    public int hashCode() {
      int result = Objects.hashCode(measurementId);
      result = 31 * result + Objects.hashCode(projectId);
      result = 31 * result + Objects.hashCode(measurementCode);
      result = 31 * result + Objects.hashCode(measurementName);
      result = 31 * result + Objects.hashCode(facility);
      result = 31 * result + Objects.hashCode(organisation);
      result = 31 * result + Objects.hashCode(msDevice);
      result = 31 * result + Objects.hashCode(samplePool);
      result = 31 * result + Objects.hashCode(registeredAt);
      result = 31 * result + Objects.hashCode(digestionEnzyme);
      result = 31 * result + Objects.hashCode(digestionMethod);
      result = 31 * result + Objects.hashCode(enrichmentMethod);
      result = 31 * result + Double.hashCode(injectionVolume);
      result = 31 * result + Objects.hashCode(labelType);
      result = 31 * result + Objects.hashCode(label);
      result = 31 * result + Objects.hashCode(technicalReplicateName);
      result = 31 * result + Objects.hashCode(lcmsMethod);
      result = 31 * result + Objects.hashCode(lcColumn);
      //take care of breaking interface method in persistance bag
      result = 31 * result + Objects.hashCode(sampleInfos.stream().toList());
      return result;
    }
  }


}
