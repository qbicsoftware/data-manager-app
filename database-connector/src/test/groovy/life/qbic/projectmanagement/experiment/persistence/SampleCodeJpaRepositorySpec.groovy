package life.qbic.projectmanagement.experiment.persistence

import life.qbic.projectmanagement.domain.project.ProjectCode
import life.qbic.projectmanagement.domain.project.ProjectId
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
        SampleStatisticEntry sampleStatisticEntry = SampleStatisticEntry.create(projectId, ProjectCode.parse("QTEST"))

        and:
        sampleStatistic.findByProjectId(projectId) >> [sampleStatisticEntry]

        and:
        def repo = new SampleCodeServiceImplementation(sampleStatistic)

        when:
        def result = repo.generateFor(projectId)

        then:
        result.isValue()
        result.getValue().code().equals("QTEST001AL")
    }

    def "Given 998 sample statistic entry, generate the next available sample code with no letter jump and counter starting with 999"() {
        given:
        SampleStatistic sampleStatistic = Mock(SampleStatistic.class)

        and:
        ProjectId projectId = ProjectId.create()

        and:
        SampleStatisticEntry sampleStatisticEntry = SampleStatisticEntry.create(projectId, ProjectCode.parse("QTEST"))
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
        result.getValue().code().equals("QTEST999AW")
    }


    def "Given a 999 sample statistic entry, generate the next available sample code with letter jump and counter starting with 001"() {
        given:
        SampleStatistic sampleStatistic = Mock(SampleStatistic.class)

        and:
        ProjectId projectId = ProjectId.create()

        and:
        SampleStatisticEntry sampleStatisticEntry = SampleStatisticEntry.create(projectId, ProjectCode.parse("QTEST"))
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
        result.getValue().code().equals("QTEST001BU")
    }

    def "Given a 1998 sample statistic entry, generate the next available sample code with letter jump and counter starting with 001"() {
        given:
        SampleStatistic sampleStatistic = Mock(SampleStatistic.class)

        and:
        ProjectId projectId = ProjectId.create()

        and:
        SampleStatisticEntry sampleStatisticEntry = SampleStatisticEntry.create(projectId, ProjectCode.parse("QTEST"))
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
        result.getValue().code().equals("QTEST001C5")
    }

    def "Given a 1997 sample statistic entry, generate the next available sample code with letter jump and counter starting with 001"() {
        given:
        SampleStatistic sampleStatistic = Mock(SampleStatistic.class)

        and:
        ProjectId projectId = ProjectId.create()

        and:
        SampleStatisticEntry sampleStatisticEntry = SampleStatisticEntry.create(projectId, ProjectCode.parse("QTEST"))
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
        result.getValue().code().equals("QTEST999B7")
    }

}
