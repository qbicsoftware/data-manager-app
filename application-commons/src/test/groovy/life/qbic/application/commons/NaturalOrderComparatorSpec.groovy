package life.qbic.application.commons


import spock.lang.Specification

import java.util.function.Consumer
import java.util.function.Function

import static life.qbic.application.commons.Result.*


class NaturalOrderComparatorSpec extends Specification {

    def "should order '#a' and '#b' with expected sign #expectedSign"() {
        expect:
        Integer.signum(NaturalOrderComparator.CASE_INSENSITIVE.compare(a, b)) == expectedSign

        where:
        a       | b       || expectedSign
        "S-1"   | "S-2"   || -1
        "S-2"   | "S-1"   || 1
        "S-2"   | "S-10"  || -1
        "S-10"  | "S-2"   || 1
        "S-1"   | "S-01"  || -1   // equal numeric value, shorter string wins
        "S-01"  | "S-1"   || 1    // equal numeric value, longer string loses
        "abc"   | "abc"   || 0
        "abc"   | "abd"   || -1
    }

    def "should ignore case for letter chunks only when CASE_INSENSITIVE is used"() {
        given:
        def a = "Q2ABCD"
        def b = "q2abcd"

        expect:
        NaturalOrderComparator.CASE_INSENSITIVE.compare(a, b) == 0
        NaturalOrderComparator.CASE_SENSITIVE.compare(a, b) != 0
    }

    def "should treat comparison as case-sensitive when configured"() {
        given: "two strings differing only in case"
        def result = NaturalOrderComparator.CASE_SENSITIVE.compare("Q2ABCD", "q2abcd")

        expect: "uppercase sorts before lowercase, per String.compareTo"
        result < 0
    }

    def "should return 0 when both values are null"() {
        expect:
        NaturalOrderComparator.CASE_INSENSITIVE.compare(null, null) == 0
    }

    def "should sort null values before non-null values"() {
        expect:
        NaturalOrderComparator.CASE_INSENSITIVE.compare(null, "a") < 0
        NaturalOrderComparator.CASE_INSENSITIVE.compare("a", null) > 0
    }

    def "should sort a full sample list into natural ascending order"() {
        given: "an unsorted list of sample-like IDs"
        def ids = ["Q2ABCD-10", "Q2ABCD-2", "Q2ABCD-1", "Q2ABCD-20", "Q2ABCD-3"]

        when: "sorted using the natural order comparator"
        def sorted = ids.toSorted(NaturalOrderComparator.CASE_INSENSITIVE)

        then: "numeric suffixes are ordered by value, not lexicographically"
        sorted == ["Q2ABCD-1", "Q2ABCD-2", "Q2ABCD-3", "Q2ABCD-10", "Q2ABCD-20"]
    }

}
