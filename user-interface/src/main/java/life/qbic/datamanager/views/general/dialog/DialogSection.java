package life.qbic.datamanager.views.general.dialog;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class DialogSection extends Div {

  private final Div title;

  private final Div description;

  private final Div content;

  private DialogSection(String title, String description) {
    addClassName("dialog-section");
    this.title = new Div(title);
    this.title.addClassName("heading-4");
    this.description = new Div(description);
    this.description.addClassName("normal-body-text");
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

  public void content(Component content) {
    this.content.removeAll();
    this.content.add(content);
  }

}
