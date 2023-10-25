package life.qbic.datamanager.views.projects.project.experiments.experiment.components;

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
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Species;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Specimen;

/**
 * <b>Experiment Info Component</b>
 *
 * <p>A component that nicely renders experiment-related general information</p>
 *
 * @since 1.0.0
 */
public class ExperimentInfoComponent extends Card {

  public static final String CARD_SECTION_TITLE = "title";
  @Serial
  private static final long serialVersionUID = -4790635833822470484L;
  private final Collection<Species> species;
  private final Collection<Specimen> specimen;
  private final Collection<Analyte> analytes;
  private final Div controls = new Div();
  private final MenuBar menuBar;
  private final List<ComponentEventListener<ExperimentInfoEditEvent>> editListeners = new ArrayList<>();
  private boolean controlsEnabled = true;

  private ExperimentInfoComponent(Collection<Species> species, Collection<Specimen> specimen,
      Collection<Analyte> analytes) {
    super();
    this.species = species;
    this.specimen = specimen;
    this.analytes = analytes;
    this.menuBar = createMenuBar();
    layoutComponent();
  }

  public static ExperimentInfoComponent create(Collection<Species> species,
      Collection<Specimen> specimen, Collection<Analyte> analytes) {
    Objects.requireNonNull(species);
    Objects.requireNonNull(specimen);
    Objects.requireNonNull(analytes);
    return new ExperimentInfoComponent(species, specimen, analytes);
  }

  private MenuBar createMenuBar() {
    MenuBar menu = new MenuBar();
    menu.addThemeVariants(MenuBarVariant.LUMO_TERTIARY);
    MenuItem menuItem = menu.addItem("•••");
    SubMenu subMenu = menuItem.getSubMenu();
    subMenu.addItem("Edit", event -> fireEditEvent());
    return menu;
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
    span.addClassName(CARD_SECTION_TITLE);
    var speciesList = speciesList();
    content.add(span, speciesList);

    Span specimenTitle = new Span("Specimen");
    specimenTitle.addClassName(CARD_SECTION_TITLE);
    var specimenList = specimenList();
    content.add(specimenTitle, specimenList);

    Span analytesTitle = new Span("Analytes");
    analytesTitle.addClassName(CARD_SECTION_TITLE);
    var analytesList = analytesList();
    content.add(analytesTitle, analytesList);
    content.addClassName("content");

    add(content);
    addClassName("experiment-info");
  }

  private void fireEditEvent() {
    var event = new ExperimentInfoEditEvent(this, true);
    editListeners.forEach(listener -> listener.onComponentEvent(event));
  }

  private UnorderedList speciesList() {
    return new UnorderedList(
        this.species.stream().map(thisSpecies -> new ListItem(thisSpecies.value()))
            .toArray(ListItem[]::new));
  }

  private UnorderedList specimenList() {
    return new UnorderedList(
        this.specimen.stream().map(thisSpecimen -> new ListItem(thisSpecimen.value()))
            .toArray(ListItem[]::new));
  }

  private UnorderedList analytesList() {
    return new UnorderedList(
        this.analytes.stream().map(thisAnalyte -> new ListItem(thisAnalyte.value()))
            .toArray(ListItem[]::new));
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
