package life.qbic.datamanager.views.settings;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;

/**
 * Settings Section
 * <p>
 * Blueprint layout for a single settings page section. It provides a max-width column with a
 * header (title, optional description and optional right-aligned actions) and a content area for
 * the section-specific components. All settings sections use this layout so they share a
 * consistent look and spacing.
 */
public class SettingsSection extends Div {

  private final Div header = new Div();
  private final Div contentArea = new Div();

  /**
   * Creates a settings section with a title only.
   *
   * @param title the section title, must not be {@code null}
   */
  public SettingsSection(String title) {
    this(title, (Component) null);
  }

  /**
   * Creates a settings section with a title and an optional description.
   *
   * @param title       the section title, must not be {@code null}
   * @param description the section description, may be {@code null}
   */
  public SettingsSection(String title, String description) {
    this(title, description == null ? null : new Paragraph(description));
  }

  /**
   * Creates a settings section with a title and a custom description component.
   *
   * @param title       the section title, must not be {@code null}
   * @param description the section description component, may be {@code null}
   */
  public SettingsSection(String title, Component description) {
    requireNonNull(title, "title must not be null");
    addClassName("settings-section");

    H2 titleElement = new H2(title);
    titleElement.addClassName("settings-section__title");

    Div titlesLayout = new Div();
    titlesLayout.addClassName("settings-section__titles");
    titlesLayout.add(titleElement);
    if (description != null) {
      description.addClassName("settings-section__description");
      titlesLayout.add(description);
    }

    header.addClassName("settings-section__header");
    header.add(titlesLayout);

    contentArea.addClassName("settings-section__content");

    add(header, contentArea);
  }

  /**
   * Adds an action (e.g. a button) to the right side of the section header.
   *
   * @param action the action component to add
   */
  public void addAction(Component action) {
    requireNonNull(action, "action must not be null");
    action.addClassName("settings-section__actions");
    header.add(action);
  }

  /**
   * Adds components to the content area of the section.
   *
   * @param components the components to add
   * @return this section for chaining
   */
  public SettingsSection addContent(Component... components) {
    contentArea.add(components);
    return this;
  }
}