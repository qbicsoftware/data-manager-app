package life.qbic.datamanager;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import life.qbic.logging.api.Logger;
import static life.qbic.logging.service.LoggerFactory.logger;
import org.springframework.stereotype.Component;

/**
 * <b>Rejected Execution Handler Implementation</b>
 * <p>
 * This implementation handles the case when a {@link ThreadPoolExecutor}'s queue is full and throws
 * a {@link RejectedExecutionException}. Without explicit handling, the application would throw the
 * exception and the task not executed.
 * <p>
 * This class
 * {@link RejectedExecutionHandlerImplementation#rejectedExecution(Runnable, ThreadPoolExecutor)} is
 * called by the thread pool executor, when it has been set via
 * {@link ThreadPoolExecutor#setRejectedExecutionHandler(RejectedExecutionHandler)}.
 * <p>
 * The current implementation will use the {@link java.util.concurrent.BlockingQueue#put(Object)}
 * method, which is by definition blocking in case the queue has reached its configured max
 * capacity.
 *
 * @since 1.0.0
 */
@Component
public class RejectedExecutionHandlerImplementation implements RejectedExecutionHandler {

  private static final Logger log = logger(RejectedExecutionHandlerImplementation.class);

  @Override
  public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
    try {
      log.debug(
          "Thread " + Thread.currentThread().getId() + " rejected, because the queue was full.");
      executor.getQueue().put(r);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RejectedExecutionException("Unexpected InterruptedException", e);
    }
  }
}
