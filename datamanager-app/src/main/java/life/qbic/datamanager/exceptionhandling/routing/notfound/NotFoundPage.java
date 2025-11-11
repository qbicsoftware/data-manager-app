package life.qbic.datamanager.exceptionhandling.routing.notfound;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.History;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import life.qbic.datamanager.exceptionhandling.routing.ErrorPage;

/**
 * Page shown when a resource was not found.
 */
@AnonymousAllowed
@Route("not-found")
public class NotFoundPage extends Div implements ErrorPage<NotFoundException> {

  public NotFoundPage() {
    add(content());
  }

  private Component content() {
    HorizontalLayout horizontalLayout = new HorizontalLayout();
    horizontalLayout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
    VerticalLayout verticalLayout = new VerticalLayout();
    verticalLayout.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
    horizontalLayout.add(verticalLayout);
    verticalLayout.add(new H1("Not Found"));
    verticalLayout.add(
        new H2("The page you requested does not exist. Please contact support@qbic.zendesk.com."));
    verticalLayout.add(new Span("Error code " + getStatusCode()));
    Button goBack = new Button("Go back");
    goBack.addClickListener(it -> {
      History history = UI.getCurrent().getPage().getHistory();
      history.back();
    });
    verticalLayout.add(goBack);
    return horizontalLayout;
  }

  @Override
  public int getStatusCode() {
    return HttpStatusCode.NOT_FOUND.getCode();
  }

  @Override
  public int setErrorParameter(BeforeEnterEvent event,
      ErrorParameter<NotFoundException> parameter) {
    ErrorPage.super.setErrorParameter(event, parameter);
    return getStatusCode();
  }
}
