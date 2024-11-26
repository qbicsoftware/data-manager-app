package life.qbic.datamanager.exceptionhandling.routing.exception;

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
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.util.Locale;
import life.qbic.datamanager.exceptionhandling.ErrorMessageTranslationService;
import life.qbic.datamanager.exceptionhandling.ErrorMessageTranslationService.UserFriendlyErrorMessage;
import life.qbic.datamanager.exceptionhandling.routing.ErrorPage;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Page shown when any error occurred. Shows an appropriate user friendly message.
 */
@AnonymousAllowed
@Route("error")
public class ExceptionErrorPage extends Div implements ErrorPage<Exception> {

  private final transient ErrorMessageTranslationService errorMessageTranslationService;
  private final H1 title;
  private final H2 errorCode;
  private final Span message;


  public ExceptionErrorPage(
      @Autowired ErrorMessageTranslationService errorMessageTranslationService) {
    this.errorMessageTranslationService = errorMessageTranslationService;
    title = new H1();
    message = new Span();
    errorCode = new H2("Error code " + getStatusCode());
    add(content());
  }

  private Component content() {
    HorizontalLayout horizontalLayout = new HorizontalLayout();
    horizontalLayout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
    VerticalLayout verticalLayout = new VerticalLayout();
    verticalLayout.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
    horizontalLayout.add(verticalLayout);
    verticalLayout.add(title);
    verticalLayout.add(message);
    verticalLayout.add(errorCode);
    Button goBack = new Button("Go back");
    goBack.addClickListener(it -> {
      History history = UI.getCurrent().getPage().getHistory();
      history.back();
    });
    verticalLayout.add(goBack);
    verticalLayout.setSizeFull();
    horizontalLayout.setSizeFull();
    return horizontalLayout;
  }

  @Override
  public void showError(Exception error, Locale locale) {
    showUserFriendlyMessage(errorMessageTranslationService.translate(error, locale));
  }

  private void showUserFriendlyMessage(UserFriendlyErrorMessage message) {
    this.title.setText(message.title());
    this.message.setText(message.message());
  }

  @Override
  public int getStatusCode() {
    return HttpStatusCode.INTERNAL_SERVER_ERROR.getCode();
  }
}
