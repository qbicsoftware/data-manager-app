package life.qbic.datamanager.views.general.icon;

import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

/**
 * <b>Icon Factory</b>
 *
 * <p>Convenience icon factory for re-used icons following the iconography of the application's
 * style guide</p>
 *
 * @since 1.7.0
 */
public class IconFactory {

  private IconFactory() {}

  public static Icon warningIcon() {
    var icon = VaadinIcon.EXCLAMATION_CIRCLE.create();
    icon.addClassNames("icon-color-warning", "icon-size-m");
    return icon;
  }

}
