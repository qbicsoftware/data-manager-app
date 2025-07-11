package life.qbic.datamanager.views.projects.project.measurements;

import com.vaadin.flow.component.html.Div;
import java.util.Objects;
import life.qbic.datamanager.views.general.dialog.DialogSection;
import life.qbic.datamanager.views.general.dialog.InputValidation;
import life.qbic.datamanager.views.general.dialog.UserInput;
import life.qbic.datamanager.views.projects.project.measurements.registration.MeasurementUpload;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class MeasurementUpdateComponent extends Div implements UserInput {


  private final MeasurementTemplateComponent templateComponent;
  private final MeasurementUpload measurementUpload;

  public MeasurementUpdateComponent(
      MeasurementTemplateComponent templateComponent,
      MeasurementUpload measurementUpload
  ) {
    this.templateComponent = Objects.requireNonNull(templateComponent);
    this.measurementUpload = Objects.requireNonNull(measurementUpload);

    var templateSection = DialogSection.with("Download Template", templateComponent);

    add(templateSection);
    add(measurementUpload);
    addClassNames("flex-vertical", "gap-06");
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
