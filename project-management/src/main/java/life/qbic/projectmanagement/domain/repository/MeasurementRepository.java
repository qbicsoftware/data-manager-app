package life.qbic.projectmanagement.domain.repository;

import java.util.Collection;
import life.qbic.application.commons.Result;
import life.qbic.projectmanagement.application.measurement.MeasurementService.NGSMeasurementWrapper;
import life.qbic.projectmanagement.application.measurement.MeasurementService.ProteomicsMeasurementWrapper;
import life.qbic.projectmanagement.domain.model.measurement.NGSMeasurement;
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsMeasurement;
import life.qbic.projectmanagement.domain.service.MeasurementDomainService.ResponseCode;

/**
 * <b><interface short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public interface MeasurementRepository {

  Result<NGSMeasurement, ResponseCode> save(NGSMeasurementWrapper measurement);

  Result<ProteomicsMeasurement, ResponseCode> save(ProteomicsMeasurementWrapper measurement);

  Result<Collection<NGSMeasurement>, ResponseCode> saveAll(Collection<NGSMeasurement> ngsMeasurements);

}
