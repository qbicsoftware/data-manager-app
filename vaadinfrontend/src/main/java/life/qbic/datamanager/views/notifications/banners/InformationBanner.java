package life.qbic.datamanager.views.notifications.banners;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import java.util.Optional;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public class InformationBanner extends Banner {

  private final Component details;
  private final Component title;

  public InformationBanner(Component details) {
    this(null, details);
  }

  public InformationBanner(Component title, Component details) {
    super(VaadinIcon.INFO_CIRCLE_O.create(), new Div());
    requireNonNull(details, "details must not be null");
    ;
    addClassName("information");
    this.details = details;
    details.addClassName("details");
    this.title = title;
    title().ifPresent(it -> it.addClassName("title"));
    setContent(content());
  }

  private Component content() {
    Div content = new Div();
    title().ifPresent(content::add);
    content.add(details);
    return content;
  }

  protected Optional<Component> title() {
    return Optional.ofNullable(title);
  }

}
