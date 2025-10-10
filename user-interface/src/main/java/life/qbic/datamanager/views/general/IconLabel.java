package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

/**
 * <b>Icon label</b>
 *
 * <p>Provides an icon and a label next to it.</p>
 *
 * @since 1.6.0
 */
public class IconLabel extends Div {

  private Icon icon;

  private String label;

  private final Icon toolTipIcon = VaadinIcon.INFO_CIRCLE.create();

  private String toolTipText;

  private boolean showTooltip;

  private String information;

  private IconLabel() {
    addClassName("icon-label");
    addClassNames("flex-horizontal", "width-full");
    this.showTooltip = false;
  }

  public IconLabel(Icon icon, String label) {
    this();
    this.icon = icon;
    this.label = label;
    rebuild();
  }

  public void setTooltipText(String toolTipText) {
    this.showTooltip = true;
    this.toolTipText = toolTipText;
    rebuild();
  }

  public void setInformation(String information) {
    this.information = information;
    rebuild();
  }

  private void rebuild() {
    removeAll();
    var iconLabelContainer = new Div();
    iconLabelContainer.setClassName("icon-label-container");

    if (icon != null) {
      iconLabelContainer.add(icon);
    }
    iconLabelContainer.add(new Span(label));

    if (showTooltip) {
      toolTipIcon.setTooltipText(toolTipText);
      iconLabelContainer.add(toolTipIcon);
    }
    add(iconLabelContainer);

    if (information != null) {
      add(new Span(information));
    }
  }


}
