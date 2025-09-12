package life.qbic.datamanager.views.demo;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.annotation.UIScope;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import life.qbic.datamanager.views.StringBean;
import life.qbic.datamanager.views.general.Card;
import life.qbic.datamanager.views.general.DetailBox;
import life.qbic.datamanager.views.general.DetailBox.Header;
import life.qbic.datamanager.views.general.MultiSelectLazyLoadingGrid;
import life.qbic.datamanager.views.general.dialog.AppDialog;
import life.qbic.datamanager.views.general.dialog.DialogBody;
import life.qbic.datamanager.views.general.dialog.DialogFooter;
import life.qbic.datamanager.views.general.dialog.DialogHeader;
import life.qbic.datamanager.views.general.dialog.DialogSection;
import life.qbic.datamanager.views.general.dialog.InputValidation;
import life.qbic.datamanager.views.general.dialog.UserInput;
import life.qbic.datamanager.views.general.dialog.stepper.Step;
import life.qbic.datamanager.views.general.dialog.stepper.StepperDialog;
import life.qbic.datamanager.views.general.dialog.stepper.StepperDialogFooter;
import life.qbic.datamanager.views.general.dialog.stepper.StepperDisplay;
import life.qbic.datamanager.views.general.grid.Filter;
import life.qbic.datamanager.views.general.grid.FilterGrid;
import life.qbic.datamanager.views.general.icon.IconFactory;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.datamanager.views.projects.project.info.SimpleParagraph;
import org.springframework.beans.factory.annotation.Autowired;
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
@Profile("development") // This view will only be available when the "test-ui" profile is active
@Route("test-view")
@UIScope
@AnonymousAllowed
@Component
public class ComponentDemo extends Div {

  public static final String HEADING_2 = "heading-2";
  public static final String HEADING_3 = "heading-3";
  public static final String GAP_04 = "gap-04";
  public static final String FLEX_VERTICAL = "flex-vertical";
  public static final String NORMAL_BODY_TEXT = "normal-body-text";
  Div title = new Div("Data Manager - Component Demo");
  private final MessageSourceNotificationFactory messageFactory;

  @Autowired
  public ComponentDemo(MessageSourceNotificationFactory messageSourceNotificationFactory) {
    this.messageFactory = Objects.requireNonNull(messageSourceNotificationFactory);
    title.addClassName("heading-1");
    addClassNames("padding-left-right-07", "padding-top-bottom-04");
    add(title);
    add(headingShowcase());
    add(colorShowCase());
    add(clickableShowCase());
    add(fontsShowCase());
    add(detailBoxShowCase());
    add(dialogShowCase());
    add(cardShowCase());
    add(toastShowCase());
    add(filterGridShowCase());
  }

  private Div filterGridShowCase() {

    var grid = new MultiSelectLazyLoadingGrid<Person>();
    grid.addColumn(Person::firstName).setHeader("First Name").setKey("firstName");
    grid.addColumn(Person::lastName).setHeader("Last Name").setKey("lastName");
    grid.addColumn(Person::age).setHeader("Age").setKey("age");

    var filterDataProvider = DataProvider.<Person, Filter<Person>>fromFilteringCallbacks(query ->
        {
          var offsetIgnored = query.getOffset();
          var limitIgnored = query.getLimit();
          var filter = query.getFilter().orElse(new ExampleFilter(""));
          return examples.stream().filter(filter::test);
        }
        , count -> {
          var offsetIgnored = count.getOffset();
          var limitIgnored = count.getLimit();
          var filter = count.getFilter().orElse(new ExampleFilter(""));
          return examples.stream().filter(filter::test).toList().size();
        });

    var filterGrid = new FilterGrid<Person>(grid, filterDataProvider, new ExampleFilter(""), (filter, term) -> new ExampleFilter(term));


    return new Div(filterGrid);
  }

  class ExampleFilter implements Filter<Person> {

    private String term;

    ExampleFilter(String term) {
      this.term = term;
    }

    @Override
    public void setSearchTerm(String searchTerm) {
      this.term = searchTerm;
    }

    @Override
    public boolean test(Person data) {
      return data.firstName.contains(term) || data.lastName.contains(term);
    }
  }

  static List<Person> examples = new ArrayList<>();

  static {
    examples.add(new Person("John", "Doe", 18));
    examples.add(new Person("John", "Wane", 22));
    examples.add(new Person("Jae", "Doe", 44));
  }

  record Person(String firstName, String lastName, int age) {

  }

