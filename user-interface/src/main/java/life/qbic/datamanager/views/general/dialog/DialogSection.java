package life.qbic.datamanager.views.general.dialog;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.charts.model.Dial;
import com.vaadin.flow.component.html.Div;

/**
 * <b>Dialog Section</b>
 * <p>
 * Pre-formatted component used in a dialog body. Provides a title, short description and
 * the content itself.
 *
 * @since 1.7.0
 */
public class DialogSection extends Div {

  private final Div title;

  private final Div description;

  private final Div content;

  private DialogSection(String title, String description) {
    addClassName("dialog-section");
    this.title = new Div(title);
    this.title.addClassNames("heading-4", "text-margin-bottom-03");
    this.description = new Div(description);
    this.description.addClassNames("normal-body-text", "text-margin-bottom-03");
    this.content = new Div();
    this.content.addClassName("dialog-content");

    add(this.title, this.description, this.content);
  }

  public static DialogSection with(String title, String description) {
    return new DialogSection(title, description);
  }

  public static DialogSection with(String title, String description, Component content) {
    var dialogSection = new DialogSection(title, description);
    dialogSection.content(content);
    return dialogSection;
  }

  public static DialogSection with(String title, Component content) {
    var dialogSection = new DialogSection(title, "");
    dialogSection.content(content);
    dialogSection.remove(dialogSection.description);
    return dialogSection;
  }

  private void hideDescription() {
    description.setVisible(false);
  }

  public void content(Component content) {
    this.content.removeAll();
    this.content.add(content);
  }

}
