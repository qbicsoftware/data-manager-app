package life.qbic.datamanager.views.projects.project.experiments.experiment.component;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import life.qbic.datamanager.views.general.Card;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Species;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Specimen;

/**
 * <b>Experiment Info Component</b>
 *
 * <p>A component that nicely renders experiment-related general information</p>
 *
 * @since 1.0.0
 */
public class ExperimentInfoComponent extends Card {

  private boolean controlsEnabled = true;
  @Serial
  private static final long serialVersionUID = -4790635833822470484L;
  private final Collection<Species> species;
  private final Collection<Specimen> specimen;
  private final Collection<Analyte> analytes;
  private final Div controls = new Div();
  private final MenuBar menuBar;
  private final List<ComponentEventListener<ExperimentInfoEditEvent>> editListeners = new ArrayList<>();

  private ExperimentInfoComponent(Collection<Species> species, Collection<Specimen> specimen,
      Collection<Analyte> analytes) {
    super();
    this.species = species;
    this.specimen = specimen;
    this.analytes = analytes;
    this.menuBar = createMenuBar();
    layoutComponent();
  }

  private MenuBar createMenuBar() {
    MenuBar menuBar = new MenuBar();
    menuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY);
    MenuItem menuItem = menuBar.addItem("•••");
    SubMenu subMenu = menuItem.getSubMenu();
    subMenu.addItem("Edit", event -> {
      fireEditEvent();
    });
    return menuBar;
  }

  private void layoutComponent() {
    Div cardHeader = new Div();
    cardHeader.addClassName("header");

    controls.addClassName("controls");
    controls.add(menuBar);

    cardHeader.add(new Span("Fact Sheet"));
    cardHeader.add(controls);
    this.add(cardHeader);

    Div content = new Div();
    Span span = new Span("Species");
    span.addClassName("title");
    var species = speciesList();
    content.add(span, species);

    Span specimenTitle = new Span("Specimen");
    specimenTitle.addClassName("title");
    var specimen = specimenList();
    content.add(specimenTitle, specimen);

    Span analytesTitle = new Span("Analytes");
    analytesTitle.addClassName("title");
    var analytes = analytesList();
    content.add(analytesTitle, analytes);
    content.addClassName("content");

    add(content);
    addClassName("experiment-info-card");
  }

  private void fireEditEvent() {
    var event = new ExperimentInfoEditEvent(this, true);
    editListeners.forEach(listener -> listener.onComponentEvent(event));
  }

  private UnorderedList speciesList() {
    return new UnorderedList(
        this.species.stream().map(thisSpecies -> new ListItem(thisSpecies.value())).toList()
            .toArray(ListItem[]::new));
  }

  private UnorderedList specimenList() {
    return new UnorderedList(
        this.specimen.stream().map(thisSpecimen -> new ListItem(thisSpecimen.value())).toList()
            .toArray(ListItem[]::new));
  }

  private UnorderedList analytesList() {
    return new UnorderedList(
        this.analytes.stream().map(thisAnalyte -> new ListItem(thisAnalyte.value())).toList()
            .toArray(ListItem[]::new));
  }

  public static ExperimentInfoComponent create(Collection<Species> species,
      Collection<Specimen> specimen, Collection<Analyte> analytes) {
    Objects.requireNonNull(species);
    Objects.requireNonNull(specimen);
    Objects.requireNonNull(analytes);
    return new ExperimentInfoComponent(species, specimen, analytes);
  }

  /**
   * Register an {@link ComponentEventListener} that will get informed with a
   * {@link ExperimentInfoEditEvent}, as soon as a user wants to edit experiment information.
   *
   * @param listener A listener for edit events
   * @since 1.0.0
   */
  public void subscribeToEditEvent(ComponentEventListener<ExperimentInfoEditEvent> listener) {
    this.editListeners.add(listener);
  }

  /**
   * Adds and displays the control menu to the component
   *
   * @since 1.0.0
   */
  public void showMenu() {
    if (!controlsEnabled) {
      addControlMenu();
      controlsEnabled = true;
    }
  }

  private void addControlMenu() {
    controls.add(menuBar);
  }

  /**
   * Removes the control menu from the component.
   *
   * @since 1.0.0
   */
  public void hideMenu() {
    if (controlsEnabled) {
      removeControlMenu();
      controlsEnabled = false;
    }
  }

  private void removeControlMenu() {
    controls.remove(menuBar);
  }
}
