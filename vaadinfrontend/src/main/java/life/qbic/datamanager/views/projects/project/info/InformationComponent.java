package life.qbic.datamanager.views.projects.project.info;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import java.io.Serial;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
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

  public static InformationComponent create(String title) {
    var component = new InformationComponent();
    component.setTitle(title);
    return component;
  }

  public void add(Entry entry) {
    this.content.add(styleRow(entry.label(), entry.content()));
  }

  public void clearContent() {
    this.content.removeAll();
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

  public void setTitle(String title) {
    this.title.setText(title);
  }

  public record Entry(String label, Component content) {

  }

}
