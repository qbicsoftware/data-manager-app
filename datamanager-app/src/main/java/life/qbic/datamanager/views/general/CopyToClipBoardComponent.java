package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.theme.lumo.LumoUtility.IconSize;
import java.io.Serial;

/**
 * Copy to Clipboard Component
 * <p>
 * Customizable component based on {@link Span} which enables the user to copy the contained string
 * into the clipboard. Once the user clicks on the copy icon, the icon is switched for
 * {@code CONFIRMATION_MILLIS} to a check mark and then back. The switch to the success icon can be
 * listened to via the {@link SwitchToSuccessFullCopyIconEvent}.
 *
 * <h3>The confirmation lives in the browser, deliberately</h3>
 * <p>It used to be server state: the click listener hid the copy icon, showed the check, and
 * restored the copy icon from a {@code CompletableFuture} + {@link UI#accessLater}. That is a trap —
 * <b>without push, {@code accessLater} runs on the next request</b>, so the check mark stayed on
 * screen and the value looked "already copied": a second copy appeared impossible. It was reported
 * exactly like that, from the setup key of the authenticator enrolment.</p>
 *
 * <p>So the transient state is an attribute: {@code frontend/javascript/copyfeedback.js} sets
 * {@code data-copied} on this component for a moment after a click and clears it again, and
 * {@code icon.css} swaps the two icons. Nothing on the server has to be undone, so nothing can get
 * stuck, and the feedback appears immediately instead of after a round trip.</p>
 *
 * <p>The copy itself stays on the proven path ({@code window.copyToClipboard}, shared with
 * {@link CopyToClipboardButton}) because the clipboard API and its Safari/legacy fallbacks belong in
 * one place.</p>
 */
@JsModule("./javascript/copytoclipboard.js")
@JsModule("./javascript/copyfeedback.js")
public class CopyToClipBoardComponent extends Span {

  private final Icon copyIcon;
  private final Icon copySuccessIcon;
  private String textToBeCopied = "";

  public CopyToClipBoardComponent() {
    addClassName("copy-to-clipboard");
    // Marks this element for the delegated click listener in copyfeedback.js, which owns the
    // "copied" state. See the class documentation for why it is not server state.
    getElement().setAttribute("data-copy-feedback", "");

    copyIcon = VaadinIcon.COPY_O.create();
    copyIcon.addClassNames("clickable", "copy-icon");
    copySuccessIcon = VaadinIcon.CHECK.create();
    copySuccessIcon.addClassName("copy-icon-success");
    add(copyIcon, copySuccessIcon);
    addClickListener(this::handleCopyClicked);
  }

  public CopyToClipBoardComponent(String textToBeCopied) {
    this();
    setCopyText(textToBeCopied);
  }

  public void setCopyText(String textToBeCopied) {
    this.textToBeCopied = textToBeCopied;
  }

  public void setIconSize(String size) {
    copyIcon.setSize(size);
    copySuccessIcon.setSize(size);
  }

  private void handleCopyClicked(ComponentEvent<Span> componentEvent) {
    fireEvent(new SwitchToSuccessFullCopyIconEvent(this, componentEvent.isFromClient()));
    UI.getCurrent().getPage().executeJs("window.copyToClipboard($0)", textToBeCopied);
  }

  /**
   * Register an {@link ComponentEventListener} that will get informed with a
   * {@link SwitchToSuccessFullCopyIconEvent}, as soon as the user has copied the value.
   *
   * <p>Note what this event does <em>not</em> promise: it fires when the copy is requested, not when
   * the clipboard write has succeeded (the browser gives no reliable answer for that), and there is
   * no matching "switched back" event any more — that transient state belongs to the browser and
   * never reaches the server. A caller that needs the confirmation to end can style it with
   * {@code :has([data-copied])} instead of keeping a timer.</p>
   *
   * @param switchToSuccessFullCopyIconEventListener a listener on the switch to the success icon
   */
  public void addSwitchToSuccessfulCopyIconListener(
      ComponentEventListener<SwitchToSuccessFullCopyIconEvent> switchToSuccessFullCopyIconEventListener) {
    addListener(SwitchToSuccessFullCopyIconEvent.class, switchToSuccessFullCopyIconEventListener);
  }

  /**
   * <b>Switch to successful Copy Icon Event</b>
   *
   * <p>Indicates that the copy was requested and the icon switched to its successful state.</p>
   */
  public static class SwitchToSuccessFullCopyIconEvent extends
      ComponentEvent<CopyToClipBoardComponent> {

    @Serial
    private static final long serialVersionUID = 801480218486067833L;

    public SwitchToSuccessFullCopyIconEvent(CopyToClipBoardComponent source, boolean fromClient) {
      super(source, fromClient);
    }
  }
}
