package life.qbic.projectmanagement.application.measurement.foobar2;


import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;

public record NgsMeasuredSample(
    @Id String sampleId,
    @Column String sampleCode,
    NgsSampleProperties sampleProperties
) implements MeasuredSample<NgsSampleProperties> {

}
