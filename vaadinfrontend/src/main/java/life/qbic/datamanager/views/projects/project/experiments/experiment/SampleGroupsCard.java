package life.qbic.datamanager.views.projects.project.experiments.experiment;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.FlexLayout.FlexDirection;
import com.vaadin.flow.component.orderedlayout.FlexLayout.FlexWrap;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public class SampleGroupsCard extends VerticalLayout {

  public SampleGroupsCard() {
    FlexLayout container = new FlexLayout();
    container.setFlexDirection(FlexDirection.ROW);
    for (int i = 0; i < 2000; i++) {
      container.add(getSampleGroup());
    }
    container.setFlexWrap(FlexWrap.WRAP);
    add(container);
  }

  private static HorizontalLayout getSampleGroup() {
    Button thisIsSomeText = new Button("This is some text");
    HorizontalLayout horizontalLayout = new HorizontalLayout(thisIsSomeText);
    horizontalLayout.setSpacing(true);
    return horizontalLayout;
  }
}
