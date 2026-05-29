package life.qbic.datamanager.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

@Configuration
public class UploadConfiguration {

  private final long maxInMemoryBytes;

  public UploadConfiguration(
      @Value("${qbic.upload.in-memory-limit}") DataSize maxInMemoryThreshold) {
    this.maxInMemoryBytes = maxInMemoryThreshold.toBytes();
  }

  public long maxInMemoryBytes() {
    return maxInMemoryBytes;
  }
}
