package life.qbic.projectmanagement.application.measurement.validation;

import java.util.Collection;
import java.util.Optional;
import life.qbic.projectmanagement.application.measurement.NGSMeasurementMetadata;
import life.qbic.projectmanagement.application.measurement.ProteomicsMeasurementMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <b>Validation Service</b>
 *
 * <p>Validation Service for measurement metadata validation prior to registration</p>
 * <p>
 * This service can be used to validate provided measurement metadata before register them. Of
 * course registration will also contain validation, consider this service as a try-run registration
 * service to let the user fail early with detailed provided {@link MeasurementValidationResult}.
 *
 * @since 1.0.0
 */
@Service
public class MeasurementValidationService {

  private final MeasurementNGSValidator measurementNgsValidator;

  private final MeasurementProteomicsValidator pxpValidator;

  @Autowired
  public MeasurementValidationService(MeasurementNGSValidator measurementNgsValidator,
      MeasurementProteomicsValidator pxpValidator) {
    this.measurementNgsValidator = measurementNgsValidator;
    this.pxpValidator = pxpValidator;
  }

  public MeasurementValidationResult validateNGS(NGSMeasurementMetadata ngsMeasurementMetadata) {
    return measurementNgsValidator.validate(ngsMeasurementMetadata);
  }

  public MeasurementValidationResult validateProteomics(
      ProteomicsMeasurementMetadata pxMeasurementMetadata) {
    return pxpValidator.validate(pxMeasurementMetadata);
  }

  public Optional<Domain> inferDomainByPropertyTypes(Collection<String> propertyTypes) {
    return Optional.ofNullable(determinDomain(propertyTypes));
  }

  private static Domain determinDomain(Collection<String> propertyTypes) {
    if (MeasurementNGSValidator.isNGS(propertyTypes)) {
      return Domain.NGS;
    }
    if (MeasurementProteomicsValidator.isProteomics(propertyTypes)) {
      return Domain.PROTEOMICS;
    }
    return null;
  }

  public enum Domain {
    NGS, PROTEOMICS
  }


}
