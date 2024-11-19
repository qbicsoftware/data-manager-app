package life.qbic.datamanager.views.demo;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.annotation.UIScope;
import java.util.Arrays;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Profile("test-ui") // This view will only be available when the "test-ui" profile is active
@Route("test-view")
@UIScope
@AnonymousAllowed
@Component
public class ComponentDemo extends Div {

  Div title = new Div("Data Manager - Component Demo");

  public ComponentDemo() {
    title.addClassName("heading-1");
    add(title);
    add(headingShowcase());
    add(fontsShowCase());
  }

  private static Div headingShowcase() {
    Div container = new Div();
    Div header = new Div();
    header.addClassName("heading-2");
    header.setText("Heading styles");
    container.add(header);

    for (int i = 1; i < 7; i++) {
      Div heading = new Div();
      heading.addClassName("heading-" + i);
      heading.setText("Heading " + i);
      Div description = new Div();
      description.addClassName("normal-body-text");
      description.setText("CSS class: %s".formatted(".heading-" + i));
      container.add(heading, description);
    }

    return container;
  }

  private static Div fontsShowCase() {
    Div container = new Div();
    Div header = new Div("Body Font Styles");
    header.addClassName("heading-2");
    container.add(header);

    Arrays.stream(BodyFontStyles.fontStyles).forEach(fontStyle -> {
      Div styleHeader = new Div();
      styleHeader.addClassName("heading-4");
      styleHeader.setText(fontStyle);
      container.add(styleHeader);
      Div style = new Div();
      style.addClassName(fontStyle);
      style.setText(
          ("This is an example of the '.%s' font style.%n"
              + " And it continues in this additional line to demonstrate its line-height. Just make "
              + "the window smaller until the text starts to wrap over multiple "
              + "lines on the screen. ").formatted(
              fontStyle));
      container.add(style);
    });

    return container;

  }

  private static class BodyFontStyles {

    static String[] fontStyles = new String[]{
        "normal-body-text",
        "small-body-text",
        "extra-small-body-text",
        "field-label-text",
        "input-field-text",
        "list-item-text",
        "button.text"
    };
  }
}
