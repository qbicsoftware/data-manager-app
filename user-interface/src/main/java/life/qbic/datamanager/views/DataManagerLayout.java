package life.qbic.datamanager.views;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.PageTitle;

/**
 * <b>Data Manager Layout</b>
 *
 * <p>Defines the basic look of the application before a user has logged in
 *
 */
@PageTitle("Data Manager")
public abstract class DataManagerLayout extends AppLayout {

  protected DataManagerLayout() {
    addClassName("data-manager-layout");
    Span drawerTitle = new Span("Data Manager");
    drawerTitle.addClassName("data-manager-title");
    addToNavbar(drawerTitle);
  }
}
