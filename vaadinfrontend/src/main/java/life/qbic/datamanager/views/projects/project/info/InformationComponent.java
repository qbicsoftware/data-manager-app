package life.qbic.datamanager.views.projects.project.info;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
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
   * Creates an information component with a title
   *
   * @param title the title of the information component
   * @return a new instance of the component
   * @since 1.0.0
   */
  public static InformationComponent create(String title) {
    var component = new InformationComponent();
    component.setTitle(title);
    return component;
  }

  private static Div styleRow(String label, Component content) {
    var rowEntry = new Div();
    rowEntry.setClassName("info-entry");
    var rowLabel = new Div();
    rowLabel.setClassName("info-entry-label");
    rowLabel.add(label);
    var rowContent = new Div();
    rowContent.setClassName("info-entry-content");
    rowContent.add(content);
    rowEntry.add(rowLabel, rowContent);
    return rowEntry;
  }

  /**
   * Appends a new {@link Entry} with the default rendering of the information component.
   *
   * @param entry the entry to append, containing a label and content
   * @since 1.0.0
   */
  public void add(Entry entry) {
    this.content.add(styleRow(entry.label(), entry.content()));
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
  public void setTitle(String title) {
    this.title.setText(title);
  }

  /**
   * A row entry for the information component, contains a label and a Vaadin {@link Component},
   * that will be shown next to the label.
   *
   * @param label   a descriptive label about the content
   * @param content the content to display
   * @since 1.0.0
   */
  public record Entry(String label, Component content) {

  }

}
