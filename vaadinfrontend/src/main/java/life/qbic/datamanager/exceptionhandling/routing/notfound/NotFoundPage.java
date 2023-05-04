package life.qbic.datamanager.exceptionhandling.routing.notfound;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import life.qbic.datamanager.exceptionhandling.routing.ErrorPage;

/**
 * Page shown when a resource was not found.
 */
@AnonymousAllowed
public class NotFoundPage extends Div implements ErrorPage<NotFoundException> {

  public NotFoundPage() {
    Element element = setup();
    getElement().appendChild(element);
  }

  private Element setup() {
    HorizontalLayout horizontalLayout = new HorizontalLayout();
    horizontalLayout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
    VerticalLayout verticalLayout = new VerticalLayout();
    verticalLayout.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
    horizontalLayout.add(verticalLayout);
    verticalLayout.add(new H1("404 Not Found"));
    verticalLayout.add(
        new H2("The page you requested does not exist. Please contact support@qbic.zendesk.com."));
    return horizontalLayout.getElement();
  }

  @Override
  public int getStatusCode() {
    return HttpStatusCode.NOT_FOUND.getCode();
  }

  @Override
  public int setErrorParameter(BeforeEnterEvent event,
      ErrorParameter<NotFoundException> parameter) {
    return getStatusCode();
  }
}
