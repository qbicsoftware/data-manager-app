package life.qbic.datamanager.views.demo;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.annotation.UIScope;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import life.qbic.datamanager.views.general.dialog.AppDialog;
import life.qbic.datamanager.views.general.dialog.DialogBody;
import life.qbic.datamanager.views.general.dialog.DialogFooter;
import life.qbic.datamanager.views.general.dialog.DialogHeader;
import life.qbic.datamanager.views.general.dialog.DialogSection;
import life.qbic.datamanager.views.general.dialog.InputValidation;
import life.qbic.datamanager.views.general.dialog.UserInput;
import life.qbic.datamanager.views.general.dialog.stepper.Step;
import life.qbic.datamanager.views.general.dialog.stepper.StepperDisplay;
import life.qbic.datamanager.views.general.dialog.stepper.StepperDialog;
import life.qbic.datamanager.views.general.dialog.stepper.StepperDialogFooter;
import life.qbic.datamanager.views.general.icon.IconFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * <b>Component Demo</b>
 *
 * <p>Showcases of various pre-defined styles used in the application.</p>
 *
 * @since 1.7.0
 */
@Profile("test-ui") // This view will only be available when the "test-ui" profile is active
@Route("test-view")
@UIScope
@AnonymousAllowed
@Component
public class ComponentDemo extends Div {

  public static final String HEADING_2 = "heading-2";
  Div title = new Div("Data Manager - Component Demo");

  public ComponentDemo() {
    title.addClassName("heading-1");
    addClassNames("padding-left-right-07", "padding-top-bottom-04");
    add(title);
    add(headingShowcase());
    add(fontsShowCase());
    add(dialogShowCase(AppDialog.small(), "Small Dialog Type"));
    add(dialogShowCase(AppDialog.medium(), "Medium Dialog Type"));
    add(dialogShowCase(AppDialog.large(), "Large Dialog Type"));
    add(dialogSectionShowCase());
    add(stepperDialogShowCase(threeSteps(), "Three steps example"));
  }

  private static Div dialogSectionShowCase() {
    Div container = new Div();
    var title = new Div("Dialog section example");
    title.addClassName(HEADING_2);
    container.add(title);
    container.add(DialogSection.with("Dialog section title...",
        "...Followed by some descriptive text about the content or instructions.",
        new TextField("Some component, like a text field")));
    return container;
  }

  private static Div headingShowcase() {
    Div container = new Div();
    Div header = new Div();
    header.addClassName(HEADING_2);
    header.setText("Heading styles");
    container.add(header);

    for (int i = 1; i < 7; i++) {
      Div heading = new Div();
      heading.addClassName("heading-" + i);
      heading.setText("Heading " + i);
      Div description = new Div();
      description.addClassName("normal-body-text");
      description.setText("CSS class: %s".formatted(".heading-" + i));
      container.add(heading, description);
    }

    return container;
  }

  private static Div fontsShowCase() {
    Div container = new Div();
    Div header = new Div("Body Font Styles");
    header.addClassName(HEADING_2);
    container.add(header);
    container.addClassNames("flex-vertical", "gap-04");

    Arrays.stream(BodyFontStyles.fontStyles).forEach(fontStyle -> {
      Div styleHeader = new Div();
      styleHeader.addClassName("heading-4");
      styleHeader.setText(fontStyle);
      container.add(styleHeader);
      Div style = new Div();
      style.addClassName(fontStyle);
      style.setText(
          ("This is an example of the '.%s' font style.%n"
              + " And it continues in this additional line to demonstrate its line-height. Just make "
              + "the window smaller until the text starts to wrap over multiple "
              + "lines on the screen. ").formatted(
              fontStyle));
      container.add(style);
    });

    return container;
  }

