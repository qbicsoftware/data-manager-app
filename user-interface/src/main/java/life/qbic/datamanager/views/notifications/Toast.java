package life.qbic.datamanager.views.notifications;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;
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

  protected Component content;

  private final Button closeButton;
  private boolean closeable;
  private Type type;

  protected enum Type {
    SUCCESS,
    INFO
  }

  protected Toast() {
    super();
    addClassName("toast-notification");
    closeButton = new Button(LumoIcon.CROSS.create());
    closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    closeButton.addClickListener(it -> this.close());
    closeButton.addClassName("close-button");
    closeButton.setVisible(false);
    add(closeButton);
    setCloseable(true);
    setPosition(Position.BOTTOM_START);
    setType(Type.INFO);
  }

  public Toast success() {
    setType(Type.SUCCESS);
    return this;
  }

  public Toast info() {
    setType(Type.INFO);
    return this;
  }

  private void setType(Type type) {
    this.type = requireNonNull(type, "type must not be null");
  }


  public static Toast createWithText(String text) {
    return create(new Html("<div class=\"text-content\">%s</div>".formatted(text)));
  }

  public static Toast create(Component content) {
    requireNonNull(content, "content must not be null");
    Toast toast = new Toast();
    toast.setContent(content);
    return toast;
  }

  public static Toast createWithRouting(Component content, String linkText,
      Class<? extends Component> navigationTarget, RouteParameters routeParameters) {
    var toast = new Toast();
    toast.setContent(Toast.createRoutingContent(content,
        toast.createRoutingComponent(linkText, navigationTarget, routeParameters)));
    return toast;
  }

  public Toast setContent(Component content) {
    if (nonNull(this.content)) {
      this.content.removeFromParent();
    }
    this.content = requireNonNull(content, "content must not be null");
    this.content.addClassName("toast-content");
    refresh();
    return this;
  }

  private void refresh() {
    removeAll();
    add(this.content, this.closeButton);
  }

  public boolean isCloseable() {
    return closeable;
  }

  public Toast setCloseable(boolean closeable) {
    closeButton.setVisible(closeable);
    this.closeable = closeable;
    return this;
  }

  public static Component createRoutingContent(Component content, Component routingComponent) {
    var container = new Div();
    container.addClassName("routing-container");
    content.addClassName("routing-content");
    routingComponent.addClassName("routing-link");
    container.add(content, routingComponent);
    return container;
  }

  public Component createRoutingComponent(String text,
      Class<? extends Component> navigationTarget, RouteParameters routeParameters) {
    var routerLink = new RouterLink(navigationTarget, routeParameters);
    routerLink.addClassName("routing-link");
    Button button = new Button(text);
    button.addClassName("routing-button");
    button.addClickListener(routingClicked -> close());
    routerLink.add(button);
    return routerLink;
  }
}
