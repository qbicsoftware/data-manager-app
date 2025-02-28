package life.qbic.datamanager.security;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
import org.springframework.context.annotation.Bean;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.stereotype.Component;

@Component
public class FilterChainDebugger {

  private final FilterChainProxy filterChainProxy;

  public FilterChainDebugger(FilterChainProxy filterChainProxy) {
    this.filterChainProxy = filterChainProxy;
  }

  @Bean
  public void printFilterChains() {
    filterChainProxy.getFilterChains().forEach(chain -> {
      System.out.println("Filter Chain for: " + chain.getFilters());
      chain.getFilters().forEach(filter -> System.out.println("  " + filter.getClass().getName()));
    });
  }
}
