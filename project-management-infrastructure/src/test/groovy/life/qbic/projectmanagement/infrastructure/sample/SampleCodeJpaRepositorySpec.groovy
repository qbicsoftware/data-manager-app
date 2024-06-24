package life.qbic.projectmanagement.infrastructure.sample

import life.qbic.projectmanagement.domain.model.project.ProjectCode
import life.qbic.projectmanagement.domain.model.project.ProjectId
import spock.lang.Specification

/**
 * Tests for the sample code service implementation
 *
 * @since 1.0.0
 */
class SampleCodeJpaRepositorySpec extends Specification{

    def "Given a fresh sample statistic entry, generate the next available sample code"() {
        given:
        SampleStatistic sampleStatistic = Mock(SampleStatistic.class)

        and:
        ProjectId projectId = ProjectId.create()

        and:
        SampleStatisticEntry sampleStatisticEntry = SampleStatisticEntry.create(projectId, ProjectCode.parse("Q2TEST"))

        and:
        sampleStatistic.findByProjectId(projectId) >> [sampleStatisticEntry]

        and:
        def repo = new SampleCodeServiceImplementation(sampleStatistic)

        when:
        def result = repo.generateFor(projectId)

        then:
        result.isValue()
        result.getValue().code().equals("Q2TEST001A5")
    }

    def "Given 998 sample statistic entry, generate the next available sample code with no letter jump and counter starting with 999"() {
        given:
        SampleStatistic sampleStatistic = Mock(SampleStatistic.class)

        and:
        ProjectId projectId = ProjectId.create()

        and:
        SampleStatisticEntry sampleStatisticEntry = SampleStatisticEntry.create(projectId, ProjectCode.parse("Q2TEST"))
        // Prime to 999 sample numbers that have been drawn already
        for (int i = 1; i <= 998; i++) {
            sampleStatisticEntry.drawNextSampleNumber()
        }

        and:
        sampleStatistic.findByProjectId(projectId) >> [sampleStatisticEntry]

        and:
        def repo = new SampleCodeServiceImplementation(sampleStatistic)

        when:
        def result = repo.generateFor(projectId)

        then:
        result.isValue()
        result.getValue().code().equals("Q2TEST999A8")
    }


    def "Given a 999 sample statistic entry, generate the next available sample code with letter jump and counter starting with 001"() {
        given:
        SampleStatistic sampleStatistic = Mock(SampleStatistic.class)

        and:
        ProjectId projectId = ProjectId.create()

        and:
        SampleStatisticEntry sampleStatisticEntry = SampleStatisticEntry.create(projectId, ProjectCode.parse("Q2TEST"))
        // Prime to 999 sample numbers that have been drawn already
        for (int i = 1; i <= 999; i++) {
            sampleStatisticEntry.drawNextSampleNumber()
        }

        and:
        sampleStatistic.findByProjectId(projectId) >> [sampleStatisticEntry]

        and:
        def repo = new SampleCodeServiceImplementation(sampleStatistic)

        when:
        def result = repo.generateFor(projectId)

        then:
        result.isValue()
        result.getValue().code().equals("Q2TEST001BF")
    }

    def "Given a 1998 sample statistic entry, generate the next available sample code with letter jump and counter starting with 001"() {
        given:
        SampleStatistic sampleStatistic = Mock(SampleStatistic.class)

        and:
        ProjectId projectId = ProjectId.create()

        and:
        SampleStatisticEntry sampleStatisticEntry = SampleStatisticEntry.create(projectId, ProjectCode.parse("Q2TEST"))
        for (int i = 1; i <= 1998; i++) {
            sampleStatisticEntry.drawNextSampleNumber()
        }

        and:
        sampleStatistic.findByProjectId(projectId) >> [sampleStatisticEntry]

        and:
        def repo = new SampleCodeServiceImplementation(sampleStatistic)

        when:
        def result = repo.generateFor(projectId)

        then:
        result.isValue()
        result.getValue().code().equals("Q2TEST001CP")
    }

    def "Given a 1997 sample statistic entry, generate the next available sample code with letter jump and counter starting with 001"() {
        given:
        SampleStatistic sampleStatistic = Mock(SampleStatistic.class)

        and:
        ProjectId projectId = ProjectId.create()

        and:
        SampleStatisticEntry sampleStatisticEntry = SampleStatisticEntry.create(projectId, ProjectCode.parse("Q2TEST"))
        for (int i = 1; i <= 1997; i++) {
            sampleStatisticEntry.drawNextSampleNumber()
        }

        and:
        sampleStatistic.findByProjectId(projectId) >> [sampleStatisticEntry]

        and:
        def repo = new SampleCodeServiceImplementation(sampleStatistic)

        when:
        def result = repo.generateFor(projectId)

        then:
        result.isValue()
        result.getValue().code().equals("Q2TEST999BI")
    }

}
