package life.qbic.datamanager.views.demo;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.OrderedList;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.CallbackDataProvider.CountCallback;
import com.vaadin.flow.data.provider.CallbackDataProvider.FetchCallback;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.annotation.UIScope;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import life.qbic.datamanager.views.StringBean;
import life.qbic.datamanager.views.general.Card;
import life.qbic.datamanager.views.general.DetailBox;
import life.qbic.datamanager.views.general.DetailBox.Header;
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
import life.qbic.datamanager.views.general.grid.PredicateFilter;
import life.qbic.datamanager.views.general.grid.component.FilterGrid;
import life.qbic.datamanager.views.general.grid.component.FilterGridTab;
import life.qbic.datamanager.views.general.grid.component.FilterGridTabSheet;
import life.qbic.datamanager.views.general.icon.IconFactory;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.experimentalvariable.ExperimentalVariablesInput;
import life.qbic.datamanager.views.projects.project.info.SimpleParagraph;
import life.qbic.logging.api.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.NonNull;

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
@org.springframework.stereotype.Component
public class ComponentDemo extends Div {

  private static final Logger log = logger(ComponentDemo.class);

  public static final String HEADING_2 = "heading-2";
  public static final String HEADING_3 = "heading-3";
  public static final String HEADING_4 = "heading-4";
  public static final String GAP_04 = "gap-04";
  public static final String FLEX_VERTICAL = "flex-vertical";
  public static final String NORMAL_BODY_TEXT = "normal-body-text";
  Div title = new Div("Data Manager - Component Demo");
  private final MessageSourceNotificationFactory messageFactory;

  @Autowired
  public ComponentDemo(MessageSourceNotificationFactory messageSourceNotificationFactory) {
    this.messageFactory = Objects.requireNonNull(messageSourceNotificationFactory);
    title.addClassName("heading-1");
    addClassNames("padding-horizontal-07", "padding-vertical-04");
    add(title);
    add(headingShowcase());
    add(colorShowCase());
    add(clickableShowCase());
    add(fontsShowCase());
    add(layoutsShowCase());
    add(detailBoxShowCase());
    add(dialogShowCase());
    add(cardShowCase());
    add(toastShowCase());
    add(borderShowcase());
    add(createTestComponent());
    add(filterGridShowCase());
  }

  private record SimplePersonFilter(String term) implements PredicateFilter<Person> {

    @Override
    public Optional<String> searchTerm() {
      return Optional.ofNullable(term);
    }

    public boolean test(Person data) {
      var fullName = String.join(" ", data.firstName, data.lastName, Integer.toString(data.age));
      return fullName.contains(term);
    }
  }

  private Div filterGridShowCase() {

    PseudoDataBackend<Person, SimplePersonFilter> personDataBackend = new PseudoDataBackend<>(
        examples,
        personFilter -> (person -> personFilter.test(person)));

    FetchCallback<Person, SimplePersonFilter> contactFetchCallback = query -> {
      var filter = query.getFilter().orElse(new SimplePersonFilter(""));
      var sorting = query.getSortOrders();

      var offset = query.getOffset();
      var limit = query.getLimit();
      return personDataBackend.fetch(offset, limit, filter);
    };

    CountCallback<Person, SimplePersonFilter> contactCountCallback = query -> {
      var filter = query.getFilter().orElse(new SimplePersonFilter(""));
      return personDataBackend.count(filter);
    };

    var gridPerson = new Grid<Person>();
    gridPerson.addColumn(Person::firstName).setHeader("First Name").setKey("firstName");
    gridPerson.addColumn(Person::lastName).setHeader("Last Name").setKey("lastName");
    gridPerson.addColumn(Person::age).setHeader("Age").setKey("age");

    var personGrid = new FilterGrid<>(
        Person.class,
        gridPerson,
        () -> new SimplePersonFilter(""),
        contactFetchCallback,
        contactCountCallback,
        (searchTerm, filter) -> new SimplePersonFilter(searchTerm)
    );

    personGrid.setSecondaryActionGroup(new Button("Edit"), new Button("Delete"));
    personGrid.itemDisplayLabel("person");

    var filterTab = new FilterGridTab<>("Persons", personGrid);

    var gridContact = new Grid<Person>();
    gridContact.addColumn(Person::firstName).setHeader("First Name").setKey("firstName");
    gridContact.addColumn(Person::lastName).setHeader("Last Name").setKey("lastName");
    gridContact.addColumn(Person::age).setHeader("Age").setKey("age");

    var contactGrid = new FilterGrid<>(
        Person.class,
        gridContact,
        () -> new SimplePersonFilter(""),
        contactFetchCallback,
        contactCountCallback,
        (searchTerm, filter) -> new SimplePersonFilter(searchTerm)
    );
    var filterTabContacts = new FilterGridTab<>("Contacts", contactGrid);

    var tabSheet = new FilterGridTabSheet(filterTab, filterTabContacts);

    tabSheet.addPrimaryFeatureButtonListener(event -> log.info(
        "Clicked on the primary feature button: click-count is " + event.getClickCount()));
    tabSheet.addPrimaryActionButtonListener(event -> log.info(
        "Clicked on the primary action button: click-count is " + event.getClickCount()));

    return new Div(tabSheet);
  }

