package life.qbic.datamanager.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.RouterLayout;
import java.util.Objects;
import life.qbic.datamanager.views.general.footer.FooterComponent;

/**
 * <b>Data Manager Layout</b>
 *
 * <p>Defines the basic look of the application for all sites that do not require the user to login
 *
 */
@PageTitle("Data Manager")
public class DataManagerLayout extends AppLayout implements RouterLayout {

  private final Div contentArea;

  protected DataManagerLayout(FooterComponent footerComponent) {
    Objects.requireNonNull(footerComponent);
    addClassName("data-manager-layout");
    // Create content area
    contentArea = new Div();
    contentArea.setId("content-area");
    // Add content area and footer to the main layout
    Div mainLayout = new Div(contentArea, footerComponent);
    mainLayout.setId("main-layout");
    setContent(mainLayout);
  }

  /**
   * {@inheritDoc}
   *
   * @param content
   * @throws IllegalArgumentException if content is not a {@link Component}
   */
  @Override
  public void showRouterLayoutContent(HasElement content) {
    contentArea.removeAll();
    contentArea.getElement().appendChild(content.getElement());
  }
}
