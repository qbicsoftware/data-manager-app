package life.qbic.datamanager.configuration;

import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

@Configuration
public class UploadConfiguration {

  private final DataSize maxInMemoryThreshold;
  private final DataSize maxFileSize;

  public UploadConfiguration(
      @Value("${qbic.upload.in-memory-limit}") DataSize maxInMemoryThreshold,
      @Value("${qbic.upload.max-file-size}") DataSize maxFileSize) {
    this.maxInMemoryThreshold = Objects.requireNonNull(maxInMemoryThreshold);
    this.maxFileSize = Objects.requireNonNull(maxFileSize);
  }

  public DataSize maxInMemoryThreshold() {
    return maxInMemoryThreshold;
  }

  public DataSize maxFileSize() {
    return maxFileSize;
  }
}
