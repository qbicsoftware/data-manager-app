package life.qbic.datamanager.views.navigation;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.shared.HasSuffix;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.dom.Element;

public class OpenSideNavItem extends Span implements HasSuffix {
  private Element labelElement;
  //private Icon prefix;

  public OpenSideNavItem(String label) {
    this.setLabel(label);
    //this.addComponentAsFirst(prefix);
  }

  public void setLabel(String label) {
    if (label == null) {
      this.removeLabelElement();
    } else {
      if (this.labelElement == null) {
        this.labelElement = this.createAndAppendLabelElement();
      }

      this.labelElement.setText(label);
    }
  }

  private Element createAndAppendLabelElement() {
    Element element = Element.createText("");
    this.getElement().appendChild(new Element[]{element});
    return element;
  }

  private void removeLabelElement() {
    if (this.labelElement != null) {
      this.getElement().removeChild(new Element[]{this.labelElement});
      this.labelElement = null;
    }
  }

  public void addItem(SideNavItem... items) {
    assert items != null;

    SideNavItem[] var2 = items;
    int var3 = items.length;

    for(int var4 = 0; var4 < var3; ++var4) {
      SideNavItem item = var2[var4];
      this.setupSideNavItem(item);
      this.getElement().appendChild(new Element[]{item.getElement()});
    }
  }

  protected void setupSideNavItem(SideNavItem item) {
    item.getElement().setAttribute("slot", "children");
  }
}
