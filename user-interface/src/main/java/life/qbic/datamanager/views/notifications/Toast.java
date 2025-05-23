package life.qbic.datamanager.views.notifications;

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
import java.util.Objects;
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

  static final boolean DEFAULT_CLOSE_ON_NAVIGATION = true;
  static final Duration DEFAULT_OPEN_DURATION = Duration.ofSeconds(5);
  private static final Position DEFAULT_POSITION = Position.BOTTOM_START;
  private final List<Registration> closeOnNavigationListeners = new ArrayList<>();
  private final Button closeButton;

  private Component content;


  Toast(NotificationLevel level) {
    super();
    addClassName("toast-notification");
    addClassName(switch (level) {
      case SUCCESS -> "success-toast";
      case INFO -> "info-toast";
      case WARNING, ERROR -> "error-toast";
    });

    setPosition(DEFAULT_POSITION);
    setDuration(DEFAULT_OPEN_DURATION);

    closeButton = buttonClosing(this);
    add(closeButton);
    closeOnNavigation(DEFAULT_CLOSE_ON_NAVIGATION);
  }

  private static Button buttonClosing(Toast toast) {
    var button = new Button(LumoIcon.CROSS.create());
    button.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    button.addClickListener(it -> toast.close());
    button.addClassName("close-button");
    return button;
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
   * Sets whether toasts are closed after navigation. By default, Toasts do not stay open after
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
   * Expands the toast and includes a link to a specific route target.
   *
   * @param linkText         The text of the link shown to the user
   * @param navigationTarget the target of the navigation
   * @param routeParameters  the parameters used for navigation
   * @return
   */
  Toast withRouting(String linkText, Class<? extends Component> navigationTarget,
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
  Toast withContent(Component content) {
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

  /**
   * Adds a component to the {@link Toast}. If content already exists, the existing component is
   * taken and wrapped together with the new component in a {@link Div}, without extra formatting.
   * <p>
   * If no content yet exists, the passed component is taken.
   *
   * @param component the component to add to the toast
   * @return the toast
   * @since 1.8.0
   */
  Toast add(Component component) {
    Objects.requireNonNull(component);
    if (nonNull(this.content)) {
      var copy = this.content;
      var newContent = new Div();
      newContent.add(copy, component);
      this.content = newContent;
    } else {
      this.content = component;
    }
    refresh();
    return this;
  }
}
