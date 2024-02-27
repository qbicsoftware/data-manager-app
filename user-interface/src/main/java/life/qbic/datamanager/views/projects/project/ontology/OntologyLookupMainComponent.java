package life.qbic.datamanager.views.projects.project.ontology;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.projects.project.ProjectMainLayout;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Ontology Lookup Main Component
 * <p>
 * This component hosts the components necessary to enable the User to search and display
 * {@link life.qbic.projectmanagement.domain.model.Ontology terms in all linked repositories
 */

@SpringComponent
@UIScope
@Route(value = "projects/:projectId?/ontology", layout = ProjectMainLayout.class)
@PermitAll
public class OntologyLookupMainComponent extends Main implements BeforeEnterObserver {

  @Serial
  private static final long serialVersionUID = -7781433842615499185L;
  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  private static final Logger log = logger(OntologyLookupMainComponent.class);
  private final OntologyLookupComponent ontologyLookupComponent;
  private Context context = new Context();

  public OntologyLookupMainComponent(@Autowired OntologyLookupComponent ontologyLookupComponent) {
    requireNonNull(ontologyLookupComponent);
    this.ontologyLookupComponent = ontologyLookupComponent;
    this.addClassName("ontology-lookup-main");
    log.debug(String.format(
        "New instance for %s(#%s) created with %s(#%s)",
        this.getClass().getSimpleName(), System.identityHashCode(this),
        ontologyLookupComponent.getClass().getSimpleName(),
        System.identityHashCode(ontologyLookupComponent)));
    add(ontologyLookupComponent);
  }

  /**
   * Callback executed before navigation to attaching Component chain is made.
   *
   * @param event before navigation event with event details
   */
  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    String projectID = event.getRouteParameters().get(PROJECT_ID_ROUTE_PARAMETER)
        .orElseThrow();
    if (!ProjectId.isValid(projectID)) {
      throw new ApplicationException("invalid project id " + projectID);
    }
    ProjectId parsedProjectId = ProjectId.parse(projectID);
    this.context = new Context().with(parsedProjectId);
    ontologyLookupComponent.resetSearch();
  }
}
