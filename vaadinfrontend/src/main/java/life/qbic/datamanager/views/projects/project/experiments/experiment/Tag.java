package life.qbic.datamanager.views.projects.project.experiments.experiment;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.theme.lumo.LumoUtility.Display;
import com.vaadin.flow.theme.lumo.LumoUtility.FontSize;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin.Right;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin.Top;
import com.vaadin.flow.theme.lumo.LumoUtility.Overflow;
import com.vaadin.flow.theme.lumo.LumoUtility.Padding;
import com.vaadin.flow.theme.lumo.LumoUtility.TextOverflow;
import com.vaadin.flow.theme.lumo.LumoUtility.Whitespace;

public class Tag extends Span {

  public Tag(String text) {
    super(text);
    this.addClassName(FontSize.SMALL);
    styleOverflow();
    styleSpacing();
    this.addClassName(Display.INLINE);
    getElement().getThemeList().add("badge");
  }

  private void styleOverflow() {
    this.addClassName(Whitespace.NOWRAP);
    this.addClassName(TextOverflow.ELLIPSIS);
    this.addClassName(Overflow.HIDDEN);
  }

  private void styleSpacing() {
    this.addClassName(Top.SMALL);
    this.addClassName(Right.XSMALL);
    this.addClassName(Padding.Top.SMALL);
    this.addClassName(Padding.Right.SMALL);
    getStyle().set("width", "inherit");
  }
}
