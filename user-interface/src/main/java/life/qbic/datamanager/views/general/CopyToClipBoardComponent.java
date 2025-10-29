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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Copy to Clipboard Component
 * <p>
 * Customizable component based on {@link Span} which enables the user to copy the contained string
 * into the clipboard. Once the user clicks on the copy icon, the icon is switched for specified
 * successTimer to an icon indicating the success of the copy process. The switch between the copy
 * icon and the success icon can be listened to via the {@link SwitchToCopyIconEvent} and
 * {@link SwitchToSuccessFullCopyIconEvent}
 */
@JsModule("./javascript/copytoclipboard.js")
public class CopyToClipBoardComponent extends Span {

  private final Icon copyIcon;
  private final Icon copySuccessIcon;
  private static final long SHOW_SUCCESS_TIME = 1000;
  private String textToBeCopied = "";

  public CopyToClipBoardComponent() {
    copyIcon = VaadinIcon.COPY_O.create();
    copyIcon.setSize(IconSize.SMALL);
    copyIcon.addClassNames("clickable", "copy-icon");
    copySuccessIcon = VaadinIcon.CHECK.create();
    copySuccessIcon.addClassName("copy-icon-success");
    add(copyIcon);
    add(copySuccessIcon);
    copySuccessIcon.setVisible(false);
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
    copyIcon.setVisible(false);
    copySuccessIcon.setVisible(true);
    // reset copy view after a specific time
    UI ui = UI.getCurrent();
    Executor delayedExecutor = CompletableFuture.delayedExecutor(SHOW_SUCCESS_TIME,
        TimeUnit.MILLISECONDS);
    CompletableFuture.runAsync(() -> ui.accessLater(() -> {
          copyIcon.setVisible(true);
          copySuccessIcon.setVisible(false);
        }, null), delayedExecutor)
        .thenRun(() -> fireEvent(new SwitchToCopyIconEvent(this, componentEvent.isFromClient())));
  }

  /**
   * Register an {@link ComponentEventListener} that will get informed with a
   * {@link SwitchToSuccessFullCopyIconEvent}, as soon as the icon switches its state upon user
   * click
   *
   * @param switchToSuccessFullCopyIconEventListener a listener on the switch from default copy icon
   *                                                 to successful copy icon
   */
  public void addSwitchToSuccessfulCopyIconListener(
      ComponentEventListener<SwitchToSuccessFullCopyIconEvent> switchToSuccessFullCopyIconEventListener) {
    addListener(SwitchToSuccessFullCopyIconEvent.class, switchToSuccessFullCopyIconEventListener);
  }

  /**
   * Register an {@link ComponentEventListener} that will get informed with a
   * {@link SwitchToCopyIconEvent}, as soon as the icon switches its state upon user click
   *
   * @param switchToCopyIconEventListener a listener on the switch from successful copy icon to
   *                                      default copy icon
   */
  public void addSwitchToCopyIconListener(
      ComponentEventListener<SwitchToCopyIconEvent> switchToCopyIconEventListener) {
    addListener(SwitchToCopyIconEvent.class, switchToCopyIconEventListener);
  }

  /**
   * <b>Switch to successful Copy Icon Event</b>
   *
   * <p>Indicates that the copy icon was switched to it's successful copy icon state</p>
   */
  public static class SwitchToSuccessFullCopyIconEvent extends
      ComponentEvent<CopyToClipBoardComponent> {

    @Serial
    private static final long serialVersionUID = 801480218486067833L;

    public SwitchToSuccessFullCopyIconEvent(CopyToClipBoardComponent source, boolean fromClient) {
      super(source, fromClient);
    }
  }

  /**
   * <b>Switch to copy icon event</b>
   *
   * <p>Indicates that the copy icon was switched to it's default copy icon state</p>
   */
  public static class SwitchToCopyIconEvent extends ComponentEvent<CopyToClipBoardComponent> {

    @Serial
    private static final long serialVersionUID = 1913842487251651139L;

    public SwitchToCopyIconEvent(CopyToClipBoardComponent source, boolean fromClient) {
      super(source, fromClient);
    }
  }
}