  static class ExampleFilter implements Filter {

    private String term;

    ExampleFilter(String term) {
      this.term = term;
    }

    @Override
    public Optional<String> searchTerm() {
      return Optional.ofNullable(this.term);
    }

  }

  static List<Person> examples = new ArrayList<>();


  static {
    examples.add(new Person("John", "Doe", 18));
    examples.add(new Person("Jane", "Doe", 21));
    examples.add(new Person("Lars", "Müller", 27));
    examples.add(new Person("Alicia", "Mendez", 31));
    examples.add(new Person("Noah", "Thompson", 19));
    examples.add(new Person("Fatima", "Al-Sayed", 35));
    examples.add(new Person("Sakura", "Tanaka", 29));
    examples.add(new Person("Elena", "Petrova", 42));
    examples.add(new Person("Marcus", "Nguyen", 25));
    examples.add(new Person("Oliver", "Smith", 33));
    examples.add(new Person("Isabella", "Rossi", 24));
    examples.add(new Person("Ethan", "Johnson", 38));
    examples.add(new Person("Sofia", "Kowalski", 28));
    examples.add(new Person("Mateo", "Garcia", 40));
    examples.add(new Person("Hannah", "Schneider", 22));
    examples.add(new Person("Amir", "Rahman", 30));
    examples.add(new Person("Chloe", "Dubois", 26));
    examples.add(new Person("Leo", "Andersen", 34));
    examples.add(new Person("Priya", "Patel", 37));
    examples.add(new Person("William", "O'Connor", 45));
  }

  record Person(String firstName, String lastName, int age) {

  }

