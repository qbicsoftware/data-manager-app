package life.qbic.datamanager.views.projects.project.experiments;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.Disclaimer;
import life.qbic.datamanager.views.projects.project.experiments.experiment.ExperimentDetailsComponent;
import life.qbic.datamanager.views.projects.project.experiments.experiment.ExperimentDetailsComponent.ExperimentNameChangedEvent;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Experiment Content component
 * <p>
 * The content component is a {@link Div} container, which is responsible for hosting the components
 * handling the content within the {@link ExperimentInformationMain}. It is intended to propagate
 * experiment information provided in the {@link ExperimentDetailsComponent} to the
 * {@link ExperimentInformationMain} and vice versa and can be easily extended with additional
 * components if necessary
 */

@SpringComponent
@UIScope
public class ExperimentContentComponent extends Div {

  @Serial
  private static final long serialVersionUID = 464171225772721108L;
  private final ExperimentDetailsComponent experimentDetailsComponent;
  private final Disclaimer noExperimentDisclaimer;

  public ExperimentContentComponent(
      @Autowired ExperimentDetailsComponent experimentDetailsComponent) {
    this.experimentDetailsComponent = experimentDetailsComponent;
    this.noExperimentDisclaimer = createNoExperimentDisclaimer();
  }

  private Disclaimer createNoExperimentDisclaimer() {
    var disclaimer = Disclaimer.createWithTitle("Add an experiment",
        "Get started by adding an experiment", "Add experiment");
    disclaimer.addDisclaimerConfirmedListener(confirmedEvent -> fireEvent(
        new AddExperimentClickEvent(this, confirmedEvent.isFromClient())));
    return disclaimer;
  }

  /**
   * Propagates the context to internal components.
   *
   * @param context the context in which the user is.
   */
  public void setContext(Context context) {
    context.experimentId().ifPresentOrElse(
        experimentId -> {
          experimentDetailsComponent.setContext(context);
          this.add(experimentDetailsComponent);
          this.remove(noExperimentDisclaimer);
        }, () -> {
          this.add(noExperimentDisclaimer);
          this.remove(experimentDetailsComponent);
        }
    );
  }

  /**
   * Propagates the listener which will retrieve notification if a an {@link Experiment} was edited
   * in the {@link ExperimentDetailsComponent} within this container
   */
  public void addExperimentNameChangedListener(
      ComponentEventListener<ExperimentNameChangedEvent> experimentEditListener) {
    experimentDetailsComponent.addExperimentNameChangedListener(experimentEditListener);
  }

  public void addExperimentAddButtonClickEventListener(
      ComponentEventListener<AddExperimentClickEvent> listener) {
    addListener(AddExperimentClickEvent.class, listener);
  }

  public static class AddExperimentClickEvent extends ComponentEvent<ExperimentContentComponent> {

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public AddExperimentClickEvent(ExperimentContentComponent source, boolean fromClient) {
      super(source, fromClient);
    }
  }

}
