package life.qbic.datamanager;

import com.vaadin.flow.component.page.ExtendedClientDetails;
import com.vaadin.flow.component.page.Page.ExtendedClientDetailsReceiver;
import com.vaadin.flow.spring.annotation.SpringComponent;
import java.util.Optional;

/**
 * Receives client details from vaadin and provides them internally.
 */
@SpringComponent
public class ClientDetailsProvideImpl implements ExtendedClientDetailsReceiver,
    ClientDetailsProvider {

  private ExtendedClientDetails extendedClientDetails;

  @Override
  public void receiveDetails(ExtendedClientDetails extendedClientDetails) {
    this.extendedClientDetails = extendedClientDetails;
  }

  public Optional<ClientDetails> latestDetails() {
    return Optional.ofNullable(extendedClientDetails)
        .map(it -> new ClientDetails(it.getTimeZoneId()));
  }

}
