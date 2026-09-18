package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

/**
 * <b>Context Note</b>
 *
 * <p>A subtle, italicised note that provides contextual information to the user
 * — e.g. security reassurances, usage hints, or policy reminders. It is visually
 * distinct from normal description text (italic, slightly smaller, optional icon)
 * without being distracting.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * // Plain text note
 * add(new ContextNote("Tokens are encrypted at rest and never shared."));
 *
 * // With an icon for extra context
 * add(new ContextNote("Stored encrypted.", VaadinIcon.LOCK.create()));
 * }</pre>
 *
 * @since 1.12.0
 */
public class ContextNote extends Div {

  /**
   * Creates a context note with text only.
   *
   * @param text the note text, must not be {@code null}
   */
  public ContextNote(String text) {
    this(text, null);
  }

  /**
   * Creates a context note with an optional leading icon.
   *
   * @param text the note text, must not be {@code null}
   * @param icon an optional icon (e.g. {@code VaadinIcon.LOCK.create()}), may be {@code null}
   */
  public ContextNote(String text, Icon icon) {
    addClassName("context-note");

    if (icon != null) {
      icon.addClassName("context-note__icon");
      add(icon);
    }

    var textSpan = new Span(text);
    textSpan.addClassName("context-note__text");
    add(textSpan);
  }
}
