package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.theme.lumo.LumoIcon;

/**
 * Info box
 * <p>
 * Small Info box component based on the {@link Span} component to inform the user. Can be set to be
 * closeable or have additional components if necessary
 */
public class InfoBox extends Span {

  private final Icon infoIcon = VaadinIcon.INFO_CIRCLE.create();
  private final Span infoText = new Span("");
  private final Icon removeInfoIcon = LumoIcon.CROSS.create();

  public InfoBox() {
    infoIcon.addClassNames("primary", "small");
    add(infoIcon);
    add(infoText);
    removeInfoIcon.addClassNames("clickable", "primary", "small");
    removeInfoIcon.addClickListener(event -> this.setVisible(false));
    addClassName("info-box");
  }

  public InfoBox setInfoText(String text) {
    infoText.setText(text);
    return this;
  }

  public InfoBox setClosable(boolean isClosable) {
    if (isClosable && !getChildren().toList().contains(removeInfoIcon)) {
      add(removeInfoIcon);
    }
    if (!isClosable && getChildren().toList().contains(removeInfoIcon)) {
      remove(removeInfoIcon);
    }
    return this;
  }
}
