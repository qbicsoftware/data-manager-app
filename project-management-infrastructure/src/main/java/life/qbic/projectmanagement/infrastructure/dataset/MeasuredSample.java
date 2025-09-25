package life.qbic.projectmanagement.infrastructure.dataset;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MeasuredSample(@JsonProperty(value = "code") String sampleId, @JsonProperty(value = "label") String sampleName) {

}
