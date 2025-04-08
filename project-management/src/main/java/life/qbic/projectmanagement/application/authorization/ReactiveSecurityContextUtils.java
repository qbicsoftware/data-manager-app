package life.qbic.projectmanagement.application.authorization;

import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

public class ReactiveSecurityContextUtils {

  private ReactiveSecurityContextUtils() {
  }


  /**
   * Creates a Reactor Context that contains the Mono<SecurityContext> that can be merged into another Context.
   * @param securityContext the securityContext to set
   * @see ReactiveSecurityContextHolder#withSecurityContext(Mono)
   * @return a ContextView that can be merged into a reactor context
   * @since 1.10.0
   */
  public static ContextView reactiveSecurity(SecurityContext securityContext) {
    return ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext));
  }

  /**
   * Reads the security context from the {@link ReactiveSecurityContextHolder}. Sets the
   * {@link SecurityContext} for the current thread by populating the {@link SecurityContextHolder}
   *
   * @param original the {@link Mono} that is  {@link ReactiveSecurityContextHolder}
   * @param <T>      the type of the mono
   * @return the mono with a configured {@link ReactiveSecurityContextHolder} in the {@link Context}
   */
  public static <T> Mono<T> applySecurityContext(Mono<T> original) {
    return ReactiveSecurityContextHolder.getContext().flatMap(securityContext -> {
      SecurityContextHolder.setContext(securityContext);
      return original;
    });
  }

  /**
   * Same as {@link #applySecurityContext(Mono)} but applies to {@link Flux}.
   *
   * @param original the original reactive stream
   * @param <T>      the type of the flux
   * @return the reactive stream for which the security context has been set explicitly
   * @since 1.10.0
   */
  public static <T> Flux<T> applySecurityContextMany(Flux<T> original) {
    return ReactiveSecurityContextHolder.getContext().flatMapMany(securityContext -> {
      SecurityContextHolder.setContext(securityContext);
      return original;
    });
  }

}
