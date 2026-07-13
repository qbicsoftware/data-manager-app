package life.qbic.projectmanagement.infrastructure.dataset;

import tools.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MeasuredSample(@JsonProperty(value = "code") String sampleId, @JsonProperty(value = "label") String sampleName) {

}
