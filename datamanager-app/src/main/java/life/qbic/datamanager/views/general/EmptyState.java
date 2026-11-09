package life.qbic.datamanager.views.general;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;

/**
 * <b>Empty State</b>
 *
 * <p>A bordered, centred placeholder for a section that currently holds no items. It states the
 * empty condition explicitly, explains what the item is for and offers a single call to action —
 * so a genuinely empty list never looks like a failed load or leftover markup.</p>
 *
 * <p>Not to be confused with {@link Disclaimer}, which is a centred, borderless prompt used for
 * consent/confirmation flows. Use an {@code EmptyState} for "there is nothing here yet" and a
 * {@code Disclaimer} for "please confirm or acknowledge this".</p>
 *
 * <p>The action is optional: pass an {@code actionLabel} only when the user can resolve the empty
 * state themselves. Leave it {@code null} (see {@link #EmptyState(VaadinIcon, String, String)})
 * when the item has to be created elsewhere or by somebody else, so the card never offers a dead
 * end.</p>
 *
 * <pre>{@code
 * var emptyState = new EmptyState(VaadinIcon.KEY, "No personal access tokens yet",
 *     "Tokens let scripts and notebooks authenticate against the Data Manager API.");
 * emptyState.addActionListener(event -> generateToken());
 * }</pre>
 *
 * @since 1.19.0
 */
public class EmptyState extends Div {

  private Button actionButton;

  /**
   * The listener to attach to the action button, kept rather than bound at registration time.
   *
   * <p>{@link #setContent} can state a second condition whose action label appears only then, so the
   * button may not exist when the caller registers. Storing the listener means callers register once,
   * in any order, and the wiring happens whenever a button comes into being -- which is what the grid
   * needed: it registers in its constructor, long before it knows whether the empty state will be a
   * filtered or an unfiltered one.</p>
   */
  private java.util.function.Consumer<com.vaadin.flow.component.ClickEvent<Button>> actionHandler;

  /** Whether the delegating click listener is already on {@link #actionButton}. */
  private boolean handlerAttached;

  /**
   * Creates an empty state without a call to action. Use this when the user cannot resolve the
   * empty state from here.
   *
   * @param icon  the icon shown above the title
   * @param title a short statement of what is empty, must not be {@code null}
   * @param text  an explanation of what the item is for and what happens next, must not be
   *              {@code null}
   */
  public EmptyState(VaadinIcon icon, String title, String text) {
    this(icon, title, text, null);
  }

  /**
   * Creates an empty state with an optional call to action.
   *
   * @param icon        the icon shown above the title
   * @param title       a short statement of what is empty, must not be {@code null}
   * @param text        an explanation of what the item is for and what happens next, must not be
   *                    {@code null}
   * @param actionLabel the label of the primary action, or {@code null} to omit the action
   */
  public EmptyState(VaadinIcon icon, String title, String text, String actionLabel) {
    requireNonNull(icon, "icon must not be null");
    requireNonNull(title, "title must not be null");
    requireNonNull(text, "text must not be null");
    addClassName("empty-state");

    // The card is a live region so the empty condition is announced when it appears (for example
    // after the last item is removed) instead of being silently inserted into the page.
    getElement().setAttribute("role", "status");
    getElement().setAttribute("aria-live", "polite");

    var iconElement = icon.create();
    iconElement.addClassName("empty-state__icon");

    var titleElement = new H3(title);
    titleElement.addClassName("empty-state__title");

    var textElement = new Span(text);
    textElement.addClassName("empty-state__text");

    add(iconElement, titleElement, textElement);

    actionButton = actionLabel == null ? null : new Button(actionLabel);
    if (actionButton != null) {
      actionButton.addClassName("primary");
      actionButton.addClassName("empty-state__action");
      attachHandler();
      add(actionButton);
    }
  }

  /**
   * Wires the stored handler to the current button, exactly once per button.
   *
   * <p>The listener is a delegating one: it reads {@link #actionHandler} on every click, so replacing
   * the handler needs no re-wiring and registering twice cannot double-fire. The guard is only there
   * because a button created by {@link #setContent} and one created by the constructor must not both
   * attach a delegate.</p>
   */
  private void attachHandler() {
    if (handlerAttached) {
      return;
    }
    actionButton.addClickListener(event -> {
      if (actionHandler != null) {
        actionHandler.accept(event);
      }
    });
    handlerAttached = true;
  }

  /**
   * Restates the placeholder.
   *
   * <p>For a surface whose empty condition changes while it stays on screen -- a grid whose search
   * goes from matching nothing to being cleared -- so one instance can state both conditions instead
   * of being rebuilt. The content is replaced in place, and the live region is unchanged, so the
   * second statement is announced exactly like the first.</p>
   *
   * <p>The action is removed and re-added as needed: passing {@code null} leaves a placeholder with no
   * call to action, which is the honest rendering when the reader cannot resolve the emptiness from
   * here.</p>
   *
   * @param icon        the icon shown above the title
   * @param title       a short statement of what is empty
   * @param text        an explanation of what the item is for and what happens next
   * @param actionLabel the label of the primary action, or {@code null} to omit it
   */
  public void setContent(VaadinIcon icon, String title, String text, String actionLabel) {
    requireNonNull(icon, "icon must not be null");
    requireNonNull(title, "title must not be null");
    requireNonNull(text, "text must not be null");
    var iconElement = icon.create();
    iconElement.addClassName("empty-state__icon");
    var titleElement = new H3(title);
    titleElement.addClassName("empty-state__title");
    var textElement = new Span(text);
    textElement.addClassName("empty-state__text");
    removeAll();
    add(iconElement, titleElement, textElement);
    if (actionLabel != null) {
      if (actionButton == null) {
        actionButton = new Button(actionLabel);
        actionButton.addClassName("primary");
        actionButton.addClassName("empty-state__action");
        // A button appearing for the first time -- the filtered condition after an unfiltered one --
        // still has to fire the handler the caller registered before it existed.
        handlerAttached = false;
        attachHandler();
      } else {
        // Relabelled, not replaced: a listener registered on the first statement must keep firing
        // after a restatement, which a fresh button would silently drop.
        actionButton.setText(actionLabel);
      }
      add(actionButton);
    }
  }

  /**
   * Registers a click listener on the call-to-action button.
   *
   * <p>Safe to call before an action exists: {@link #setContent} may introduce the button later, and
   * the listener is attached when it does. This is the difference between a placeholder that can be
   * restated and one that has to be rebuilt for every condition.</p>
   *
   * @param listener the listener invoked when the action button is clicked
   */
  public void addActionListener(ComponentEventListener<ClickEvent<Button>> listener) {
    requireNonNull(listener, "listener must not be null");
    this.actionHandler = event -> listener.onComponentEvent(event);
    if (actionButton != null) {
      attachHandler();
    }
  }
}
