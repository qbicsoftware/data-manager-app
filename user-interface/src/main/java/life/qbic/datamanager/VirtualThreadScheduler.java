package life.qbic.datamanager;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class VirtualThreadScheduler {

  private static final Executor executor = Executors.newVirtualThreadPerTaskExecutor();
  private static final Scheduler scheduler = Schedulers.fromExecutor(executor);

  public static Scheduler getScheduler() {
    return scheduler;
  }

  @Bean
  public static Scheduler scheduler() {
    return scheduler;
  }



}
