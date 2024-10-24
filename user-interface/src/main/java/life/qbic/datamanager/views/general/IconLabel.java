package life.qbic.datamanager.views.general.section;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
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
    addClassName("horizontal-list");
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
