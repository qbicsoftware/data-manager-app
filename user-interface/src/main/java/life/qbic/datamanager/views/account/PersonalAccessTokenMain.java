package life.qbic.datamanager.views.account;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.util.List;
import java.util.Objects;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.MainLayout;
import life.qbic.datamanager.views.account.PersonalAccessTokenComponent.PersonalAccessTokenDTO;
import life.qbic.datamanager.views.general.Main;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Sample Information Main Component
 * <p>
 * This component hosts the components necessary to show and update the information for all
 * {@link Sample} associated with all {@link Experiment} of a {@link Project} information via the
 * provided {@link ProjectId} in the URL
 */

@Route(value = "personal-access-token", layout = MainLayout.class)
@SpringComponent
@UIScope
@PermitAll
public class PersonalAccessTokenMain extends Main implements BeforeEnterObserver {

  @Serial
  private static final long serialVersionUID = -7876265792987169498L;
  private static final Logger log = LoggerFactory.logger(PersonalAccessTokenMain.class);
  private transient Context context;
  private final PersonalAccessTokenComponent personalAccessTokenComponent;
  private final PersonalAccessTokenService personalAccessTokenService;

  public PersonalAccessTokenMain(@Autowired PersonalAccessTokenService personalAccessTokenService,
      @Autowired PersonalAccessTokenComponent personalAccessTokenComponent) {
    Objects.requireNonNull(personalAccessTokenService);
    Objects.requireNonNull(personalAccessTokenComponent);
    this.personalAccessTokenService = personalAccessTokenService;
    this.personalAccessTokenComponent = personalAccessTokenComponent;
    addClassName("personal-access-token");
    add(personalAccessTokenComponent);
    log.debug(String.format(
        "New instance for %s(#%s) created with %s(#%s)",
        this.getClass().getSimpleName(), System.identityHashCode(this),
        personalAccessTokenComponent.getClass().getSimpleName(),
        System.identityHashCode(personalAccessTokenComponent)));
  }

  /**
   * Callback executed before navigation to attaching Component chain is made.
   *
   * @param event before navigation event with event details
   */
  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    loadGeneratedPersonalAccessTokens();
  }

  private void loadGeneratedPersonalAccessTokens() {
    PersonalAccessTokenService personalAccessTokenService = new PersonalAccessTokenService();
    personalAccessTokenComponent.setTokens(personalAccessTokenService.getTokensForUser());
  }

  @Service
  public static class PersonalAccessTokenService {

    public PersonalAccessTokenService() {
    }


    public List<PersonalAccessTokenDTO> getTokensForUser() {
      return List.of(new PersonalAccessTokenDTO("PiD1", "ABC", "NEVER"),
          new PersonalAccessTokenDTO("PiD2", "DEF", "GONNA"),
          new PersonalAccessTokenDTO("PiD3", "GHIJ", "GIVE"),
          new PersonalAccessTokenDTO("PiD4", "KLMN", "YOU"),
          new PersonalAccessTokenDTO("PiD5", "OPQR", "UP"));
    }
  }

}
