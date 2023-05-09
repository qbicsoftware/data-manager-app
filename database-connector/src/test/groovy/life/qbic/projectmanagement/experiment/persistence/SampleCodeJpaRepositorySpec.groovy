package life.qbic.projectmanagement.experiment.persistence

import life.qbic.projectmanagement.domain.project.ProjectCode
import life.qbic.projectmanagement.domain.project.ProjectId
import spock.lang.Specification

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
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
        def repo = new SampleCodeJpaRepository(sampleStatistic)

        when:
        def result = repo.generateFor(projectId)

        then:
        result.isValue()
        result.getValue().code().equals("QTEST001AL")
        println result.getValue()

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
        def repo = new SampleCodeJpaRepository(sampleStatistic)

        when:
        def result = repo.generateFor(projectId)

        then:
        result.isValue()
        result.getValue().code().equals("QTEST001BU")
        println result.getValue()

    }

    def "Given a 1998 sample statistic entry, generate the next available sample code with letter jump and counter starting with 001"() {
        given:
        SampleStatistic sampleStatistic = Mock(SampleStatistic.class)

        and:
        ProjectId projectId = ProjectId.create()

        and:
        SampleStatisticEntry sampleStatisticEntry = SampleStatisticEntry.create(projectId, ProjectCode.parse("QTEST"))
        for (int i = 1; i < 1998; i++) {
            sampleStatisticEntry.drawNextSampleNumber()
        }

        and:
        sampleStatistic.findByProjectId(projectId) >> [sampleStatisticEntry]

        and:
        def repo = new SampleCodeJpaRepository(sampleStatistic)

        when:
        def result = repo.generateFor(projectId)

        then:
        result.isValue()
        result.getValue().code().equals("QTEST001B")
        println result.getValue()

    }

}
