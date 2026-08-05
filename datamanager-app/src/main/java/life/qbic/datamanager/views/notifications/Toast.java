package life.qbic.datamanager.views.notifications;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.BeforeLeaveListener;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.shared.Registration;
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
  private Component levelIcon;
  private Component actionButton;
  private ProgressBar progressBar;
  private String title;
  private String subtext;

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

    closeButton = createCloseButton();
    closeOnNavigation(DEFAULT_CLOSE_ON_NAVIGATION);
  }

  private Button createCloseButton() {
    var icon = new Icon();
    icon.getElement().setAttribute("icon", "vaadin:close-small");
    icon.addClassName("close-icon");
    var button = new Button(icon);
    button.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    button.addClassName("close-button");
    button.addClickListener(it -> close());
    return button;
  }

  /**
   * Sets an icon for the toast based on the notification level. Icons are 28x28px circular elements
   * with a colored background and a white Lumo symbol centered inside.
   *
   * @param level the notification level determining the icon color
   * @return the modified toast
   */
  Toast withLevelIcon(NotificationLevel level) {
    Component icon = getInfoForLevel(level);

    Div iconContainer = new Div(icon);
    iconContainer.addClassName("toast-icon");
    iconContainer.addClassName(getIconLevelClass(level));
    iconContainer.getElement().setAttribute("aria-label",
        "Notification status indicator");

    this.levelIcon = iconContainer;
    refresh();
    return this;
  }

  private Component getInfoForLevel(NotificationLevel level) {
    return switch (level) {
      case SUCCESS -> {
        var icon = new Icon();
        icon.getElement().setAttribute("icon", "vaadin:check");
        yield icon;
      }
      case ERROR, WARNING -> {
        var icon = new Icon();
        icon.getElement().setAttribute("icon", "vaadin:close");
        yield icon;
      }
      case INFO -> {
        var icon = new Icon();
        icon.getElement().setAttribute("icon", "vaadin:info");
        yield icon;
      }
    };
  }

  private String getIconLevelClass(NotificationLevel level) {
    return switch (level) {
      case SUCCESS -> "toast-icon-success";
      case ERROR, WARNING -> "toast-icon-error";
      case INFO -> "toast-icon-info";
    };
  }

  /**
   * Sets a title for the toast (used in progress/indeterminate toasts).
   * Title is displayed in 16px bold text above the main content.
   * Supports HTML markup.
   *
   * @param title the title text (may contain HTML markup)
   * @return the modified toast
   */
  Toast withTitle(String title) {
    requireNonNull(title, "title must not be null");
    this.title = title;
    if (progressBar != null) {
      refresh();
    }
    return this;
  }

  /**
   * Sets subtext for the toast (used in progress/indeterminate toasts).
   * Subtext is displayed in 16px regular text below the progress bar.
   * Supports HTML markup.
   *
   * @param subtext the subtext (may contain HTML markup)
   * @return the modified toast
   */
  Toast withSubtext(String subtext) {
    requireNonNull(subtext, "subtext must not be null");
    this.subtext = subtext;
    if (progressBar != null) {
      refresh();
    }
    return this;
  }

  /**
   * Adds an action button to the toast (e.g., "Retry", "Try Again").
   * Action buttons are styled with the primary accent color (#66A8FF) and a subtle background.
   *
   * @param label the button label
   * @param listener the click listener to invoke when the action button is clicked
   * @return the modified toast
   */
  Toast withAction(String label, ComponentEventListener<ClickEvent<Button>> listener) {
    requireNonNull(label, "label must not be null");
    requireNonNull(listener, "listener must not be null");
    var button = new Button(label);
    button.addClassName("action-button");
    button.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    button.getElement().setAttribute("aria-label", label);
    this.actionButton = button;
    button.addClickListener(listener);
    // Auto-close toast on action click
    button.addClickListener(event -> close());
    refresh();
    return this;
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
   * Sets the content of the toast.
   * Content is displayed in with the toast's primary text color.
   *
   * @param content the content of the toast to set.
   * @return the modified toast
   */
  Toast withContent(Component content) {
    requireNonNull(content, "content must not be null");
    // Remove current content from parent if different
    if (nonNull(this.content) && !ComponentFunctions.isParentOf(content, this.content)) {
      this.content.removeFromParent();
    }
    this.content = content;
    this.content.addClassName("toast-content");
    refresh();
    return this;
  }

  /**
   * Adds a progress bar to the toast. When set, the toast enters progress mode:
   * the center slot renders a vertical layout (title, progress bar, subtext)
   * instead of the content component, and alignment switches to BASELINE.
   * <p>
   * Composable with {@link #withTitle(String)} and {@link #withSubtext(String)}
   * in any order.
   *
   * @param progressBar the progress bar component (should be indeterminate)
   * @return the modified toast
   */
  Toast withProgressBar(ProgressBar progressBar) {
    requireNonNull(progressBar, "progressBar must not be null");
    this.progressBar = progressBar;
    refresh();
    return this;
  }

  /**
   * Builds the vertical layout used in progress mode: title, progress bar, subtext.
   */
  private Component buildProgressLayout() {
    var barContainer = new Div(progressBar);
    barContainer.addClassName("progress-bar-container");

    var verticalLayout = new VerticalLayout();
    verticalLayout.addClassName("progress-vertical");
    verticalLayout.setSpacing(false);
    verticalLayout.setPadding(false);

    if (title != null) {
      var titleElement = new Html("<span class=\"toast-title\">" + title + "</span>");
      verticalLayout.add(titleElement);
    }

    verticalLayout.add(barContainer);

    if (subtext != null) {
      var subtextElement = new Html("<span class=\"toast-subtext\">" + subtext + "</span>");
      verticalLayout.add(subtextElement);
    }

    return verticalLayout;
  }

  /**
   * Refreshes the toast content and layout.
   * <p>
   * In progress mode (progress bar set), the center slot is a vertical layout
   * built from title + progress bar + subtext, aligned at {@code BASELINE}.
   * Otherwise the center slot is {@link #content}, aligned at {@code CENTER}.
   * <p>
   * Builds: HorizontalLayout(icon?, center, actionButton?, closeButton)
   */
  private void refresh() {
    removeAll();
    var layout = new HorizontalLayout();
    layout.addClassName("toast-layout");

    boolean isProgress = (progressBar != null);
    layout.setAlignItems(isProgress ? Alignment.BASELINE : Alignment.CENTER);

    // Add level icon if present (stored separately from content)
    if (levelIcon != null) {
      layout.add(levelIcon);
    }

    if (isProgress) {
      layout.add(buildProgressLayout());
    } else if (this.content != null) {
      layout.add(this.content);
    }

    if (actionButton != null) {
      layout.add(actionButton);
    }
    layout.add(closeButton);
    layout.setSpacing(true);
    layout.setPadding(false);
    super.add(layout);
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
   * Expands the toast and includes a link to a specific route target.
   *
   * @param linkText         The text of the link shown to the user
   * @param navigationTarget the target of the navigation
   * @param routeParameters  the parameters used for navigation
   * @return
   */
  Toast withRouting(String linkText, Class<? extends Component> navigationTarget,
      RouteParameters routeParameters) {
    var routingContent = createRoutingContent(this.content, createRoutingComponent(
        linkText, navigationTarget, routeParameters));
    this.content.removeFromParent();
    this.content = routingContent;
    refresh();
    return this;
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
