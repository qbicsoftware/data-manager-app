package life.qbic.datamanager;

import java.util.concurrent.RejectedExecutionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

@Configuration
@EnableAsync()
public class AsyncConfig implements AsyncConfigurer {

  @Bean
  public ThreadPoolTaskExecutor threadPoolTaskExecutor(
      RejectedExecutionHandler rejectedExecutionHandler) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(10);
    executor.setMaxPoolSize(100);
    executor.setQueueCapacity(50);
    executor.setThreadNamePrefix("async-");
    executor.setRejectedExecutionHandler(rejectedExecutionHandler);
    return executor;
  }

  @Bean
  public DelegatingSecurityContextAsyncTaskExecutor taskExecutor(ThreadPoolTaskExecutor delegate) {
    return new DelegatingSecurityContextAsyncTaskExecutor(delegate);
  }

}
