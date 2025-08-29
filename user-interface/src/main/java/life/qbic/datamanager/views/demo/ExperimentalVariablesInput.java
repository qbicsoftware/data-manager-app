package life.qbic.datamanager.views.demo;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.ShortcutRegistration;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.shared.Registration;
import java.util.Optional;
import java.util.function.Consumer;
import life.qbic.datamanager.views.general.ButtonFactory;
import org.springframework.lang.NonNull;

public class ExperimentalVariablesInput extends Composite<Div> {


  @Override
  protected Div initContent() {
    var body = super.initContent();
    body.addClassNames(
        "dialog-section border dashed padding-vertical-05 padding-horizontal-07 margin-05");
    var variablesInput = new Div();
    variablesInput.addClassNames("flex-vertical gap-04");
    for (int i = 0; i < 4; i++) {
      variablesInput.add(variableRow());
    }
    var addVariableButton = new Button("Add Variable", VaadinIcon.PLUS.create());
    addVariableButton.addClassNames(
        "margin-bottom-04 flex-vertical width-max-content justify-self-start button-color-primary");
    variablesInput.add(addVariableButton);
    body.add(variablesInput);
    return body;
  }


  private Component variableRow() {
    var root = new Div();
    root.addClassNames(
        "border rounded-02 padding-04 gap-04 column-gap-05 grid-experimental-variable-input");
    var fields = new Div();
    fields.getStyle().set("grid-area", "a");
    fields.addClassNames("flex-horizontal gap-05");
    TextField name = new TextField();
    name.addClassNames("dynamic-growing-flex-item");
    name.setLabel("Variable Name");
    TextField unit = new TextField();
    unit.addClassNames("dynamic-growing-flex-item");
    unit.setLabel("Unit (optional)");
    Component variableLevels = createVariableLevels();
    variableLevels.getStyle().set("grid-area", "b");
    var deleteVariable = new ButtonFactory().createTertirayButton("Delete Variable",
        VaadinIcon.TRASH.create());
    deleteVariable.getStyle().set("grid-area", "c");
    deleteVariable.addClassNames("width-max-content");
    fields.add(name, unit);
    root.add(fields, variableLevels, deleteVariable);
    return root;
  }

  @Tag("variable-levels-input")
  private static class VariableLevelsInput extends Div {

    public VariableLevelsInput() {
      final String rootCssClasses = "border rounded-02 flex-vertical gap-none padding-04 padding-top-04";
      final String bodyClassNames = "flex-vertical justify-start gap-04 input-with-label";
      Div root = new Div();
      root.getStyle().set("grid-area", "b");
      root.addClassNames(rootCssClasses);

      var body = new Div();
      body.addClassNames(bodyClassNames);

      final String levelsContainerCss = "flex-horizontal gap-03 column-gap-03 width-full";
      var levelsContainer = new Div();
      levelsContainer.setId("levels-container"); //needed for labelling
      levelsContainer.addClassNames(levelsContainerCss);
      ButtonFactory buttonFactory = new ButtonFactory();

      String labelCss = "form-label input-label";
      var label = new NativeLabel("Levels");
      label.setFor(levelsContainer);
      label.addClassNames(labelCss);

      LevelField levelField = new LevelField(buttonFactory);

      var addLevelButton = buttonFactory.createTertirayButton("Add Level",
          VaadinIcon.PLUS.create());
      addLevelButton.addClickListener(clickEvent -> {
        LevelField levelField1 = new LevelField(buttonFactory);
        levelsContainer.add(levelField1);
        levelField1.focus();
      });
      levelsContainer.add(levelField);
      body.add(levelsContainer, addLevelButton);
      addLevelButton.addClassNames("width-max-content justify-self-start");
      root.add(label, body);

      this.add(root);
    }
  }

  private Component createVariableLevels() {
    return new VariableLevelsInput();
  }

  private static class LevelField extends Composite<Div> implements Focusable<Component> {

    static final String LEVEL_CLASS = "level";
    static final String LEVEL_FIELD_CSS = "flex-horizontal gap-03 width-full no-flex-wrap no-wrap input-with-label";
    static final String LEVEL_VALUE_CSS = "dynamic-growing-flex-item";

    private final TextField levelValue = new TextField();
    private final Button deleteLevelButton;

    private LevelField(@NonNull ButtonFactory buttonFactory) {
      this.deleteLevelButton = buttonFactory.createIconButton(VaadinIcon.TRASH.create());
      deleteLevelButton.addClickListener(clickEvent -> {
        fireEvent(new DeleteLevelEvent(this, clickEvent.isFromClient()));
      });
    }


    @Override
    protected Div initContent() {
      var levelField = new Div();
      levelField.addClassNames(LEVEL_FIELD_CSS);
      levelValue.addClassNames(LEVEL_VALUE_CSS);
      levelField.add(levelValue, deleteLevelButton);
      levelField.addClassNames(LEVEL_CLASS);
      return levelField;
    }

    @Override
    public void focus() {
      levelValue.focus();
    }

    @Override
    public void setTabIndex(int tabIndex) {
      levelValue.setTabIndex(tabIndex);
    }

    @Override
    public int getTabIndex() {
      return levelValue.getTabIndex();
    }

    @Override
    public void blur() {
      levelValue.blur();
    }

    @Override
    public ShortcutRegistration addFocusShortcut(Key key, KeyModifier... keyModifiers) {
      return levelValue.addFocusShortcut(key, keyModifiers);
    }

    @Override
    public Registration addFocusListener(ComponentEventListener<FocusEvent<Component>> listener) {
      return levelValue.addFocusListener(
          it -> listener.onComponentEvent(new FocusEvent<>(it.getSource(), it.isFromClient())));
    }

    public void setValue(String value) {
      levelValue.setValue(value);
    }

    public boolean isEmpty() {
      return levelValue.isEmpty();
    }

    public Optional<String> getValue() {
      return levelValue.getOptionalValue();
    }

    Registration addDeleteListener(ComponentEventListener<DeleteLevelEvent> listener) {
      return addListener(DeleteLevelEvent.class, listener);
    }

    static class DeleteLevelEvent extends ComponentEvent<LevelField> {

      public DeleteLevelEvent(LevelField source, boolean fromClient) {
        super(source, fromClient);
      }
    }
  }

  private static class PasteSupport {

    static void addPasteListener(Component component, Consumer<String> contentConsumer) {
      String clipboardText = "event.clipboardData.getData(\"text/plain\")";
      DomListenerRegistration listenerRegistration = component.getElement()
          .addEventListener("paste", event -> {
            try {
              var pastedString = event.getEventData().getString(clipboardText);
              contentConsumer.accept(pastedString);
            } catch (Exception e) {
              //TODO inform user that the paste is not accepted
              System.err.println(e.getMessage());
              e.printStackTrace();
            }
          })
          .addEventData(clipboardText);
      listenerRegistration.preventDefault();
    }

  }


}
