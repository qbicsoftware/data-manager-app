package life.qbic.datamanager.views.demo;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Profile("development")
@Service
public class InvenioDemoService {

  private final WebClient webClient;

  @Autowired
  public InvenioDemoService(@Qualifier("invenioWebClient") WebClient webClient) {
    this.webClient = Objects.requireNonNull(webClient);
  }

  public Mono<List<Map<String, Object>>> listDepositions() {
    return webClient
        .get()
        .uri("https://zenodo.org/api/deposit/depositions")
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
  }
}
