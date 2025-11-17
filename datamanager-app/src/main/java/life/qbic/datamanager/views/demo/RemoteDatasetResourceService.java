package life.qbic.datamanager.views.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Service
public class RemoteDatasetResourceService {

  private final ClientRegistrationRepository clientRegistrationRepository;

  @Autowired
  public RemoteDatasetResourceService(ClientRegistrationRepository clientRegistrationRepository) {
    this.clientRegistrationRepository = Objects.requireNonNull(clientRegistrationRepository);
  }

  public List<RemoteResource> availableResources() {
    if (clientRegistrationRepository instanceof Iterable<?> iterable) {
      return StreamSupport.stream(iterable.spliterator(), false)
          .filter(ClientRegistration.class::isInstance)
          .map(ClientRegistration.class::cast)
          .filter(item -> item.getRegistrationId().startsWith("invenio"))
          .map(item -> new RemoteResource(item.getRegistrationId(), item.getClientName(), ""))
          .toList();
    }
    return List.of();
  }

  public record RemoteResource(String id, String label, String description) {

  }

}


