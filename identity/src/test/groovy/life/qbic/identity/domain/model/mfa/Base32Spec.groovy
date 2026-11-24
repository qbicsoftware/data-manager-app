package life.qbic.identity.domain.model.mfa

import spock.lang.Specification
import spock.lang.Unroll

/**
 * <b>The seed codec (RFC 4648)</b>
 *
 * <p>Checked against the RFC's own test vectors, because this is the format a user may have to type
 * by hand from a printed recovery sheet. A codec that is "almost right" produces seeds that scan and
 * never verify.</p>
 */
class Base32Spec extends Specification {

    @Unroll
    def "RFC 4648 vectors: '#plain' encodes to '#encoded'"() {
        expect:
        Base32.encode(plain.getBytes("UTF-8")) == encoded

        and: "and decodes back"
        new String(Base32.decode(encoded), "UTF-8") == plain

        where:
        plain     | encoded
        ""        | ""
        "f"       | "MY"
        "fo"      | "MZXQ"
        "foo"     | "MZXW6"
        "foob"    | "MZXW6YQ"
        "fooba"   | "MZXW6YTB"
        "foobar"  | "MZXW6YTBOI"
    }

    def "encoding never emits padding"() {
        expect: "padding is optional in the wire format and noise in a seed field"
        !Base32.encode("f".getBytes("UTF-8")).contains("=")
        !Base32.encode("foobar".getBytes("UTF-8")).contains("=")
    }

    def "decoding accepts lower case and padding, because users type seeds"() {
        expect:
        new String(Base32.decode("mzxw6ytboi"), "UTF-8") == "foobar"
        new String(Base32.decode("MZXW6YTBOI======"), "UTF-8") == "foobar"
        new String(Base32.decode("  MZXW6YTBOI  "), "UTF-8") == "foobar"
    }

    @Unroll
    def "invalid input is refused: #reason"() {
        when:
        Base32.decode(input)

        then:
        thrown(IllegalArgumentException)

        where:
        input     | reason
        "MZXW6Y"  | "6 characters carry a partial byte"
        "MYZ"     | "3 characters carry a partial byte"
        "M"       | "1 character carries a partial byte"
        "MZXW6YT!"| "character outside the alphabet"
        "MZXW6 YT"| "space inside the seed"
        "0189"    | "0, 1, 8 and 9 are not in the alphabet"
    }

    def "a null input is rejected"() {
        when:
        Base32.decode(null)

        then:
        thrown(NullPointerException)
    }

    def "binary round-trips, including bytes above the ASCII range"() {
        given:
        def bytes = (0..255).collect { (byte) it } as byte[]

        expect:
        Base32.decode(Base32.encode(bytes)) == bytes
    }
}
