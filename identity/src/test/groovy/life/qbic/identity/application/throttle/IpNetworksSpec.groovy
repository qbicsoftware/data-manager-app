package life.qbic.identity.application.throttle

import spock.lang.Specification
import spock.lang.Unroll

/**
 * <b>The origin key must be an identity an attacker cannot rotate for free.</b>
 *
 * <p>Every case here is a way the origin bucket silently stops working. None of them is caught by a
 * test that only feeds one address through once, which is exactly how a bucket that looks correct and
 * is bypassable gets shipped.</p>
 */
class IpNetworksSpec extends Specification {

  // ---- parsing: strict literals only -----------------------------------------------------------

  def "a plain IPv4 literal parses"() {
    when:
    def parsed = IpNetworks.parse('203.0.113.7')

    then:
    parsed.present
    parsed.get().ipv4()
    parsed.get().bytes().length == 4
  }

  @Unroll
  def "the non-literal #value is rejected rather than resolved"() {
    // `InetAddress.getByName` would perform a DNS lookup here. On a request-supplied value that is
    // both an attacker-controlled resolver call and unbounded latency inside the throttle.
    expect:
    IpNetworks.parse(value).isEmpty()

    where:
    value << [
        'localhost',
        'example.org',
        'rm -rf /',
        '',
        '   ',
        null,
        '1.2.3',
        '1.2.3.4.5',
        '256.0.0.1',
        '1.2.3.-1',
        // Leading zeros are octal to some parsers and decimal to others. Two components disagreeing
        // about one string means the string is not an identity.
        '010.0.0.1',
        // A trailing space is NOT in this list: surrounding whitespace is stripped, because a proxy
        // that emits ` X-Forwarded-For: 1.2.3.4 ` is emitting a valid address and rejecting it would
        // silently disable origin scope on that deployment.
        '1.2.3.4 5',
        '::g',
        '1:2:3:4:5:6:7:8:9',
    ]
  }

  @Unroll
  def "surrounding whitespace is normalised rather than rejected: #value"() {
    // Rejecting these would be a silent deployment failure, not a security improvement: the address
    // is genuine and the bucket is the same one.
    expect:
    IpNetworks.parse(value).orElseThrow().bytes() ==
        IpNetworks.parse('203.0.113.7').orElseThrow().bytes()

    where:
    value << ['203.0.113.7', ' 203.0.113.7', '203.0.113.7 ', '\t203.0.113.7\n']
  }

  def "an IPv6 literal and its zero-compressed spelling are the same value"() {
    given:
    def expanded = IpNetworks.parse('2001:0db8:0000:0000:0000:0000:0000:0001').orElseThrow()
    def compressed = IpNetworks.parse('2001:db8::1').orElseThrow()

    expect:
    !expanded.ipv4()
    expanded.bytes() == compressed.bytes()
  }

  def "an IPv6 zone id is stripped rather than rejecting an otherwise valid address"() {
    expect:
    IpNetworks.parse('fe80::1%eth0').orElseThrow().bytes() ==
        IpNetworks.parse('fe80::1').orElseThrow().bytes()
  }

  def "an over-long or repeated `::` is rejected instead of guessed at"() {
    expect:
    // Nine groups cannot fit in 128 bits however `::` is read.
    IpNetworks.parse('::1:2:3:4:5:6:7:8').isEmpty()
    IpNetworks.parse('1:2:3:4:5:6:7:8::').isEmpty()
    IpNetworks.parse('1::2::3').isEmpty()
  }

  def "`::` standing for exactly one zero group is valid, not rejected"() {
    // RFC 4291 allows `::` to represent one or more zero groups. `1:2:3:4:5:6:7::` is
    // `1:2:3:4:5:6:7:0`, so rejecting it would drop a legitimate address.
    expect:
    IpNetworks.parse('1:2:3:4:5:6:7::').orElseThrow().bytes() ==
        IpNetworks.parse('1:2:3:4:5:6:7:0').orElseThrow().bytes()
  }

  // ---- grouping: the property that makes rotation cost the attacker ---------------------------------

  @Unroll
  def "IPv4 addresses in one /24 share a bucket: #address -> #network"() {
    expect:
    IpNetworks.groupedNetwork(IpNetworks.parse(address).orElseThrow()) == network

    where:
    address          | network
    '203.0.113.1'    | '203.0.113.0/24'
    '203.0.113.7'    | '203.0.113.0/24'
    '203.0.113.254'  | '203.0.113.0/24'
    '203.0.114.7'    | '203.0.114.0/24'
    '10.1.2.3'       | '10.1.2.0/24'
    '0.0.0.0'        | '0.0.0.0/24'
    '255.255.255.255'| '255.255.255.0/24'
  }

  @Unroll
  def "IPv6 addresses in one /64 share a bucket: #address -> #network"() {
    // A residential IPv6 allocation is commonly a /56 or /48, i.e. 256 to 65 536 addresses. Keying on
    // a single address would give an attacker that many fresh buckets for free.
    expect:
    IpNetworks.groupedNetwork(IpNetworks.parse(address).orElseThrow()) == network

    where:
    address                                | network
    '2001:db8:0:1::1'                      | '2001:0db8:0000:0001:0000:0000:0000:0000/64'
    '2001:db8:0:1::ffff'                   | '2001:0db8:0000:0001:0000:0000:0000:0000/64'
    '2001:db8:0:1:ffff:ffff:ffff:ffff'     | '2001:0db8:0000:0001:0000:0000:0000:0000/64'
    '2001:db8:0:2::1'                      | '2001:0db8:0000:0002:0000:0000:0000:0000/64'
  }

