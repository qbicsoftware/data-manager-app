package life.qbic.datamanager.views.projects.project.measurements;

import com.vaadin.flow.component.html.Div;
import java.util.Objects;
import life.qbic.datamanager.files.parsing.converters.ConverterRegistry;
import life.qbic.datamanager.files.parsing.converters.MetadataConverterV2;
import life.qbic.datamanager.views.general.dialog.InputValidation;
import life.qbic.datamanager.views.general.dialog.UserInput;
import life.qbic.datamanager.views.projects.project.measurements.MeasurementTemplateSelectionComponent.Domain;
import life.qbic.datamanager.views.projects.project.measurements.registration.MeasurementUpload;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementRegistrationInformationNGS;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementRegistrationInformationPxP;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ValidationRequestBody;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class MeasurementRegistrationComponent extends Div implements UserInput {

  private final MeasurementTemplateSelectionComponent templateSelectionComponent;
  private final MeasurementUpload measurementUpload;

  public MeasurementRegistrationComponent(
      MeasurementTemplateSelectionComponent templateSelectionComponent,
      MeasurementUpload measurementUpload, Domain defaultDomain) {
    this.templateSelectionComponent = Objects.requireNonNull(templateSelectionComponent);
    this.measurementUpload = Objects.requireNonNull(measurementUpload);
    syncComponents(defaultDomain);
    templateSelectionComponent.addDomainSelectionListener(
        event -> measurementUpload.setMetadataConverter(
            getConverterForDomain(templateSelectionComponent.selectedDomain())));
    add(templateSelectionComponent);
    add(measurementUpload);
  }

  private void syncComponents(Domain domain) {
    templateSelectionComponent.setSelectedDomain(domain);
    measurementUpload.setMetadataConverter(getConverterForDomain(domain));
  }

  private MetadataConverterV2<? extends ValidationRequestBody> getConverterForDomain(
      Domain domain) {
    return switch (domain) {
      case Genomics -> ConverterRegistry.converterFor(MeasurementRegistrationInformationNGS.class);
      case Proteomics ->
          ConverterRegistry.converterFor(MeasurementRegistrationInformationPxP.class);
    };
  }

  @Override
  public InputValidation validate() {
    return measurementUpload.validate();
  }

  @Override
  public boolean hasChanges() {
    return measurementUpload.hasChanges();
  }
}
