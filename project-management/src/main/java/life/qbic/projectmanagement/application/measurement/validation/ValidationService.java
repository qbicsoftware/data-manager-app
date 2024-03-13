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
 * service to let the user fail early with detailed provided {@link ValidationResult}.
 *
 * @since 1.0.0
 */
@Service
public class ValidationService {

  private final NGSValidator ngsValidator;

  private final ProteomicsValidator pxValidator;

  @Autowired
  public ValidationService(NGSValidator ngsValidator, ProteomicsValidator pxValidator) {
    this.ngsValidator = ngsValidator;
    this.pxValidator = pxValidator;
  }

  public ValidationResult validateNGS(NGSMeasurementMetadata ngsMeasurementMetadata) {
    return ngsValidator.validate(ngsMeasurementMetadata);
  }

  /**
   * This method validates a proteomic measurement metadata object in the case of a new measurement
   * that is going to be registered.
   *
   * It will validate all properties exhaustive, but also is not aware of any present measurement id, which
   * also will not get validated.
   *
   * If you want to validate a measurement update, please use
   *
   * @param pxMeasurementMetadata
   * @return
   * @since
   */
  public ValidationResult validateProteomics(ProteomicsMeasurementMetadata pxMeasurementMetadata) {
    return pxValidator.validate(pxMeasurementMetadata);
  }

  public ValidationResult validateProteomicsUpdate(ProteomicsMeasurementMetadata pxMeasurementMetadata) {
    return pxValidator.validateUpdate(pxMeasurementMetadata);
  }

  public Optional<Domain> inferDomainByPropertyTypes(Collection<String> propertyTypes) {
    return Optional.ofNullable(determinDomain(propertyTypes));
  }

  private static Domain determinDomain(Collection<String> propertyTypes) {
    if (NGSValidator.isNGS(propertyTypes)) {
      return Domain.NGS;
    }
    if (ProteomicsValidator.isProteomics(propertyTypes)) {
      return Domain.PROTEOMICS;
    }
    return null;
  }

  public enum Domain {
    NGS, PROTEOMICS
  }


}
