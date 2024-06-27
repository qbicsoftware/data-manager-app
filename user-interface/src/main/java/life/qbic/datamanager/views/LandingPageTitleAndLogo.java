package life.qbic.datamanager.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.server.StreamResource;

/**
 * Landing Page Title and Logo
 * <p>
 * {@link Div} based Component hosting the university Tübingen Logo and the Title to be shown in all
 * Pages that have the {@link life.qbic.datamanager.views.landing.LandingPageLayout} as their parent
 * layout.
 */
public class LandingPageTitleAndLogo extends Div {

  private final Span title = new Span("Data Manager");
  private final Span subTitle = new Span("University of Tübingen Life Science Data Management");
  private final static String UT_LOGO_PATH = "login/university-tuebingen-logo.svg";

  public LandingPageTitleAndLogo() {
    Image UTLogo = getUTLogo();
    UTLogo.addClickListener(
        event -> UI.getCurrent().getPage().open("https://uni-tuebingen.de/", "_blank"));
    UTLogo.addClassName("clickable");
    add(UTLogo);
    addClassName("landing-page-title-and-logo");
    add(title);
    title.addClassName("title");
    add(subTitle);
    subTitle.addClassName("subtitle");
  }

  private Image getUTLogo() {
    StreamResource utResource = new StreamResource("university_tuebingen_logo.svg",
        () -> getClass().getClassLoader().getResourceAsStream(UT_LOGO_PATH));
    Image utLogo = new Image(utResource, "university_tuebingen_logo");
    utLogo.addClassName("ut-logo");
    return utLogo;
  }
}
