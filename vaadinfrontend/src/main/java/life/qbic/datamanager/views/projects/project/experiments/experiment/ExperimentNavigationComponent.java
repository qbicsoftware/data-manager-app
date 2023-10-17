package life.qbic.datamanager.views.projects.project.experiments.experiment;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabVariant;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.tabs.TabsVariant;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import life.qbic.datamanager.views.projects.overview.ProjectOverviewPage;
import life.qbic.datamanager.views.projects.overview.components.ProjectAddSubmitEvent;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentInformationMain;
import life.qbic.datamanager.views.projects.project.samples.SampleInformationMain;

/**
 * <class short description - One Line!>
 * <p>
 * <More detailed description - When to use, what it solves, etc.>
 *
 * @since <version tag>
 */
public class ExperimentNavigationComponent extends Div {

  Tabs experimentNavigationTabs = new Tabs();
  RoutingTab designExperiment = new RoutingTab(VaadinIcon.USER.create(),
      new Span("Design Experiment"),
      ExperimentInformationMain.class);
  RoutingTab registerSampleBatch = new RoutingTab(VaadinIcon.USER.create(),
      new Span("Register Sample Batch"), SampleInformationMain.class);
  RoutingTab viewMeasurements = new RoutingTab(VaadinIcon.USER.create(),
      new Span("View Measurements"), ProjectOverviewPage.class);
  RoutingTab downloadAnalysis = new RoutingTab(VaadinIcon.USER.create(),
      new Span("Download Analysis"), ProjectOverviewPage.class);

  private final List<ComponentEventListener<ExperimentNavigationTriggeredEvent>> experimentNavigationTriggeredListeners = new ArrayList<>();

  public ExperimentNavigationComponent() {
    initializeSteps();
    disableUnusedSteps();
    addListeners();
    styleComponent();
  }

  private void initializeSteps() {
    experimentNavigationTabs.addThemeVariants(TabsVariant.LUMO_EQUAL_WIDTH_TABS);
    experimentNavigationTabs.add(designExperiment, createArrowTab(), registerSampleBatch,
        createArrowTab(),
        viewMeasurements, createArrowTab(), downloadAnalysis);
    experimentNavigationTabs.setWidthFull();
    add(experimentNavigationTabs);
  }

  private void disableUnusedSteps() {
    viewMeasurements.setEnabled(false);
    downloadAnalysis.setEnabled(false);
  }

  //ToDo Remove me
  private void styleComponent() {
    setWidthFull();
    experimentNavigationTabs.setWidthFull();
  }

  private void addListeners() {
    experimentNavigationTabs.addSelectedChangeListener(
        event -> fireExperimentNavigationTriggeredEvent(
            (RoutingTab<Component>) event.getSelectedTab(), event.isFromClient()));
  }

  /**
   * Add a listener that is called, when a new {@link ProjectAddSubmitEvent event} is emitted.
   *
   * @param listener a listener that should be called
   * @since 1.0.0
   */
  public void addListener(ComponentEventListener<ExperimentNavigationTriggeredEvent> listener) {
    Objects.requireNonNull(listener);
    experimentNavigationTriggeredListeners.add(listener);
  }

  private void fireExperimentNavigationTriggeredEvent(RoutingTab<Component> source,
      boolean fromClient) {
    var experimentNavigationTriggeredEvent = new ExperimentNavigationTriggeredEvent(source,
        fromClient);
    experimentNavigationTriggeredListeners.forEach(
        listener -> listener.onComponentEvent(experimentNavigationTriggeredEvent));
  }

  private Tab createArrowTab() {
    Tab arrow = new Tab(VaadinIcon.ARROW_RIGHT.create());
    arrow.setEnabled(false);
    return arrow;
  }

  public static class RoutingTab<T> extends Tab {

    private final Class<T> navigationTarget;

    public RoutingTab(Component icon, Component label, Class<T> navigationTarget) {
      this.add(icon);
      this.add(label);
      this.navigationTarget = navigationTarget;
      this.addThemeVariants(TabVariant.LUMO_ICON_ON_TOP);
    }

    public Class<T> navigationTarget() {
      return navigationTarget;
    }
  }

  public static class ExperimentNavigationTriggeredEvent extends
      ComponentEvent<RoutingTab<Component>> {

    @Serial
    private static final long serialVersionUID = -4190190706670253753L;

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public ExperimentNavigationTriggeredEvent(RoutingTab<Component> source,
        boolean fromClient) {
      super(source, fromClient);
    }
  }

}
