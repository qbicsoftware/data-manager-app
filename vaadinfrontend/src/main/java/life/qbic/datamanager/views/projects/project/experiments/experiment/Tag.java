package life.qbic.datamanager.views.projects.project.experiments.experiment;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.theme.lumo.LumoUtility.FontSize;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public class Tag extends Span {

  public Tag(String text) {
    super(text);
    getElement().getThemeList().add("badge");
    getElement().getThemeList().add(FontSize.SMALL);
    getStyle().set("white-space", "nowrap");
    getStyle().set("margin", "3px 5px");
  }
}
