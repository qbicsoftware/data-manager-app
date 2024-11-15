package life.qbic.datamanager.views.projects.project.info;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import java.io.Serial;

/**
 * <b>Information component</b>
 * <p>
 * Provides sections with a title and a form like display of row entries.
 *
 * @since 1.0.0
 */
public class InformationComponent extends Div {

  @Serial
  private static final long serialVersionUID = -926153591514170945L;
  private final Div title;
  private final Div content;

  private InformationComponent() {
    addClassName("info-section");
    title = new Div();
    title.setClassName("info-title");
    content = new Div();
    content.setClassName("info-content");
    add(title, content);
  }

  /**
   * Creates an information component with a title and optional tooltip
   *
   * @param title the title of the information component
   * @param tooltip a tooltip for the title to explain this information section (can be empty)
   * @return a new instance of the component
   * @since 1.0.0
   */
  public static InformationComponent create(String title, String tooltip) {
    var component = new InformationComponent();
    component.initializeTitle(title, tooltip);
    return component;
  }

  private void initializeTitle(String title, String tooltip) {
    setTitle(title);
    if(!tooltip.isBlank()) {
      this.title.getElement().setAttribute("title", tooltip);
    }
  }

  private static Div styleRow(String label, String tooltip, Component content) {
    var rowEntry = new Div();
    rowEntry.setClassName("info-entry");
    var rowLabel = new Span();
    rowLabel.setClassName("info-entry-label");
    rowLabel.add(label);
    var rowContent = new Div();
    rowContent.setClassName("info-entry-content");
    rowContent.add(content);
    rowEntry.add(rowLabel, rowContent);

    if(!tooltip.isBlank()) {
      Icon infoIcon = new Icon(VaadinIcon.INFO_CIRCLE);
      infoIcon.addClassName("info-icon");
      infoIcon.setTooltipText(tooltip);
      rowLabel.add(infoIcon);
    }
    return rowEntry;
  }

  /**
   * Appends a new {@link Entry} with the default rendering of the information component.
   *
   * @param entry the entry to append, containing a label, content and tooltip
   * @since 1.0.0
   */
  public void add(Entry entry) {
    this.content.add(styleRow(entry.label(), entry.tooltip(), entry.content()));
  }

  /**
   * Removes all entries appended in the information content.
   * <p>
   * <i>Note: will not impact the title</i>
   *
   * @since 1.0.0
   */
  public void clearContent() {
    this.content.removeAll();
  }

  /**
   * Sets and overrides an existing title of the information component
   *
   * @param title the new title
   */
  @Override
  public void setTitle(String title) {
    this.title.setText(title);
  }

  /**
   * A row entry for the information component, contains a label and a Vaadin {@link Component},
   * that will be shown next to the label, as well as an optional tooltip.
   *
   * @param label   a descriptive label about the content
   * @param tooltip the tooltip being shown when hovering over the label
   * @param content the content to display
   * @since 1.0.0
   */
  public record Entry(String label, String tooltip, Component content) {

  }

}