  private Component layoutsShowCase() {
    var gaps = new String[]{
        "gap-none",
        "gap-01",
        "gap-02",
        "gap-03",
        "gap-04",
        "gap-05",
        "gap-06",
        "gap-07"
    };
    var root = new Div();
    root.add(createHeading2("Layouts"));

    var verticalShowcaseBody = new Div();
    verticalShowcaseBody.addClassNames("flex-horizontal");
    for (String gap : gaps) {
      var vertLayout = new Div();
      vertLayout.addClassNames("flex-vertical margin-03 height-min-content border " + gap);
      for (int i = 0; i < 10; i++) {
        Div placeholder = createPlaceholder("padding-03", "border");
        placeholder.setText("placeholder " + (i + 1));
        vertLayout.add(placeholder);
      }
      verticalShowcaseBody.add(createHeading4(gap), vertLayout);

    }

    var horizontalShowcaseBody = new Div();
    horizontalShowcaseBody.addClassNames("flex-vertical");
    for (String gap : gaps) {
      var horizontalLayout = new Div();
      horizontalLayout.addClassNames("flex-horizontal margin-03 border width-50-pct " + gap);
      for (int i = 0; i < 10; i++) {
        Div placeholder = createPlaceholder("padding-03", "border padding-02");
        placeholder.setText("placeholder " + (i + 1));
        horizontalLayout.add(placeholder);
      }
      horizontalShowcaseBody.add(createHeading4(gap), horizontalLayout);
    }

    var columnGapDescriptor = new Span(
        "Sometimes it might be useful to differentiate between gap and column-gap. For this you can combine the gap classes with the column-gap classes.");
    var columnGapExample1 = new Div();
    columnGapExample1.addClassNames("border flex-horizontal width-50-pct gap-04 column-gap-none");
    for (int i = 0; i < 10; i++) {
      Div placeholder = createPlaceholder("padding-02", "background-color-grey", "border");
      placeholder.setText("placeholder " + (i + 1));
      columnGapExample1.add(placeholder);
    }

    var columnGapExample2 = new Div();
    columnGapExample2.addClassNames("border flex-horizontal width-50-pct gap-none column-gap-04");
    for (int i = 0; i < 10; i++) {
      Div placeholder = createPlaceholder("padding-02", "background-color-grey", "border");
      placeholder.setText("placeholder " + (i + 1));
      columnGapExample2.add(placeholder);
    }

    root.add(createHeading3("flex-vertical"), verticalShowcaseBody,
        createHeading3("flex-horizontal"), horizontalShowcaseBody,
        columnGapDescriptor,
        createHeading3("gap-04 column-gap-none"), columnGapExample1,
        createHeading3("gap-none column-gap-04"), columnGapExample2);
    return root;
  }

  private static Div createPlaceholder(String classNames, String... hiddenClassNames) {
    var comp = new Div();
    comp.addClassNames(classNames);
    comp.addClassNames(hiddenClassNames);
    comp.setText(classNames);
    return comp;
  }

  private Component createTestComponent() {
    return new ExperimentalVariablesInput();
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
    Div headingPrimaryText = createHeading3("color-primary-text");
    headingPrimaryText.addClassNames("color-primary-text");
    Div headingPrimary = createHeading3("color-primary");
    headingPrimary.addClassName("color-primary");
    Div headingSecondary = createHeading3("color-secondary");
    headingSecondary.addClassName("color-secondary");
    Div headingTertiary = createHeading3("color-tertiary");
    headingTertiary.addClassName("color-tertiary");
    container.add(heading, headingPrimaryText, headingPrimary, headingSecondary, headingTertiary);
    return container;
  }

