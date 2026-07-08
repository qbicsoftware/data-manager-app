package life.qbic.projectmanagement.infrastructure.experiment.measurement;

import java.util.Optional;
import life.qbic.projectmanagement.domain.model.measurement.ImmunopeptidomicsMeasurement;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementCode;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Simple Immunopeptidomics measurement JPA repository to query and filter concise
 * {@link ImmunopeptidomicsMeasurement} information
 */
public interface ImmunopeptidomicsMeasurementJpaRepo
    extends JpaRepository<ImmunopeptidomicsMeasurement, MeasurementId>,
    JpaSpecificationExecutor<ImmunopeptidomicsMeasurement> {

  Optional<ImmunopeptidomicsMeasurement> findImmunopeptidomicsMeasurementByMeasurementCode(
      MeasurementCode measurementCode);

  Optional<ImmunopeptidomicsMeasurement> findImmunopeptidomicsMeasurementByMeasurementId(
      MeasurementId measurementId);
}
