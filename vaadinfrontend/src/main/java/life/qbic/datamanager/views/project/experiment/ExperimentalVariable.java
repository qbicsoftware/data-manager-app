package life.qbic.datamanager.views.project.experiment;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

/**
 * An experimental variable. Experimental variables are independent of each other. Their value is
 * altered to measure their effect on a dependent variable.
 *
 * @since 1.0.0
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
