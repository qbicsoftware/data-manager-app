package life.qbic.datamanager.views.projects.project.samples;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;
import java.io.Serial;
import life.qbic.datamanager.views.general.Tag;
import life.qbic.datamanager.views.general.Tag.TagColor;

/**
 * <b>Registration Milestone Band</b>
 *
 * <p>Shown on the experiment-scoped samples page in the one state the page historically hid
 * entirely: the experiment has at least one experimental group, so registration has just become
 * possible, but no sample has been registered yet. The band names that achievement ("Sample
 * registration is unlocked"), states the facts that unlocked it, and carries the page's single
 * primary action.</p>
 *
 * <p>Previously this state replaced the whole page with a centred prompt that hid the page title
 * and the experiment context, so the user could not see what they had just achieved. Keeping the
 * page and moving the invitation into a band makes the milestone visible without turning it into
 * furniture for returning users -- the band disappears as soon as a sample exists, and it can be
 * dismissed for the rest of the session.</p>
 *
 * @since 1.20.0
 */
public class RegistrationMilestoneBand extends Div {

  @Serial
  private static final long serialVersionUID = -4413009219883370651L;

  /**
   * Session attribute prefix for the per-experiment dismissal. Keyed by experiment, so dismissing
   * the notice for one experiment does not hide it for another.
   */
  private static final String DISMISS_KEY_PREFIX = "sampleMilestoneDismissed:";

  private final Span body = new Span();
  private String experimentId;

  public RegistrationMilestoneBand() {
    addClassName("sample-milestone");
    setVisible(false);

    Tag tag = new Tag("Unlocked");
    tag.setTagColor(TagColor.SUCCESS);
    tag.addClassName("sample-milestone-tag");

    H2 title = new H2("Sample registration is unlocked");
    title.addClassName("sample-milestone-title");

    Button dismiss = new Button(VaadinIcon.CLOSE.create());
    dismiss.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    dismiss.addClassName("sample-milestone-dismiss");
    dismiss.getElement().setAttribute("aria-label", "Dismiss registration notice");
    dismiss.addClickListener(event -> dismissForSession());

    Div header = new Div(tag, title, dismiss);
    header.addClassName("sample-milestone-header");

    body.addClassName("sample-milestone-body");

    Button register = new Button("Register samples", VaadinIcon.PLUS.create());
    register.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    register.addClassName("sample-milestone-action");
    register.addClickListener(
        event -> fireEvent(new RegisterRequested(this, event.isFromClient())));

    Button review = new Button("Review experimental design");
    review.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    review.addClassName("sample-milestone-review");
    review.addClickListener(
        event -> fireEvent(new ReviewDesignRequested(this, event.isFromClient())));

    Div actions = new Div(register, review);
    actions.addClassName("sample-milestone-actions");

    add(header, body, actions);
    // Starts unpopulated: an empty milestone must never render before setExperiment() has stated
    // the facts that unlocked it.
  }

  /**
   * Re-targets the band at an experiment and states the facts that unlocked registration.
   *
   * @param experimentId  the experiment the milestone belongs to, used to scope the dismissal
   * @param groupCount    the number of experimental groups in the experiment
   * @param variableCount the number of experimental variables in the experiment
   */
  public void setExperiment(String experimentId, int groupCount, int variableCount) {
    this.experimentId = experimentId;
    body.setText(("This experiment has %s and %s, so every registered sample can be assigned to a "
        + "condition. Register your first batch with the metadata template.")
        .formatted(pluralise(groupCount, "experimental group"),
            pluralise(variableCount, "variable")));
  }

  private static String pluralise(int count, String noun) {
    return count == 1 ? "1 " + noun : "%d %ss".formatted(count, noun);
  }

  private void dismissForSession() {
    VaadinSession session = VaadinSession.getCurrent();
    if (session != null && experimentId != null) {
      session.setAttribute(DISMISS_KEY_PREFIX + experimentId, Boolean.TRUE);
    }
    setVisible(false);
  }

  /**
   * @param experimentId the experiment to check
   * @return whether the user already dismissed the milestone for this experiment in this session
   */
  public static boolean isDismissed(String experimentId) {
    VaadinSession session = VaadinSession.getCurrent();
    return session != null && experimentId != null
        && Boolean.TRUE.equals(session.getAttribute(DISMISS_KEY_PREFIX + experimentId));
  }

  public Registration addRegisterListener(
      ComponentEventListener<RegisterRequested> listener) {
    return addListener(RegisterRequested.class, listener);
  }

  public Registration addReviewDesignListener(
      ComponentEventListener<ReviewDesignRequested> listener) {
    return addListener(ReviewDesignRequested.class, listener);
  }

  /**
   * <b>Register Requested</b>
   *
   * <p>Indicates that the user took the milestone's primary action and wants to register the first
   * batch of samples.</p>
   */
  public static class RegisterRequested extends ComponentEvent<RegistrationMilestoneBand> {

    @Serial
    private static final long serialVersionUID = 8104771933029418872L;

    public RegisterRequested(RegistrationMilestoneBand source, boolean fromClient) {
      super(source, fromClient);
    }
  }

  /**
   * <b>Review Design Requested</b>
   *
   * <p>Indicates that the user wants to inspect the experimental design that unlocked
   * registration.</p>
   */
  public static class ReviewDesignRequested extends ComponentEvent<RegistrationMilestoneBand> {

    @Serial
    private static final long serialVersionUID = -1102744128763920113L;

    public ReviewDesignRequested(RegistrationMilestoneBand source, boolean fromClient) {
      super(source, fromClient);
    }
  }
}
