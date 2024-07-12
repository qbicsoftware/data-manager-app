package life.qbic.datamanager.views.notifications;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.theme.lumo.LumoIcon;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public class Toast extends Notification {

  protected final Div layout;
  protected Component content;

  private final Button closeButton;
  private boolean closeable;

  protected Toast() {
    super();
    addClassName("toast-notification");
    layout = new Div();
    closeButton = new Button(LumoIcon.CROSS.create());
    closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    closeButton.addClickListener(it -> this.close());
    closeButton.setVisible(false);
    add(layout, closeButton);
    setCloseable(true);

    setPosition(Position.BOTTOM_START);
    setDuration(8_000);
  }


  public static Toast createWithText(String text) {
    return create(new Span(text));
  }

  public static Toast create(Component content) {
    requireNonNull(content, "content must not be null");
    Toast toast = new Toast();
    toast.setContent(content);
    return toast;
  }

  public Toast setContent(Component content) {
    if (nonNull(this.content)) {
      this.content.removeFromParent();
    }
    this.content = requireNonNull(content, "content must not be null");
    this.content.addClassName("toast-content");
    layout.addComponentAtIndex(Math.max(0, layout.getComponentCount() - 1), this.content);
    return this;
  }

  public boolean isCloseable() {
    return closeable;
  }

  public Toast setCloseable(boolean closeable) {
    closeButton.setVisible(closeable);
    this.closeable = closeable;
    return this;
  }

  public static Component createRoutingContent(Component content, Component routing) {
    var container = new Div();
    container.addClassName("routing-container");
    container.add(container, routing);
    return container;
  }
}
