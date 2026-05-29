package life.qbic.datamanager.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UploadConfiguration {

  private final long maxInMemoryBytes;

  public UploadConfiguration(@Value("${qbic.upload.in-memory-limit}") long maxInMemoryBytes) {
    this.maxInMemoryBytes = maxInMemoryBytes;
  }

  public long maxInMemoryBytes() {
    return maxInMemoryBytes;
  }
}
