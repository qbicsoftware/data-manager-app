package life.qbic.datamanager.views.general.grid.component;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.shared.Registration;
import java.util.Arrays;
import java.util.Objects;
import org.springframework.lang.NonNull;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public final class FilterGridTabSheet extends TabSheet {

  private final PrimaryActionButtonGroup primaryActionGroup;

  public FilterGridTabSheet(FilterGridTab... tabs) {
    super();
    Arrays.stream(tabs).forEach(tab -> {
      add(tab, tab.getComponent());
    });
    this.primaryActionGroup = new PrimaryActionButtonGroup(
        mainActionButton("Primary Action"),
        mainFeatureButton("Main Feature"));

    setSuffixComponent(primaryActionGroup);
  }

  public Registration addPrimaryActionButtonListener(
      ComponentEventListener<ClickEvent<Button>> listener
  ) {
    return primaryActionGroup.addClickListenerPrimaryAction(listener);
  }

  public Registration addPrimaryFeatureButtonListener(
      ComponentEventListener<ClickEvent<Button>> listener
  ) {
    return primaryActionGroup.addClickListenerFeature(listener);
  }


  private static Button mainActionButton(String caption) {
    var button = new Button(caption);
    button.addClassName("button-color-primary");
    return button;
  }

  private static Button mainFeatureButton(String caption) {
    return new Button(caption);
  }

  private static class PrimaryActionButtonGroup extends Div {

    private final Button actionButton;
    private final Button featureButton;

    public PrimaryActionButtonGroup(@NonNull Button actionButton, @NonNull Button featureButton) {
      this.actionButton = Objects.requireNonNull(actionButton);
      this.featureButton = Objects.requireNonNull(featureButton);
      add(actionButton, featureButton);
    }

    Registration addClickListenerPrimaryAction(
        ComponentEventListener<ClickEvent<Button>> listener) {
      return actionButton.addClickListener(listener);
    }

    Registration addClickListenerFeature(
        ComponentEventListener<ClickEvent<Button>> listener) {
      return featureButton.addClickListener(listener);
    }
  }
}
