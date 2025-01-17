package life.qbic.datamanager.views.projects.project.experiments;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.Collection;
import java.util.Objects;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.Disclaimer;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.projects.project.experiments.experiment.create.AddExperimentDialog;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Experiment list component
 * <p>
 * The list component is a {@link PageArea} component, which is responsible for showing the
 * {@link Experiment} information for all experiments in its {@link ListBox} within the currently
 * examined {@link Project}.
 * <p>
 * Additionally, it provides the possibility to create new experiments with its
 * {@link AddExperimentDialog} and enables the user to select an experiment of interest via
 * {@link AddExperimentDialog} and enables the user to select an experiment of interest via
 * clicking on the item within {@link ListBox} associated with the experiment.
 * <p>
 * Finally, it allows components to be informed about a new experiment creation or selection via the
 * {@link ExperimentSelectionEvent} event.
 */
@SpringComponent
@UIScope
public class ExperimentListComponent extends PageArea {

  @Serial
  private static final long serialVersionUID = -2196400941684042549L;
  private final transient ExperimentInformationService experimentInformationService;
  private final Disclaimer noExperimentDisclaimer;
  private final Div header = new Div();
  private final Div content = new Div();
  private final ListBox<Experiment> listBox = new ListBox<>();
  private Context context;

  public ExperimentListComponent(
      @Autowired ExperimentInformationService experimentInformationService) {
    Objects.requireNonNull(experimentInformationService);
    this.experimentInformationService = experimentInformationService;
    this.add(listBox);
    this.addClassName("experiment-list-component");
    listBox.addClassName("transparent-icons");
    layoutComponents();
    this.noExperimentDisclaimer = createNoExperimentDisclaimer();
  }

  public void setContext(Context context) {
    this.context = context;
    refresh();
  }

  private void layoutComponents() {
    initHeader();
    content.addClassName("content");
    add(content);
  }

  private void loadExperimentsForProject(ProjectId projectId) {
    Collection<Experiment> foundExperiments = experimentInformationService.findAllForProject(
        projectId);
    remove(header);
    remove(content);
    content.removeAll();
    if (foundExperiments.isEmpty()) {
      content.add(noExperimentDisclaimer);
    } else {
      addComponentAsFirst(header);
      setExperimentsInListBox(foundExperiments);
      addExperimentSelectionListener();
      content.add(listBox);
    }
    add(content);
  }

  private void setExperimentsInListBox(Collection<Experiment> experiments) {
    var dataView = listBox.setItems(experiments);
    dataView.setSortOrder(experiment -> experiment.getName().toLowerCase(),
        SortDirection.ASCENDING);
    listBox.setRenderer(new ComponentRenderer<>(this::generateExperimentListItem));
  }

  private void addExperimentSelectionListener() {
    listBox.addValueChangeListener(
        listBoxExperimentComponentValueChangeEvent -> {
          if (listBoxExperimentComponentValueChangeEvent.isFromClient()) {
            fireExperimentSelectionEvent(
                listBoxExperimentComponentValueChangeEvent.getValue().experimentId(),
                listBoxExperimentComponentValueChangeEvent.isFromClient());
          }
        });
  }

  private Span generateExperimentListItem(Experiment experiment) {
    Icon flaskIcon = new Icon(VaadinIcon.FLASK);
    flaskIcon.setClassName("primary");
    Span experimentListItem = new Span();
    experimentListItem.addClassName("experiment-list-item");
    experimentListItem.add(flaskIcon, new Span(experiment.getName()));
    return experimentListItem;
  }

  public void refresh() {
    loadExperimentsForProject(context.projectId()
        .orElseThrow(() -> new ApplicationException("no project id in context " + context)));
  }

  private void initHeader() {
    Div controls = new Div();
    controls.addClassName("controls");
    Button addButton = new Button("Add");
    addButton.addClickListener(
        event -> fireEvent(new AddExperimentClickEvent(this, event.isFromClient())));
    controls.add(addButton);
    Span title = new Span("Experiments");
    title.addClassName("title");
    header.add(title, controls);
    header.addClassName("header");
    addComponentAsFirst(header);
  }

  private Disclaimer createNoExperimentDisclaimer() {
    var disclaimer = Disclaimer.createWithTitle("Add an experiment",
        "Get started by adding an experiment", "Add experiment");
    disclaimer.addDisclaimerConfirmedListener(confirmedEvent -> fireEvent(
        new AddExperimentClickEvent(this, confirmedEvent.isFromClient())));
    return disclaimer;
  }


  public void addAddButtonListener(ComponentEventListener<AddExperimentClickEvent> listener) {
    addListener(AddExperimentClickEvent.class, listener);
  }

  /**
   * Adds the provided component listener for {@link ExperimentSelectionEvent}
   */
  public void addExperimentSelectionListener(
      ComponentEventListener<ExperimentSelectionEvent> experimentSelectionListener) {
    addListener(ExperimentSelectionEvent.class, experimentSelectionListener);
  }

  private void fireExperimentSelectionEvent(ExperimentId experimentId, boolean fromClient) {
    var experimentSelectionEvent = new ExperimentSelectionEvent(this, fromClient, experimentId);
    fireEvent(experimentSelectionEvent);
  }

  public static class AddExperimentClickEvent extends ComponentEvent<ExperimentListComponent> {

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public AddExperimentClickEvent(ExperimentListComponent source, boolean fromClient) {
      super(source, fromClient);
    }
  }

  public static class ExperimentSelectionEvent extends ComponentEvent<ExperimentListComponent> {

    private final ExperimentId experimentId;

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source       the source component
     * @param fromClient   <code>true</code> if the event originated from the client
     *                     side, <code>false</code> otherwise
     * @param experimentId the selected experiment
     */
    public ExperimentSelectionEvent(ExperimentListComponent source, boolean fromClient,
        ExperimentId experimentId) {
      super(source, fromClient);
      this.experimentId = experimentId;
    }

    public ExperimentId getExperimentId() {
      return experimentId;
    }
  }

}
