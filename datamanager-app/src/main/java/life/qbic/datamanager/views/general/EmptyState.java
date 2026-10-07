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
 * @since 1.13.0
 */
public class EmptyState extends Div {

  private final Button actionButton;

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
      add(actionButton);
    }
  }

  /**
   * Registers a click listener on the call-to-action button. Only call this when the empty state
   * was created with an action label.
   *
   * @param listener the listener invoked when the action button is clicked
   */
  public void addActionListener(ComponentEventListener<ClickEvent<Button>> listener) {
    requireNonNull(actionButton,
        "This empty state has no action button; create it with an action label first")
        .addClickListener(requireNonNull(listener, "listener must not be null"));
  }
}
