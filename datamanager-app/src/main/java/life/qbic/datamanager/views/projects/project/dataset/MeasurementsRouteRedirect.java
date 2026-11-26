package life.qbic.datamanager.views.projects.project.dataset;

import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import life.qbic.datamanager.views.projects.project.ProjectMainLayout;

/**
 * Keeps the retired {@code .../measurements} route working by redirecting it to the merged
 * measurement dataset view.
 *
 * @see LegacyRouteRedirect
 * @since 1.21.0
 */
@SpringComponent
@UIScope
@Route(value = "projects/:projectId?/experiments/:experimentId?/measurements",
    layout = ProjectMainLayout.class)
@PermitAll
public class MeasurementsRouteRedirect extends LegacyRouteRedirect {
}
