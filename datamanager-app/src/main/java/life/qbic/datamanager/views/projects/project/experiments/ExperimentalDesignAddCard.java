package life.qbic.datamanager.views.projects.project.experiments;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility.Background;
import com.vaadin.flow.theme.lumo.LumoUtility.Border;
import com.vaadin.flow.theme.lumo.LumoUtility.BorderColor;
import com.vaadin.flow.theme.lumo.LumoUtility.BorderRadius;
import com.vaadin.flow.theme.lumo.LumoUtility.BoxShadow;
import com.vaadin.flow.theme.lumo.LumoUtility.FontSize;
import com.vaadin.flow.theme.lumo.LumoUtility.IconSize;
import java.io.Serial;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalDesign;

/**
 * <b>Experimental Design Add Card</b>
 *
 * <p>A PageComponent based Component which is simliarly structured as the
 * {@link ExperimentalDesignCard}. However it's content is fixed and it's purpose is to allow for a
 * user to click on the card to create a new
 * {@link ExperimentalDesign}.
 */
public class ExperimentalDesignAddCard extends Composite<VerticalLayout> {

  @Serial
  private static final long serialVersionUID = 2082499691092642085L;
  private final VerticalLayout contentLayout = getContent();

  public ExperimentalDesignAddCard() {
    initCardLayout();
    setCardStyles();
  }

  private void initCardLayout() {
    Icon myIcon = VaadinIcon.PLUS.create();
    myIcon.setSize(IconSize.LARGE);
    Span text = new Span("Add Experiment");
    text.addClassName("font-bold");
    contentLayout.addClassName(FontSize.LARGE);
    contentLayout.add(myIcon, text);
  }

  private void setCardStyles() {
    contentLayout.setMaxHeight(100, Unit.PERCENTAGE);
    contentLayout.setMaxWidth(100, Unit.PERCENTAGE);
    contentLayout.setJustifyContentMode(JustifyContentMode.CENTER);
    contentLayout.setAlignItems(Alignment.CENTER);
    this.getStyle().set("cursor", "pointer");
    this.addClassNames(Background.BASE, Border.ALL, BorderColor.CONTRAST_10, BorderRadius.MEDIUM,
        BoxShadow.SMALL, FontSize.SMALL);
  }
}
