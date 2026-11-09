package life.qbic.application.commons

import spock.lang.Specification
import spock.lang.Unroll

/**
 * Pins the compact size form: whole units, and an approximation mark only where one was made.
 *
 * <p>The mark is the point. A rounded figure that reads exactly ("300 MB") claims a precision the
 * sync does not have, and an exact figure that carries the mark ("~1 KB") disclaims one it does. Both
 * are wrong in the same way -- they misdescribe the number beside them -- so the rule is stated and
 * tested rather than left to whichever branch happened to run.</p>
 */
class FileSizeFormatterSpec extends Specification {

    @Unroll
    def "compact sizes are stated in whole units, marked only when rounded (#bytes -> #expected)"() {
        expect:
        FileSizeFormatter.formatBytesCompact(bytes) == expected

        where:
        bytes                    || expected
        0L                       || "0 B"
        512L                     || "512 B"
        1024L                    || "1 KB"
        2048L                    || "2 KB"
        // Below 10 of a unit one decimal is kept, so a small but real volume does not read as zero.
        400_000L                 || "~391 KB"
        // Below 100 of the unit, one decimal is kept: 9.4 MB must not become "9 MB" silently and
        // a sub-megabyte volume must not become "0 MB".
        3_000_000L               || "~2.9 MB"
        40_000L                  || "~39.1 KB"
        9_400_000L               || "~9 MB"
        100_000_000L             || "~95.4 MB"
        300_000_000L             || "~286 MB"
        1_048_576L               || "1 MB"
        3_145_728L               || "3 MB"
        4_200_000_000L           || "~3.9 GB"
        1_073_741_824L           || "1 GB"
        // Defensive: a negative size is not a size.
        -1L                      || "0 B"
    }

    def "an exact value never carries the approximation mark"() {
        expect: "the mark reports that the figure was rounded, so it must not appear otherwise"
        !FileSizeFormatter.formatBytesCompact(1024L).startsWith("~")
        !FileSizeFormatter.formatBytesCompact(1_048_576L).startsWith("~")
        !FileSizeFormatter.formatBytesCompact(512L).startsWith("~")
    }

    def "the precise form is unchanged for the upload limits that depend on it"() {
        expect: "a limit is a threshold a file is compared against, so its decimals stay"
        FileSizeFormatter.formatBytes(1024L) == "1.00 KB"
        FileSizeFormatter.formatBytes(4_200_000_000L) == "3.91 GB"
    }
}
