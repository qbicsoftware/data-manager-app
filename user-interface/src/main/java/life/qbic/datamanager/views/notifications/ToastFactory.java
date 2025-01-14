package life.qbic.datamanager.views.notifications;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import java.time.Duration;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class ToastFactory {

  private ToastFactory() {}

  public static Toast asInfo(Component c, Duration d) {
    var t = new Toast(NotificationLevel.INFO).withContent(c).closeOnNavigation(false);
    t.setDuration(d);
    return t;
  }

  public static Toast asSuccess(String message, Duration d) {
    var t = new Toast(NotificationLevel.SUCCESS).withContent(new Div(message)).closeOnNavigation(false);
    t.setDuration(d);
    return t;
  }
}