  private static Div clickableShowCase() {
    Div container = new Div();
    Div heading = createHeading2("Cursor Classes");
    Div headingClickable = createHeading3("clickable");
    headingClickable.addClassName("clickable");
    var handIcon = VaadinIcon.HAND.create();
    handIcon.addClassName("clickable");
    Div headingDefault = createHeading3("no class provided");
    var cursorIcon = VaadinIcon.CURSOR.create();
    container.add(heading, headingClickable, handIcon, headingDefault, cursorIcon);
    return container;
  }

  private static Div dialogSectionShowCase() {
    Div container = new Div();
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
      description.addClassName(NORMAL_BODY_TEXT);
      description.setText("CSS class: %s".formatted(".heading-" + i));
      container.add(heading, description);
    }

    return container;
  }

  private static Div colorShowCase() {
    Div container = new Div();
    Div heading = createHeading2("Color Classes");
    Div headingPrimary = createHeading3("color-primary");
    headingPrimary.addClassName("color-primary");
    Div headingSecondary = createHeading3("color-secondary");
    headingSecondary.addClassName("color-secondary");
    Div headingTertiary = createHeading3("color-tertiary");
    headingTertiary.addClassName("color-tertiary");
    container.add(heading, headingPrimary, headingSecondary, headingTertiary);
    return container;
  }
  private static Div fontsShowCase() {
    Div container = new Div();
    Div header = new Div("Body Font Styles");
    header.addClassName(HEADING_2);
    container.add(header);
    container.addClassNames(FLEX_VERTICAL, GAP_04);

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
    title.addClassName(HEADING_3);
    Button showDialog = new Button("Show Stepper");
    AppDialog dialog = AppDialog.medium();

    DialogHeader.with(dialog, dialogTitle);
    StepperDialog stepperDialog = StepperDialog.create(dialog, steps);
    StepperDialogFooter.with(stepperDialog);

    StepperDisplay.with(stepperDialog, steps.stream().map(Step::name).toList());

    showDialog.addClickListener(listener -> stepperDialog.open());

    content.add(title);
    content.add(showDialog);
    content.addClassNames(FLEX_VERTICAL, GAP_04);

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
    for (int step = 0; step < 3; step++) {
      int stepNumber = step + 1;
      steps.add(new Step() {

        final ExampleUserInput userInput = new ExampleUserInput("example step " + stepNumber);


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

  private static Div dialogWithOneButton(AppDialog dialog, String dialogType) {
    Div content = new Div();
    Button showDialog = new Button("Show Dialog");
    // Dialog set-up
    DialogHeader.withIcon(dialog, dialogType, IconFactory.warningIcon());
    DialogFooter.withConfirmOnly(dialog, "Close");
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
    content.addClassNames(FLEX_VERTICAL, GAP_04);
    return content;
  }

  private static Div dialogShowCase(AppDialog dialog, String dialogType) {
    Div content = new Div();
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
    content.addClassNames(FLEX_VERTICAL, GAP_04);
    return content;
  }

  private Div toastShowCase() {
    var title = new Div("Toast it!");
    title.addClassName(HEADING_2);
    var description = new Div(
        "Let's see how toasts work and also how to use them when we want to indicate a background task to the user.");
    description.addClassName(NORMAL_BODY_TEXT);
    var content = new Div();
    content.addClassNames(FLEX_VERTICAL, GAP_04);

    content.add(title);
    content.add(description);

    var button = new Button("Show Toast");

    content.add(button);

    button.addClickListener(e ->
    {
      var progressBar = new ProgressBar();
      progressBar.setIndeterminate(true);
      var toast = messageFactory.pendingTaskToast("task.in-progress", new Object[]{"Doing something really heavy here"}, getLocale());
      var succeededToast = messageFactory.toast("task.finished", new Object[]{"Heavy Task #1"},  getLocale());
      toast.open();
      var ui = UI.getCurrent();
      CompletableFuture.runAsync(() -> {
        try {
          Thread.sleep(5000);

        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
        }
      }).thenRunAsync(() -> {
        ui.access(() -> {
              toast.close();
              succeededToast.open();
            }
        );
      });
    });
    content.add(button);
    return content;
  }

  private static Div detailBoxShowCase() {
    Div container = new Div();
    container.add(createHeading2("Detail Box"));

    DetailBox emptyBox = new DetailBox();
    container.add(createHeading3("Empty Detail Box"));
    container.add(emptyBox);

    DetailBox withHeader = new DetailBox();
    withHeader.setHeader(new DetailBox.Header("What the details are about"));
    container.add(createHeading3("Empty Detail Box with Heading"));
    container.add(withHeader);

    DetailBox withHeaderAndIcon = new DetailBox();
    Header headerWithIcon = new Header(VaadinIcon.BUILDING.create(), "What the details are about");
    withHeaderAndIcon.setHeader(headerWithIcon);
    container.add(createHeading3("Empty Detail Box with Heading and Icon"));
    container.add(withHeaderAndIcon);

    Header header = new Header(VaadinIcon.SCATTER_CHART.create(), "What the details are about");
    Header header2 = new Header(VaadinIcon.SCATTER_CHART.create(), "What the details are about");

    DetailBox withSmallContent = new DetailBox();
    withSmallContent.setHeader(header);
    withSmallContent.setContent(
        new Div("Here are some details about what this is about. This example is a short text."));
    container.add(createHeading3("Small content in the box"));
    container.add(withSmallContent);

    DetailBox withLargeContent = new DetailBox();
    withLargeContent.setHeader(header2);
    withLargeContent.setContent(new Div("""
        Here are some details about what this is about. This example is a long text.
        orem ipsum dolor sit amet, consectetur adipiscing elit. Donec venenatis, nibh eget congue imperdiet, nisi enim porta odio, nec dapibus enim augue eget mauris. Mauris ultrices tortor nec arcu pretium consequat. Etiam sit amet nibh quis justo consectetur condimentum in id enim. Etiam nisl nisl, porta sit amet posuere ac, efficitur ut purus. Aenean elementum felis sit amet ligula mattis, malesuada lobortis nisi ornare. Praesent eleifend felis nec commodo maximus. Aliquam erat volutpat. Donec vel malesuada lorem. Mauris ut consequat dolor. Donec sit amet efficitur nulla.
        Proin orci turpis, ullamcorper eget magna ac, lacinia tristique orci. Aliquam a dui tempor, consequat neque vel, facilisis odio. Pellentesque vehicula augue id turpis gravida sodales. Mauris viverra leo sed enim faucibus, ac ultricies ipsum commodo. Phasellus at erat neque. Curabitur vitae lectus vel nisl posuere pretium nec tincidunt libero. Mauris ut mi vulputate, maximus mi semper, placerat nulla. Etiam velit massa, consequat eu varius eu, rhoncus sed urna. Integer at dolor diam. Aenean at egestas lacus. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. Morbi imperdiet consectetur iaculis.
        Nulla lobortis, nunc non molestie efficitur, ipsum justo pellentesque mi, ac pulvinar augue eros nec metus. Morbi ac fermentum dolor. Aenean hendrerit non ante dapibus tristique. Praesent sodales, libero quis consequat lobortis, elit lorem interdum sem, nec tincidunt mauris massa vel tortor. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. Cras non tempor est. Duis tellus urna, consectetur ac sodales sit amet, volutpat dapibus erat.
        Nunc dictum turpis eget tempus vestibulum. Morbi interdum vehicula ligula eget mollis. Ut interdum sit amet ex ut tempor. Nulla consectetur id metus vitae sollicitudin. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia curae; Praesent blandit non orci quis interdum. Mauris id gravida augue.\s
        """));
    container.add(createHeading3("Large content in the box"));
    container.add(withLargeContent);

    return container;
  }

  private static Div createHeading2(String text) {
    var heading = new Div();
    heading.setText(text);
    heading.addClassName(HEADING_2);
    return heading;
  }

  private static Div createHeading3(String text) {
    var heading = new Div();
    heading.setText(text);
    heading.addClassName(HEADING_3);
    return heading;
  }

  private static Div dialogShowCase() {
    Div container = new Div();
    container.add(createHeading2("Dialogs"));

    container.add(createHeading3("Small Dialog"));
    container.add(dialogShowCase(AppDialog.small(), "Small Dialog"));
    container.add(createHeading3("Medium Dialog"));
    container.add(dialogShowCase(AppDialog.medium(), "Medium Dialog"));
    container.add(createHeading3("Large Dialog"));
    container.add(dialogShowCase(AppDialog.large(), "Large Dialog Type"));
    container.add(createHeading3("Dialog Section"));
    container.add(dialogSectionShowCase());
    container.add(createHeading3("Three steps example"));
    container.add(stepperDialogShowCase(threeSteps(), "Three steps example"));
    container.add(createHeading3("Dialog with one button"));
    container.add(dialogWithOneButton(AppDialog.small(), "Dialog with one button"));

    return container;
  }

  private static Div cardShowCase() {
    Div container = new Div();
    Div header = new Div();
    header.addClassName(HEADING_2);
    header.setText("Cards");

    Card card = new Card();
    card.add(VaadinIcon.USER.create());
    card.add(new SimpleParagraph("Some simple paragraph"));
    container.add(card);

    container.add(header);
    return container;
  }

  private static class BodyFontStyles {

    static String[] fontStyles = new String[]{
        NORMAL_BODY_TEXT,
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
      var dialogSection = DialogSection.with("User Input Validation",
          "Try correct and incorrect input values in the following field.");
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

}
