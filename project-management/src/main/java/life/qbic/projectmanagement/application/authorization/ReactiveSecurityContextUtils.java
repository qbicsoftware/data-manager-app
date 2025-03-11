package life.qbic.projectmanagement.application.authorization;

import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

public class ReactiveSecurityContextUtils {

  private ReactiveSecurityContextUtils() {
  }

  /**
   * Provides the security context to the {@link ReactiveSecurityContextHolder}. The
   * {@link ReactiveSecurityContextHolder} may be used to get the {@link SecurityContext} when
   * needed. Operations upstream of this method may use the {@link ReactiveSecurityContextHolder} to
   * set the {@link SecurityContext} in the thread-local {@link SecurityContextHolder}.
   *
   * @param securityContext the security context to provide over the
   *                        {@link ReactiveSecurityContextHolder}
   * @param original        the {@link Mono} in which the context is enriched with the
   *                        {@link ReactiveSecurityContextHolder}
   * @param <T>             the type of the mono
   * @return the mono with a configured {@link ReactiveSecurityContextHolder} in the {@link Context}
   */
  public static <T> Mono<T> writeSecurityContext(Mono<T> original,
      SecurityContext securityContext) {
    return original.contextWrite(
        ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)));
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

  /**
   * Same as {@link #writeSecurityContext(Mono, SecurityContext)} but applies to {@link Flux}.
   *
   * @param original        the original reactive stream
   * @param securityContext the security context to write into the context of the flux
   * @param <T>             the type of the flux
   * @return the reactive stream for which the {@link ReactiveSecurityContextHolder} has been
   * configured with the provided {@link SecurityContext}.
   * @since 1.10.0
   */
  public static <T> Flux<T> writeSecurityContextMany(Flux<T> original,
      SecurityContext securityContext) {
    return original.contextWrite(
        ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)));
  }

}
