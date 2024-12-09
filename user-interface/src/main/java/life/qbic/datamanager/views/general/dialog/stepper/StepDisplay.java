package life.qbic.datamanager.views.general.dialog.stepper;

import com.vaadin.flow.component.html.Div;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class StepDisplay extends Div {

  public static final String ICON_BACKGROUND_COLOR_DEFAULT = "icon-background-color-default";
  public static final String ICON_BACKGROUND_COLOR_PRIMARY = "icon-background-color-primary";
  public static final String ICON_LABEL_TEXT_COLOR_DEFAULT = "icon-label-text-color-default";
  public static final String ICON_LABEL_TEXT_COLOR_PRIMARY = "icon-label-text-color-primary";
  private final Div numberIcon;
  private final Div stepLabel;

  private StepDisplay(int number, String label) {
    this.addClassNames("flex-vertical", "gap-02", "flex-align-items-center");
    this.numberIcon = new Div(String.valueOf(number));
    this.stepLabel = new Div(label);
    numberIcon.addClassNames("round", "icon-size-m", ICON_BACKGROUND_COLOR_DEFAULT,
        "icon-text-white", "icon-content-center", "icon-text-inner");
    stepLabel.addClassNames("icon-label-text", ICON_LABEL_TEXT_COLOR_DEFAULT);
    add(numberIcon, stepLabel);
  }

  public static StepDisplay with(int number, String label) {
    return new StepDisplay(number, label);
  }

  public void activate() {
    numberIcon.removeClassName(ICON_BACKGROUND_COLOR_DEFAULT);
    numberIcon.addClassName(ICON_BACKGROUND_COLOR_PRIMARY);

    stepLabel.removeClassName(ICON_LABEL_TEXT_COLOR_DEFAULT);
    stepLabel.addClassName(ICON_LABEL_TEXT_COLOR_PRIMARY);
  }

  public void deactivate() {
    numberIcon.removeClassName(ICON_BACKGROUND_COLOR_PRIMARY);
    numberIcon.addClassName(ICON_BACKGROUND_COLOR_DEFAULT);

    stepLabel.removeClassName(ICON_LABEL_TEXT_COLOR_PRIMARY);
    stepLabel.addClassName(ICON_LABEL_TEXT_COLOR_DEFAULT);
  }
}
