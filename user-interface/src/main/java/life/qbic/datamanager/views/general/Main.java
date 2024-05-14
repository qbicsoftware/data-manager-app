package life.qbic.datamanager.views.general;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.BeforeEnterObserver;
import java.io.Serial;
import life.qbic.datamanager.security.UserPermissions;

/**
 * <b>Main component functions as a page to which the user can be guided</b>
 *
 * <p>Contains the {@link PageArea} containing the components,
 * which will be shown on the page and handles the routing logic and access rights.
 * Additionally requires that its' implementations provides handling of its context via the {@link BeforeEnterObserver} and the components within
 * via the {@link AfterNavigationObserver}
 */
@com.vaadin.flow.component.Tag(Tag.DIV)
public abstract class Main extends Div implements BeforeEnterObserver, AfterNavigationObserver {

  @Serial
  private static final long serialVersionUID = 6764184508972422298L;
  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  public static final String EXPERIMENT_ID_ROUTE_PARAMETER = "experimentId";
  public final transient UserPermissions userPermissions;

  protected Main(UserPermissions userPermissions) {
    this.userPermissions = requireNonNull(userPermissions, "userPermissions must not be null");
    addClassName("main");
  }


}
