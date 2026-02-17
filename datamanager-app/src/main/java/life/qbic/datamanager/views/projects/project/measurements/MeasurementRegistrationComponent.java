package life.qbic.datamanager.views.projects.project.measurements;

import com.vaadin.flow.component.html.Div;
import java.util.Objects;
import life.qbic.datamanager.files.parsing.converters.ConverterRegistry;
import life.qbic.datamanager.files.parsing.converters.MetadataConverterV2;
import life.qbic.datamanager.views.general.dialog.DialogSection;
import life.qbic.datamanager.views.general.dialog.InputValidation;
import life.qbic.datamanager.views.general.dialog.UserInput;
import life.qbic.datamanager.views.projects.project.measurements.MeasurementTemplateSelectionComponent.Domain;
import life.qbic.datamanager.views.projects.project.measurements.registration.MeasurementUpload;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementRegistrationInformationIP;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementRegistrationInformationNGS;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementRegistrationInformationPxP;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ValidationRequestBody;

/**
 * <b>Measurement Registration Component</b>
 * <p>
 * A component that orchestrates the selection of a user in a
 * {@link MeasurementTemplateSelectionComponent} and sets the {@link MeasurementUpload} with the
 * correct {@link MetadataConverterV2} based on the domain selection.
 *
 * @since 1.11.0
 */
public class MeasurementRegistrationComponent extends Div implements UserInput {

  private final MeasurementTemplateSelectionComponent templateSelectionComponent;
  private final MeasurementUpload measurementUpload;

  public MeasurementRegistrationComponent(
      MeasurementTemplateSelectionComponent templateSelectionComponent,
      MeasurementUpload measurementUpload,
      Domain defaultDomain) {
    this.templateSelectionComponent = Objects.requireNonNull(templateSelectionComponent);
    this.measurementUpload = Objects.requireNonNull(measurementUpload);
    // 1. We want to ensure that the correct metadata converter is set based on the domain selection
    syncComponents(Objects.requireNonNull(defaultDomain));

    // 2. Everytime the user changes the domain, the correct converter for the domain shall be set
    // for the measurement upload component. Only then, the validation will be done in the correct
    // domain context.
    templateSelectionComponent.addDomainSelectionListener(
        event -> measurementUpload.setMetadataConverter(
            getConverterForDomain(templateSelectionComponent.selectedDomain())));

    // 3. We add the components as children to this component
    var templateSection = DialogSection.with("Download domain-specific template",
        "Please select the relevant domain to download the domain-specific measurement metadata template.",
        templateSelectionComponent);
    add(templateSection);
    add(measurementUpload);
    addClassNames("flex-vertical", "gap-06");
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
      case ImmunoPeptidomics -> ConverterRegistry.converterFor(
          MeasurementRegistrationInformationIP.class);
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
