package life.qbic.datamanager.views.project.create;

import com.vaadin.flow.router.BeforeEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import life.qbic.projectmanagement.domain.offer.OfferId;
import org.springframework.stereotype.Component;

@Component
public class CreateProjectHandler implements CreateProjectHandlerInterface {

  private static final String OFFER_ID_QUERY_PARAM = "offerId";

  private CreateProjectLayout createProjectLayout;

  public CreateProjectHandler(){}

  @Override
  public void handle(CreateProjectLayout createProjectLayout) {
    if (this.createProjectLayout != createProjectLayout) {
      this.createProjectLayout = createProjectLayout;
      addSaveClickListener();
    }
  }

  @Override
  public void handleEvent(BeforeEvent event) {
    Map<String, List<String>> queryParams = event.getLocation().getQueryParameters()
        .getParameters();
    if (queryParams.containsKey(OFFER_ID_QUERY_PARAM)) {
      String offerId = queryParams.get(OFFER_ID_QUERY_PARAM).iterator().next();
      preloadContentFromOffer(offerId);
    }
  }

  private void preloadContentFromOffer(String offerId) {
    System.out.println("Receiving offerId " + offerId);
    OfferId offer = new OfferId(offerId);
    // TODO call Offer Lookup Service
    // TODO fill field from offer query result
  }

  private Map<String, String> parseFromUrlParameter(String parameter) throws IllegalArgumentException {
    String[]  parameterArray = parameter.trim().split("=");
    if (parameterArray.length != 2) {
      throw new IllegalArgumentException("Unknown parameter syntax " + parameter);
    }
    Map<String, String> paramMap = new HashMap<>();
    paramMap.put(parameterArray[0], parameterArray[1]);
    return paramMap;
  }

  private void addSaveClickListener() {
    createProjectLayout.saveButton.addClickListener(it -> saveClicked());
  }

  private void saveClicked() {
    String titleFieldValue = createProjectLayout.titleField.getValue();
    //TODO pass information to service
  }
}