  private static Div fontsShowCase() {
    Div container = new Div();
    Div header = new Div("Body Font Styles");
    header.addClassName(HEADING_2);
    container.add(header);
    container.addClassNames(FLEX_VERTICAL, GAP_04);

    Arrays.stream(BodyFontStyles.fontStyles).forEach(fontStyle -> {
      Div styleHeader = createHeading4(fontStyle);
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
        public Component component() {
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
      var toast = messageFactory.pendingTaskToast("task.in-progress",
          new Object[]{"Doing something really heavy here"}, getLocale());
      var succeededToast = messageFactory.toast("task.finished", new Object[]{"Heavy Task #1"},
          getLocale());
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
    withHeader.setHeader(new Header("What the details are about"));
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

  private static Div createHeading4(String text) {
    var heading = new Div();
    heading.setText(text);
    heading.addClassName(HEADING_4);
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
    container.add(createHeading3("Dialog with dangerous confirm action"));
    container.add(dialogWithDangerButton(AppDialog.small(), "Dialog with danger button"));

    return container;
  }

  private static Div dialogWithDangerButton(AppDialog dialog, String dialogType) {

    Div content = new Div();
    Button showDialog = new Button("Show Dialog");
    // Dialog set-up
    DialogHeader.withIcon(dialog, dialogType, IconFactory.warningIcon());
    DialogFooter.withDangerousConfirm(dialog, "Cancel", "Save");
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

  private static Div cardShowCase() {
    Div container = new Div();
    Div header = new Div();
    header.addClassName(HEADING_2);
    header.setText("Cards");
    container.add(header);

    Card card = new Card();
    card.addClassNames("padding-04");
    card.add(VaadinIcon.USER.create());
    card.add(new SimpleParagraph(
        "Some simple paragraph. The card has the `.padding-04` class assigned."));
    container.add(card);

    return container;
  }

  private Component borderShowcase() {
    var container = new Div();
    var header = new Div();
    header.addClassName(HEADING_2);
    header.setText("Border styles");
    container.add(header);
    container.addClassNames(FLEX_VERTICAL, GAP_04, "width-50-pct");
    var headerBorderVisibility = new Div();
    headerBorderVisibility.setText("Border Visibility");
    headerBorderVisibility.addClassNames(HEADING_3);
    var borderVisibilityDescription = new Div(
        "Border visibility is determined by the classes. You can choose to compose the border yourself using the .composite-border class."
            + " This class changes the behaviour of the other border classes. When assigning .composite-border, instead of being mutually exclusive, the border classes combine to achieve the desired visibility.");
    var borderPrecedence = new Div(
        "When no .composite-border class is present the following borders will take precedence (top beats bottom)");
    var borderPrecedenceList = new OrderedList();
    borderPrecedenceList.add(new ListItem(".border-left"));
    borderPrecedenceList.add(new ListItem(".border-bottom"));
    borderPrecedenceList.add(new ListItem(".border-right"));
    borderPrecedenceList.add(new ListItem(".border-top"));
    borderPrecedenceList.add(new ListItem(".border-top-bottom"));
    borderPrecedenceList.add(new ListItem(".border-left-right"));
    borderPrecedenceList.add(new ListItem(".border"));
    var border = new Div(".border");
    border.addClassNames("border");
    var borderClrDialog = new Div(".border .border-color-dialog");
    borderClrDialog.addClassNames("border", "border-color-dialog");
    var borderLeftRight = new Div(".border-left-right");
    borderLeftRight.addClassNames("border-left-right");
    var borderTopBottom = new Div(".border-top-bottom");
    borderTopBottom.addClassNames("border-top-bottom");
    var borderTop = new Div(".border-top");
    borderTop.addClassNames("border-top");
    var borderRight = new Div(".border-right");
    borderRight.addClassNames("border-right");
    var borderBottom = new Div(".border-bottom");
    borderBottom.addClassNames("border-bottom");
    var borderLeft = new Div(".border-left");
    borderLeft.addClassNames("border-left");

    var borderComposite = new Div(".composite-border");
    borderComposite.addClassNames("composite-border");
    var cbTop = new Div(".composite-border .border-top");
    cbTop.addClassNames("composite-border", "border-top");
    var cbTopRight = new Div(".composite-border .border-top .border-right");
    cbTopRight.addClassNames("composite-border", "border-top", "border-right");
    var cbTopRightBottom = new Div(".composite-border .border-top .border-right .border-bottom");
    cbTopRightBottom.addClassNames("composite-border", "border-top", "border-right",
        "border-bottom");

    var headerBorderStyles = new Div();
    headerBorderStyles.setText("Border Styles");
    headerBorderStyles.addClassNames(HEADING_3);
    var borderDashed = new Div(".border .dashed");
    borderDashed.addClassNames("border", "dashed");
    var borderRound = new Div(".border .round");
    borderRound.addClassNames("border", "round");
    var borderRounded02 = new Div(".border .rounded-02");
    borderRounded02.addClassNames("border", "rounded-02");
    var borderRounded03 = new Div(".border .rounded-03");
    borderRounded03.addClassNames("border", "rounded-03");

    container.add(
        border,
        borderClrDialog,
        headerBorderVisibility,
        borderLeftRight,
        borderTopBottom,
        borderTop,
        borderRight,
        borderBottom,
        borderLeft,
        borderVisibilityDescription,
        borderPrecedence,
        borderPrecedenceList,
        borderComposite,
        cbTop,
        cbTopRight,
        cbTopRightBottom,
        headerBorderStyles,
        borderDashed,
        borderRound,
        borderRounded02,
        borderRounded03
    );
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
