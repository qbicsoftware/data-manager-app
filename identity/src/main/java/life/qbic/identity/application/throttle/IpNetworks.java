package life.qbic.identity.application.throttle;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * <b>Strict literal IP-address parsing, CIDR matching, and the network grouping used as an
 * origin-throttle key.</b>
 *
 * <p>This class exists instead of {@link java.net.InetAddress#getByName(String)} on purpose.
 * {@code getByName} performs a <b>DNS lookup</b> for anything that is not a literal, so feeding it a
 * request-supplied value turns the throttle into (a) an attacker-controlled resolver call and (b) a
 * source of latency the throttle cannot bound. An unparseable value here is simply
 * {@link Optional#empty()} — the caller then treats itself as having no trusted origin.</p>
 *
 * <h3>Why addresses are grouped rather than used whole</h3>
 * <p>A bucket keyed on a single address is trivially evaded by an attacker holding an allocation: a
 * residential IPv6 prefix is commonly a {@code /56} or a {@code /48}, giving 256 to 65536 addresses
 * that rotate freely, and an IPv4 {@code /24} is a normal hosting-provider allotment. Grouping to
 * {@code /24} (IPv4) and {@code /64} (IPv6) makes rotation inside one allocation cost the attacker
 * the same bucket. This is the smallest grouping that still distinguishes separate household or
 * campus connections from one another.</p>
 *
 * <p><b>IPv4-mapped IPv6 is folded to IPv4 first.</b> {@code ::ffff:203.0.113.7} and
 * {@code 203.0.113.7} are the same client, but grouping them separately would hand an attacker two
 * buckets for one source — and a dual-stack client reaches the same server both ways.</p>
 *
 * @since 1.20.4
 */
public final class IpNetworks {

  /** IPv4 grouped to its /24: one provider allotment, one household, one campus NAT pool. */
  static final int IPV4_GROUP_BITS = 24;

  /** IPv6 grouped to its /64: one subscriber, one allocation. */
  static final int IPV6_GROUP_BITS = 64;

  private IpNetworks() {
    // static helpers only
  }

  /**
   * Parses a literal address in any of the forms a proxy or a JDK actually emits.
   *
   * @param literal the candidate; may be {@code null}
   * @return the packed address, or empty when the value is not an IP literal
   */
  public static Optional<ParsedAddress> parse(String literal) {
    if (literal == null) {
      return Optional.empty();
    }
    // `%zone` is the IPv6 scope id, e.g. fe80::1%eth0. It is not part of the address and never
    // appears in a forwarded header, but stripping it is cheaper than rejecting a valid address.
    String candidate = literal.strip();
    int zone = candidate.indexOf('%');
    if (zone >= 0) {
      candidate = candidate.substring(0, zone);
    }
    if (candidate.isEmpty()) {
      return Optional.empty();
    }
    return candidate.indexOf(':') >= 0 ? parseIpv6(candidate) : parseIpv4(candidate);
  }

  /**
   * The key material for this address: its network, in canonical form.
   *
   * <p>Two spellings of one network must produce the same string, or the bucket splits. The forms
   * here are fully expanded and zero-padded so that no two distinct networks collide and no single
   * network has two spellings.</p>
   *
   * @param address a parsed address
   * @return e.g. {@code 203.0.113.0/24} or {@code 2001:db8:0:1::/64}
   */
  public static String groupedNetwork(ParsedAddress address) {
    requireNonNull(address, "address must not be null");
    if (address.ipv4()) {
      byte[] network = new byte[4];
      System.arraycopy(address.bytes(), 0, network, 0, 3);
      return dotted(network) + "/" + IPV4_GROUP_BITS;
    }
    byte[] network = new byte[16];
    System.arraycopy(address.bytes(), 0, network, 0, IPV6_GROUP_BITS / 8);
    return expandedIpv6(network) + "/" + IPV6_GROUP_BITS;
  }

  /**
   * Whether a parsed address falls inside one of the comma-separated CIDR ranges.
   *
   * <p>Used to answer "is this address still an internal proxy?", which is the check that stops a
   * bucket being keyed on the load balancer itself.</p>
   *
   * @param address the address to test
   * @param csvCidrs a comma-separated list of {@code address/prefix} entries; blank entries and
   *                 entries that are not valid CIDRs are skipped rather than failing the whole list
   * @return whether the address is inside any range
   */
  public static boolean isInAnyRange(ParsedAddress address, String csvCidrs) {
    requireNonNull(address, "address must not be null");
    if (csvCidrs == null || csvCidrs.isBlank()) {
      return false;
    }
    for (String entry : csvCidrs.split(",")) {
      Optional<CidrRange> range = parseRange(entry);
      if (range.isPresent() && range.get().contains(address)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Parses one {@code address/prefix} entry.
   *
   * @param entry e.g. {@code 10.0.0.0/8}
   * @return the range, or empty when the entry is not a valid CIDR
   */
  public static Optional<CidrRange> parseRange(String entry) {
    if (entry == null) {
      return Optional.empty();
    }
    String candidate = entry.strip();
    int slash = candidate.indexOf('/');
    if (slash <= 0 || slash == candidate.length() - 1) {
      return Optional.empty();
    }
    Optional<ParsedAddress> base = parse(candidate.substring(0, slash));
    int prefix;
    try {
      prefix = Integer.parseInt(candidate.substring(slash + 1).strip());
    } catch (NumberFormatException e) {
      return Optional.empty();
    }
    if (base.isEmpty()) {
      return Optional.empty();
    }
    int width = base.get().ipv4() ? 32 : 128;
    if (prefix < 0 || prefix > width) {
      return Optional.empty();
    }
    return Optional.of(new CidrRange(base.get(), prefix));
  }

  // ------------------------------------------------------------------ IPv4

  private static Optional<ParsedAddress> parseIpv4(String literal) {
    String[] parts = literal.split("\\.", -1);
    if (parts.length != 4) {
      return Optional.empty();
    }
    byte[] bytes = new byte[4];
    for (int i = 0; i < 4; i++) {
      // Reject leading zeros: `010.0.0.1` is octal to some parsers and decimal to others, so two
      // components could disagree about the same string — and an identity that two parsers disagree
      // about is not an identity.
      if (!parts[i].matches("0|[1-9][0-9]{0,2}")) {
        return Optional.empty();
      }
      int octet = Integer.parseInt(parts[i]);
      if (octet > 255) {
        return Optional.empty();
      }
      bytes[i] = (byte) octet;
    }
    return Optional.of(new ParsedAddress(bytes, true));
  }

  private static String dotted(byte[] address) {
    return "%d.%d.%d.%d".formatted(
        address[0] & 0xFF, address[1] & 0xFF, address[2] & 0xFF, address[3] & 0xFF);
  }

  // ------------------------------------------------------------------ IPv6

  private static Optional<ParsedAddress> parseIpv6(String literal) {
    String candidate = literal.toLowerCase(Locale.ROOT);

    // A trailing dotted quad (`::ffff:203.0.113.7`) is legal RFC 4291 notation. Convert it to two
    // hex groups and continue, so there is exactly one representation downstream.
    int lastColon = candidate.lastIndexOf(':');
    if (lastColon >= 0 && candidate.indexOf('.', lastColon) > 0) {
      Optional<ParsedAddress> embedded = parseIpv4(candidate.substring(lastColon + 1));
      if (embedded.isEmpty()) {
        return Optional.empty();
      }
      byte[] v4 = embedded.get().bytes();
      candidate = candidate.substring(0, lastColon + 1)
          + "%04x:%04x".formatted(((v4[0] & 0xFF) << 8) | (v4[1] & 0xFF),
          ((v4[2] & 0xFF) << 8) | (v4[3] & 0xFF));
    }

    // At most one `::`, which may expand to one or more zero groups.
    int doubleColon = candidate.indexOf("::");
    if (doubleColon >= 0 && candidate.indexOf("::", doubleColon + 1) >= 0) {
      return Optional.empty();
    }

    String head = doubleColon >= 0 ? candidate.substring(0, doubleColon) : candidate;
    String tail = doubleColon >= 0 ? candidate.substring(doubleColon + 2) : "";
    List<Integer> headGroups = groups(head);
    List<Integer> tailGroups = groups(tail);
    if (headGroups == null || tailGroups == null) {
      return Optional.empty();
    }

    List<Integer> all = new ArrayList<>(headGroups);
    if (doubleColon >= 0) {
      int filler = 8 - headGroups.size() - tailGroups.size();
      if (filler < 1) {
        // `::` must stand for at least one group, otherwise the notation is ambiguous.
        return Optional.empty();
      }
      for (int i = 0; i < filler; i++) {
        all.add(0);
      }
    } else if (headGroups.size() != 8) {
      return Optional.empty();
    }
    all.addAll(tailGroups);
    if (all.size() != 8) {
      return Optional.empty();
    }

    byte[] bytes = new byte[16];
    for (int i = 0; i < 8; i++) {
      bytes[i * 2] = (byte) ((all.get(i) >> 8) & 0xFF);
      bytes[i * 2 + 1] = (byte) (all.get(i) & 0xFF);
    }

    // Fold RFC 4291 IPv4-mapped addresses (::ffff:0:0/96) to their IPv4 form. A dual-stack client
    // reaches the server both ways, so treating them as two networks gives one client two buckets.
    if (isIpv4Mapped(bytes)) {
      return Optional.of(new ParsedAddress(new byte[]{bytes[12], bytes[13], bytes[14], bytes[15]},
          true));
    }
    return Optional.of(new ParsedAddress(bytes, false));
  }

  /** @return the groups, or {@code null} when any group is not 1–4 hex digits */
  private static List<Integer> groups(String text) {
    List<Integer> groups = new ArrayList<>();
    if (text.isEmpty()) {
      return groups;
    }
    for (String group : text.split(":", -1)) {
      if (group.isEmpty() || group.length() > 4 || !group.matches("[0-9a-f]{1,4}")) {
        return null;
      }
      groups.add(Integer.parseInt(group, 16));
    }
    return groups;
  }

  private static boolean isIpv4Mapped(byte[] bytes) {
    for (int i = 0; i < 10; i++) {
      if (bytes[i] != 0) {
        return false;
      }
    }
    // bytes[10] and bytes[11] must both be 0xff for ::ffff:a.b.c.d
    return (bytes[10] & 0xFF) == 0xFF && (bytes[11] & 0xFF) == 0xFF;
  }

  /**
   * Fully expanded, lower-case form with every group zero-padded to four digits.
   *
   * <p>Not RFC 5952 canonical form ({@code ::}-compressed), deliberately: the value is a bucket key
   * input, so uniqueness and stability matter and readability does not. Expansion cannot collapse two
   * different networks into one string.</p>
   */
  private static String expandedIpv6(byte[] bytes) {
    StringBuilder builder = new StringBuilder(39);
    for (int i = 0; i < 8; i++) {
      if (i > 0) {
        builder.append(':');
      }
      builder.append("%04x".formatted(((bytes[i * 2] & 0xFF) << 8) | (bytes[i * 2 + 1] & 0xFF)));
    }
    return builder.toString();
  }

  // ------------------------------------------------------------------ value types

  /**
   * A packed address plus which family it belongs to.
   *
   * @param bytes exactly 4 bytes for IPv4, 16 for IPv6
   * @param ipv4  whether this is an IPv4 address
   */
  public record ParsedAddress(byte[] bytes, boolean ipv4) {

    public ParsedAddress {
      requireNonNull(bytes, "bytes must not be null");
      if (bytes.length != (ipv4 ? 4 : 16)) {
        throw new IllegalArgumentException(
            "expected %d bytes for %s, but got %d"
                .formatted(ipv4 ? 4 : 16, ipv4 ? "IPv4" : "IPv6", bytes.length));
      }
      bytes = bytes.clone();
    }

    @Override
    public byte[] bytes() {
      return bytes.clone();
    }

    /** @return whether this address is IPv4 (4 bytes) rather than IPv6 (16 bytes) */
    public boolean isIpv4() {
      return ipv4;
    }
  }

  /**
   * One CIDR range.
   *
   * @param base   the network address as written
   * @param prefix the number of leading bits that must match
   */
  public record CidrRange(ParsedAddress base, int prefix) {

    public CidrRange {
      requireNonNull(base, "base must not be null");
    }

    /**
     * @param address the address to test
     * @return whether {@code address} shares the first {@code prefix} bits with the base
     */
    public boolean contains(ParsedAddress address) {
      if (address.ipv4() != base.ipv4()) {
        return false;
      }
      byte[] candidate = address.bytes();
      byte[] network = base.bytes();
      int fullBytes = prefix / 8;
      for (int i = 0; i < fullBytes; i++) {
        if (candidate[i] != network[i]) {
          return false;
        }
      }
      int remainingBits = prefix % 8;
      if (remainingBits == 0) {
        return true;
      }
      int mask = 0xFF << (8 - remainingBits) & 0xFF;
      return (candidate[fullBytes] & mask) == (network[fullBytes] & mask);
    }
  }
}
