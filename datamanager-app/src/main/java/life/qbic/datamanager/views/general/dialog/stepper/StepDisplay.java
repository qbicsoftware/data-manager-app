package life.qbic.datamanager.views.general.dialog.stepper;

import com.vaadin.flow.component.html.Div;

/**
 * <b>Step Display</b>
 *
 * <p>A visualisation of an individual step in a {@link StepperDisplay}. Contains an step icon with
 * the step number and the step name.</p>
 *
 * @since 1.7.0
 */
public class StepDisplay extends Div {

  public static final String ICON_BACKGROUND_COLOR_DEFAULT = "icon-background-color-default";
  public static final String ICON_BACKGROUND_COLOR_PRIMARY = "icon-background-color-primary";
  public static final String ICON_LABEL_TEXT_COLOR_DEFAULT = "icon-label-text-color-default";
  public static final String ICON_LABEL_TEXT_COLOR_PRIMARY = "icon-label-text-color-primary";
  private final Div numberIcon;
  private final Div stepLabel;

  private StepDisplay(int number, String label) {
    this.addClassNames("flex-vertical", "gap-03", "flex-align-items-center");
    this.numberIcon = new Div(String.valueOf(number));
    this.stepLabel = new Div(label);
    numberIcon.addClassNames("round", "icon-size-m", ICON_BACKGROUND_COLOR_DEFAULT,
        "icon-text-white", "icon-content-center", "icon-text-inner");
    stepLabel.addClassNames("dialog-step-name-text", ICON_LABEL_TEXT_COLOR_DEFAULT);
    add(numberIcon, stepLabel);
  }

  /**
   * Creates a {@link StepDisplay} with a step number and step label.
   * <p>
   * By default, the step display is deactivated. To activate it, {@link #activate()} can be
   * called.
   *
   * @param number the step number
   * @param label  the step label
   * @return a step display
   * @since 1.70
   */
  public static StepDisplay with(int number, String label) {
    return new StepDisplay(number, label);
  }

  /**
   * Activates the current {@link StepDisplay}, which highlights it over deactivated step displays
   * in a {@link StepperDisplay}.
   *
   * @since 1.7.0
   */
  public void activate() {
    numberIcon.removeClassName(ICON_BACKGROUND_COLOR_DEFAULT);
    numberIcon.addClassName(ICON_BACKGROUND_COLOR_PRIMARY);

    stepLabel.removeClassName(ICON_LABEL_TEXT_COLOR_DEFAULT);
    stepLabel.addClassName(ICON_LABEL_TEXT_COLOR_PRIMARY);
  }

  /**
   * Deactivates the current {@link StepDisplay}, which removes any changes made to highlight it.
   *
   * @since 1.7.0
   */
  public void deactivate() {
    numberIcon.removeClassName(ICON_BACKGROUND_COLOR_PRIMARY);
    numberIcon.addClassName(ICON_BACKGROUND_COLOR_DEFAULT);

    stepLabel.removeClassName(ICON_LABEL_TEXT_COLOR_PRIMARY);
    stepLabel.addClassName(ICON_LABEL_TEXT_COLOR_DEFAULT);
  }
}
