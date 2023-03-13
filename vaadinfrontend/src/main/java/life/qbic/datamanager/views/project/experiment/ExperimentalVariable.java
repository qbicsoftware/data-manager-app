package life.qbic.datamanager.views.project.experiment;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class ExperimentalVariable extends Div {

  private final TextField variableName = new TextField();

  private final TextField unit = new TextField();

  private final TextArea levels = new TextArea();

  public ExperimentalVariable() {
    init();
  }

  private void init() {
    this.add(variableName, unit, levels);
  }

}
