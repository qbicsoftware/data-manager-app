package life.qbic.datamanager.views.projects.project.samples;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.shared.Registration;
import java.io.Serial;

/**
 * <b>Sample Prerequisite Gate</b>
 *
 * <p>Shown on the experiment-scoped samples page while the experiment has no experimental group.
 * Every registered sample is assigned to an experimental group, so without at least one group there
 * is nothing the user can register. The gate states that prerequisite and offers the single action
 * that removes it: defining the experimental design.</p>
 *
 * <p>The gate is a prerequisite, not an error. A missing prerequisite is a normal step in the
 * project lifecycle, so it renders neutrally (dashed border, muted surface, lock glyph) rather than
 * in the warning palette. It is also never dismissible, because it is the only path forward from
 * this state -- unlike the {@link RegistrationMilestoneBand}, which states an achievement and can
 * therefore be dismissed.</p>
 *
 * @since 1.20.0
 */
public class SamplePrerequisiteGate extends Div {

  @Serial
  private static final long serialVersionUID = 7710482819243144901L;

  private final Span evidence = new Span();
  private final Button defineDesign = new Button("Define experimental design",
      VaadinIcon.ARROW_RIGHT.create());
  public SamplePrerequisiteGate() {
    addClassName("sample-gate");
    setVisible(false);

    Icon lock = VaadinIcon.LOCK.create();
    lock.addClassName("sample-gate-icon");
    lock.getElement().setAttribute("aria-hidden", "true");

    H2 title = new H2("Define experimental design first");
    title.addClassName("sample-gate-title");

    Div heading = new Div(lock, title);
    heading.addClassName("sample-gate-heading");

    Span body = new Span("Every registered sample is assigned to an experimental group, so this "
        + "experiment needs at least one group before samples can be registered. Groups are built "
        + "from the variables: variables, then groups.");
    body.addClassName("sample-gate-body");

    evidence.addClassName("sample-gate-evidence");

    defineDesign.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    defineDesign.addClassName("sample-gate-action");
    defineDesign.addClickListener(
        event -> fireEvent(new DefineDesignRequested(this, event.isFromClient())));

    add(heading, body, evidence, defineDesign);
    update(0, 0);
  }

  /**
   * Sets the prerequisite evidence line, so the gate never states a bare "0" without saying what is
   * missing.
   *
   * @param groupCount    the number of experimental groups in the experiment
   * @param variableCount the number of experimental variables in the experiment
   */
  public void update(int groupCount, int variableCount) {
    evidence.setText(pluralise(groupCount, "experimental group") + " · "
        + pluralise(variableCount, "variable"));
  }

  private static String pluralise(int count, String noun) {
    return count == 1 ? "1 " + noun : "%d %ss".formatted(count, noun);
  }

  /**
   * Hides the gate's action. Used for read-only users, who cannot define the design themselves: the
   * prerequisite and its evidence still have to be stated, but offering a button that can only fail
   * would be a dead control.
   *
   * @param canDefineDesign whether the current user may change the experimental design
   */
  public void setActionVisible(boolean canDefineDesign) {
    defineDesign.setVisible(canDefineDesign);
  }

  public Registration addDefineDesignRequestedListener(
      ComponentEventListener<DefineDesignRequested> listener) {
    return addListener(DefineDesignRequested.class, listener);
  }

  /**
   * <b>Define Design Requested</b>
   *
   * <p>Indicates that the user wants to be routed to the experimental design surface, where the
   * missing prerequisite can be created.</p>
   */
  public static class DefineDesignRequested extends ComponentEvent<SamplePrerequisiteGate> {

    @Serial
    private static final long serialVersionUID = -3314165523095532191L;

    public DefineDesignRequested(SamplePrerequisiteGate source, boolean fromClient) {
      super(source, fromClient);
    }
  }
}
