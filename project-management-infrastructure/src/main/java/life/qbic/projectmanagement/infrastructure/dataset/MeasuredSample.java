package life.qbic.projectmanagement.infrastructure.dataset;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * <b><record short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MeasuredSample(@JsonProperty(value = "code") String sampleId, @JsonProperty(value = "label") String sampleName) {

}