  def "an IPv4-mapped IPv6 address is folded to the IPv4 bucket, not given its own"() {
    // ::ffff:203.0.113.7 and 203.0.113.7 are one client. Treating them as two networks hands a
    // dual-stack attacker two buckets for one source.
    when:
    def mapped = IpNetworks.parse('::ffff:203.0.113.7').orElseThrow()
    def plain = IpNetworks.parse('203.0.113.7').orElseThrow()

    then:
    mapped.ipv4()
    IpNetworks.groupedNetwork(mapped) == IpNetworks.groupedNetwork(plain)
    IpNetworks.groupedNetwork(plain) == '203.0.113.0/24'
  }

  def "the hex-spelled mapped form is also folded"() {
    expect:
    IpNetworks.parse('::ffff:cb00:7107').orElseThrow().ipv4()
    IpNetworks.groupedNetwork(IpNetworks.parse('::ffff:cb00:7107').orElseThrow()) == '203.0.113.0/24'
  }

  @Unroll
  def "two spellings of one network never produce two keys: #a vs #b"() {
    expect:
    IpNetworks.groupedNetwork(IpNetworks.parse(a).orElseThrow()) ==
        IpNetworks.groupedNetwork(IpNetworks.parse(b).orElseThrow())

    where:
    a                       | b
    '2001:db8::1'           | '2001:0DB8:0000:0000:0000:0000:0000:0001'
    '2001:db8:0:0:0:0:0:1'  | '2001:db8::1'
    '::1'                   | '0:0:0:0:0:0:0:1'
  }

  // ---- CIDR matching: the internal-proxy trust check ----------------------------------------------

  def "the RFC1918 ranges the container trusts are recognised"() {
    given: 'Boot\'s default internal-proxies value'
    def internalProxies = '10.0.0.0/8, 192.168.0.0/16, 169.254.0.0/16, 100.64.0.0/10, ' +
        'fc00::/7, 172.16.0.0/12, ::1/128, 127.0.0.0/8, fe80::/10'

    expect:
    IpNetworks.isInAnyRange(IpNetworks.parse(address).orElseThrow(), internalProxies) == internal

    where:
    address          | internal
    '10.1.2.3'       | true
    '172.20.214.50'  | true     // the HAProxy nodes in the issue's recorded config
    '172.20.214.49'  | true
    '192.168.1.1'    | true
    '127.0.0.1'      | true
    '169.254.1.1'    | true
    '100.64.0.1'     | true
    '::1'            | true
    'fe80::1'        | true
    'fd00::1'        | true
    '203.0.113.7'    | false
    '8.8.8.8'        | false
    '2001:db8::1'    | false
    // 172.32 is outside 172.16.0.0/12 (which ends at 172.31.255.255) and must not be swallowed.
    '172.32.0.1'     | false
  }

  def "a prefix that is not a whole number of bytes is matched on the remaining bits"() {
    when: 'a /12 — the only non-octet boundary in the default list'
    def range = IpNetworks.parseRange('172.16.0.0/12').orElseThrow()

    then:
    range.contains(IpNetworks.parse('172.16.0.1').orElseThrow())
    range.contains(IpNetworks.parse('172.31.255.255').orElseThrow())
    !range.contains(IpNetworks.parse('172.32.0.0').orElseThrow())
    !range.contains(IpNetworks.parse('172.15.255.255').orElseThrow())
  }

  def "families never match each other"() {
    expect:
    !IpNetworks.parseRange('10.0.0.0/8').orElseThrow()
        .contains(IpNetworks.parse('::1').orElseThrow())
    !IpNetworks.parseRange('::1/128').orElseThrow()
        .contains(IpNetworks.parse('10.0.0.1').orElseThrow())
  }

  @Unroll
  def "the malformed range #entry is skipped rather than failing the whole list"() {
    // One typo in a comma-separated list must not silently disable the trust check in the other
    // direction (i.e. make everything look internal) or throw on every request.
    expect:
    IpNetworks.parseRange(entry).isEmpty()

    where:
    entry << [null, '', '10.0.0.0', '10.0.0.0/', '/8', '10.0.0.0/33', '10.0.0.0/-1',
              'host.example/8', '10.0.0.0/eight']
  }

  def "a range with a blank or null list matches nothing"() {
    expect:
    !IpNetworks.isInAnyRange(IpNetworks.parse('10.0.0.1').orElseThrow(), null)
    !IpNetworks.isInAnyRange(IpNetworks.parse('10.0.0.1').orElseThrow(), '  ')
  }

  def "a /0 matches every address of its family"() {
    expect:
    IpNetworks.parseRange('0.0.0.0/0').orElseThrow()
        .contains(IpNetworks.parse('203.0.113.7').orElseThrow())
  }
}
