package life.qbic.datamanager.views.project.view.components;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import life.qbic.datamanager.views.layouts.CardLayout;
import life.qbic.datamanager.views.project.experiment.ExperimentCreationDialog;

/**
 * <class short description - One Line!>
 * <p>
 * <More detailed description - When to use, what it solves, etc.>
 *
 * @since <version tag>
 */
@SpringComponent
@UIScope
public class ExperimentalDesignDetailComponent extends Composite<CardLayout> {

  @Serial
  private static final long serialVersionUID = -2255999216830849632L;
  private static final String TITLE = "Experimental Design";
  private final Button createDesignButton = new Button("Add");
  private final ExperimentCreationDialog experimentCreationDialog = new ExperimentCreationDialog();
  private final transient Handler handler;

  public ExperimentalDesignDetailComponent() {
    this.handler = new Handler();
  }

  /**
   * Component logic for the {@link ExperimentalDesignDetailComponent}
   *
   * @since 1.0.0
   */
  private final class Handler {

    public Handler() {
    }
  }
}
