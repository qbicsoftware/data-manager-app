package life.qbic.datamanager.views.projects.project.samples.registration.batch;

import com.vaadin.flow.component.html.Div;
import java.util.List;
import java.util.Objects;
import life.qbic.datamanager.views.general.dialog.DialogSection;
import life.qbic.datamanager.views.general.dialog.InputValidation;
import life.qbic.datamanager.views.general.dialog.UserInput;
import life.qbic.projectmanagement.application.api.AsyncProjectService.SampleRegistrationInformation;
import org.jspecify.annotations.NonNull;

/**
 * <b>Sample Registration Component</b>
 * <p>
 * The body of the sample registration dialog: an export/template section offering the blank sample
 * metadata template plus the {@link SampleRegistrationUpload} used to submit the filled sheet.
 *
 * @since 1.11.0
 */
public class SampleRegistrationComponent extends Div implements UserInput {

  private final SampleRegistrationUpload sampleRegistrationUpload;

  public SampleRegistrationComponent(
      SampleTemplateComponent templateComponent,
      SampleRegistrationUpload sampleRegistrationUpload) {
    this.sampleRegistrationUpload = Objects.requireNonNull(sampleRegistrationUpload);

    var templateSection = DialogSection.with("Export Metadata", templateComponent);

    add(templateSection);
    add(sampleRegistrationUpload);
    addClassNames("flex-vertical", "gap-06");
  }

  @Override
  @NonNull
  public InputValidation validate() {
    return sampleRegistrationUpload.validate();
  }

  @Override
  public boolean hasChanges() {
    return sampleRegistrationUpload.hasChanges();
  }

  /**
   * @return the validated sample registrations ready to be submitted
   */
  public List<SampleRegistrationInformation> getValidatedSampleMetadata() {
    return sampleRegistrationUpload.getValidatedSampleMetadata();
  }
}