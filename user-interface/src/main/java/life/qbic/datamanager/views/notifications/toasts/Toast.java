package life.qbic.datamanager.views.notifications.toasts;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.router.BeforeLeaveListener;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoIcon;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import life.qbic.datamanager.views.general.ComponentFunctions;

/**
 * A toast notification is a non-modal, unobtrusive window element. It is meant to show information
 * in brief and auto-expiring windows to the user.
 * <p>
 * It is not meant to require user interaction but may provide optional ways for the user to
 * interact with it.
 *
 * @since 1.4.0
 */
public final class Toast extends Notification {

  private static final Position DEFAULT_POSITION = Position.BOTTOM_START;
  private static final boolean DEFAULT_CLOSEABLE = true;
  private static final boolean DEFAULT_CLOSE_ON_NAVIGATION = true;
  private static final Level DEFAULT_LEVEL = Level.INFO;
  private static final int DEFAULT_OPEN_DURATION = 5_000;

  private final List<Registration> closeOnNavigationListeners = new ArrayList<>();
  private final Button closeButton = closeButton(this);

  private Component content;
  private Level level;
  private boolean closeable;

  enum Level {
    SUCCESS,
    INFO;
  }

  static class ToastBuilder {

    Toast build() {
      return new Toast();
    }

  }

  private Toast() {
    super();
    addClassName("toast-notification");
    add(closeButton);
    setCloseable(DEFAULT_CLOSEABLE);
    setPosition(DEFAULT_POSITION);
    setType(DEFAULT_LEVEL);
    closeOnNavigation(DEFAULT_CLOSE_ON_NAVIGATION);
    setDuration(DEFAULT_OPEN_DURATION);
  }

  private static Button closeButton(Toast toast) {
    var button = new Button(LumoIcon.CROSS.create());
    button.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    button.addClickListener(it -> toast.close());
    button.addClassName("close-button");
    button.setVisible(false);
    return button;
  }

  /**
   * Sets whether toasts are closed after navigation. By default, Toasts no not stay open after
   * navigation.
   *
   * @param close whether to close the toast notification on navigation or not. true - closes the
   *              toast after navigation.
   * @return the modified toast
   */
  public Toast closeOnNavigation(boolean close) {
    //reset previous listeners
    closeOnNavigationListeners.forEach(Registration::remove);
    closeOnNavigationListeners.clear();
    if (!close) {
      return this;
    }
    Registration attachListenerRegistration = addAttachListener(closeToastBeforeLeave());
    closeOnNavigationListeners.add(attachListenerRegistration);
    return this;
  }

  private ComponentEventListener<AttachEvent> closeToastBeforeLeave() {
    return attachEvent -> {
      BeforeLeaveListener beforeLeaveListener = beforeLeaveEvent -> {
        if (attachEvent.getSource() instanceof Toast toast) {
          toast.close();
        }
      };
      UI ui = attachEvent.getUI();
      Registration beforeLeaveListenerRegistration = ui.addBeforeLeaveListener(beforeLeaveListener);
      closeOnNavigationListeners.add(beforeLeaveListenerRegistration);
    };
  }

  public void setDuration(Duration duration) {
    super.setDuration((int) duration.toMillis());
  }

  /**
   * The toast contains a success message
   *
   * @return
   */
  public Toast success() {
    setType(Level.SUCCESS);
    return this;
  }

  /**
   * The toast contains an informational message only
   *
   * @return
   */
  public Toast info() {
    setType(Level.INFO);
    return this;
  }

  @Override
  public void open() {
    var cssClass = switch (level) {
      case SUCCESS -> "success-toast";
      case INFO -> "info-toast";
    };
    addClassName(cssClass);
    super.open();
  }

  private void setType(Level level) {
    this.level = requireNonNull(level, "type must not be null");
  }


  /**
   * Creates a toast showing the component.
   *
   * @param content
   * @return
   */
  static Toast create(Component content) {
    requireNonNull(content, "content must not be null");
    Toast toast = new Toast();
    toast.withContent(content);
    return toast;
  }

  /**
   * Expands the toast and includes a link to a specific route target.
   *
   * @param linkText         The text of the link shown to the user
   * @param navigationTarget the target of the navigation
   * @param routeParameters  the parameters used for navigation
   * @return
   */
  public Toast withRouting(String linkText, Class<? extends Component> navigationTarget,
      RouteParameters routeParameters) {
    return this.withContent(Toast.createRoutingContent(this.content, createRoutingComponent(
        linkText, navigationTarget, routeParameters)));
  }

  /**
   * Sets the content of the toast
   *
   * @param content the content of the toast to set.
   * @return
   */
  private Toast withContent(Component content) {
    requireNonNull(content, "content must not be null");
    //we only want to remove the current content from its parent if the parent is not the new content.
    //Otherwise the current content would be removed from the new content as well.
    if (nonNull(this.content) && !ComponentFunctions.isParentOf(content, this.content)) {
      this.content.removeFromParent();
    }
    this.content = content;
    this.content.addClassName("toast-content");
    refresh();
    return this;
  }

  private void refresh() {
    removeAll();
    add(this.content, this.closeButton);
  }

  /**
   * Can this toast be closed by the user?
   *
   * @return
   */
  public boolean isCloseable() {
    return closeable;
  }

  /**
   * Specifies whether the toast includes a button for the user to close the toast.
   *
   * @param closeable
   * @return
   */
  public Toast setCloseable(boolean closeable) {
    closeButton.setVisible(closeable);
    this.closeable = closeable;
    return this;
  }

  /**
   * Creates a matching toast content containing routing components with the correct css classes.
   *
   * @param content
   * @param routingComponent
   * @return
   */
  private static Component createRoutingContent(Component content, Component routingComponent) {
    var container = new Div();
    container.addClassName("routing-container");
    content.addClassName("routing-content");
    routingComponent.addClassName("routing-link");
    container.add(content, routingComponent);
    return container;
  }

  /**
   * Creates a routing component with correct css classes and layout.
   *
   * @param text
   * @param navigationTarget
   * @param routeParameters
   * @return
   */
  private Component createRoutingComponent(String text,
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
