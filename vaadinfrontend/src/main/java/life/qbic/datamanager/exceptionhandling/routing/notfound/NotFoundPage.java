package life.qbic.datamanager.exceptionhandling.routing.notfound;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import life.qbic.datamanager.exceptionhandling.routing.ErrorPage;
import life.qbic.logging.api.Logger;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
@AnonymousAllowed
public class NotFoundPage extends Div implements ErrorPage<NotFoundException> {

  private static final Logger log = logger(NotFoundPage.class);

  public NotFoundPage() {
    HorizontalLayout horizontalLayout = new HorizontalLayout();
    horizontalLayout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
    VerticalLayout verticalLayout = new VerticalLayout();
    verticalLayout.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
    horizontalLayout.add(verticalLayout);
    verticalLayout.add(new H1("404 Not Found"));
    verticalLayout.add(
        new H2("The page you requested does not exist. Please contact support@qbic.zendesk.com."));
    getElement().appendChild(horizontalLayout.getElement());
  }

  @Override
  public void showError(NotFoundException error) {
    // no specific error display
  }

  @Override
  public void logError(NotFoundException error) {
    log.error(error.getMessage(), error);
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
