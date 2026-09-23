package life.qbic.datamanager.views.projects.project.samples.registration.batch;

import com.vaadin.flow.component.html.Div;
import java.util.List;
import java.util.Objects;
import life.qbic.datamanager.views.general.dialog.DialogSection;
import life.qbic.datamanager.views.general.dialog.InputValidation;
import life.qbic.datamanager.views.general.dialog.UserInput;
import life.qbic.projectmanagement.application.sample.SampleMetadata;
import org.jspecify.annotations.NonNull;

/**
 * <b>Sample Update Component</b>
 * <p>
 * The body of the sample edit dialog: an export/template section offering the sample metadata
 * download (all or selected) plus the {@link SampleUpdateUpload} used to submit the edited sheet.
 *
 * @since 1.11.0
 */
public class SampleUpdateComponent extends Div implements UserInput {

  private final SampleUpdateUpload sampleUpdateUpload;

  public SampleUpdateComponent(
      SampleTemplateComponent templateComponent,
      SampleUpdateUpload sampleUpdateUpload) {
    this.sampleUpdateUpload = Objects.requireNonNull(sampleUpdateUpload);

    var templateSection = DialogSection.with("Export Metadata", templateComponent);

    add(templateSection);
    add(sampleUpdateUpload);
    addClassNames("flex-vertical", "gap-06");
  }

  @Override
  @NonNull
  public InputValidation validate() {
    return sampleUpdateUpload.validate();
  }

  @Override
  public boolean hasChanges() {
    return sampleUpdateUpload.hasChanges();
  }

  /**
   * @return the validated sample metadata ready to be submitted for update
   */
  public List<SampleMetadata> getValidatedSampleMetadata() {
    return sampleUpdateUpload.getValidatedSampleMetadata();
  }
}