  private static Div stepperDialogShowCase(List<Step> steps, String dialogTitle) {
    Div content = new Div();
    Div title = new Div("Stepper Dialog");
    title.addClassName(HEADING_2);
    Button showDialog = new Button("Show Stepper");
    AppDialog dialog = AppDialog.medium();

    DialogHeader.with(dialog, dialogTitle);
    StepperDialog stepperDialog = StepperDialog.create(dialog, steps);
    StepperDialogFooter.with(stepperDialog);

    StepperDisplay.with(stepperDialog, steps.stream().map(Step::name).toList());

    showDialog.addClickListener(listener -> stepperDialog.open());

    content.add(title);
    content.add(showDialog);
    content.addClassNames("flex-vertical", "gap-04");

    Div confirmBox = new Div("Click the button and press 'Cancel' or 'Save'");
    dialog.registerConfirmAction(() -> {
      confirmBox.setText("Stepper dialog has been confirmed");
      dialog.close();
    });

    dialog.registerCancelAction(() -> {
      confirmBox.setText("Stepper dialog has been cancelled");
      dialog.close();
    });

    content.add(confirmBox);

    return content;
  }

  private static List<Step> threeSteps() {
    List<Step> steps = new ArrayList<>();
    for (int step= 0; step < 3; step++) {
      int stepNumber = step;
      steps.add(new Step() {

        final ExampleUserInput userInput = new ExampleUserInput("example step " + stepNumber );


        @Override
        public String name() {
          return "Step " + stepNumber;
        }

        @Override
        public com.vaadin.flow.component.Component component() {
          return userInput;
        }

        @Override
        public UserInput userInput() {
          return userInput;
        }
      });
    }
    return steps;
  }

  private static Div dialogShowCase(AppDialog dialog, String dialogType) {
    Div content = new Div();
    Div title = new Div();
    title.addClassName(HEADING_2);
    title.setText(dialogType);
    content.add(title);
    Button showDialog = new Button("Show Dialog");
    // Dialog set-up
    DialogHeader.withIcon(dialog, dialogType, IconFactory.warningIcon());
    DialogFooter.with(dialog, "Cancel", "Save");
    ExampleUserInput userInput = new ExampleUserInput("Expelliarmus");
    DialogBody.with(dialog, userInput, userInput);

    Div confirmBox = new Div("Click the button and press 'Cancel' or 'Save'");
    showDialog.addClickListener(e -> {
      dialog.open();
      confirmBox.setText("Cancelled the dialog.");
    });

    dialog.registerCancelAction(() -> {
      dialog.close();
      if (dialog.hasChanges()) {
        confirmBox.setText("Cancelled the dialog although there where changes made!");
      } else {
        confirmBox.setText("Cancelled the dialog. No changes.");
      }
    });
    dialog.registerConfirmAction(() -> {
      dialog.close();
      confirmBox.setText("Confirmed the dialog.");
    });

    content.add(showDialog, confirmBox);
    content.addClassNames("flex-vertical", "gap-04");
    return content;
  }

  private static class BodyFontStyles {

    static String[] fontStyles = new String[]{
        "normal-body-text",
        "small-body-text",
        "extra-small-body-text",
        "field-label-text",
        "input-field-text",
        "list-item-text",
        "button.text"
    };
  }

  private static class ExampleUserInput extends Div implements UserInput {

    private final String originalValue;
    private final transient StringBean valueContainer;
    Binder<StringBean> binder;

    ExampleUserInput(String prefill) {
      var dialogSection = DialogSection.with("User Input Validation", "Try correct and incorrect input values in the following field.");
      originalValue = prefill;
      var textField = new TextField();
      textField.setLabel("Correct input is 'Riddikulus'");
      textField.setPlaceholder("Type your answer here");
      textField.setWidth(20.f, Unit.REM);
      textField.setValueChangeMode(ValueChangeMode.EAGER);

      valueContainer = new StringBean(prefill);
      binder = new Binder<>(StringBean.class);
      binder.forField(textField)
          .withValidator((String value) -> value.equals("Riddikulus"), "Wrong input text.")
          .bind(StringBean::getValue, StringBean::setValue);
      binder.setBean(valueContainer);
      dialogSection.content(textField);
      add(dialogSection);
    }

    @Override
    @NonNull
    public InputValidation validate() {
      if (binder.validate().hasErrors()) {
        return InputValidation.failed();
      }
      return InputValidation.passed();
    }

    @Override
    public boolean hasChanges() {
      return binder.hasChanges() || !Objects.equals(valueContainer.getValue(), originalValue);
    }
  }

  private static class StringBean {

    private String value;

    StringBean(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }
  }

}
