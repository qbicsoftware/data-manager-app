package life.qbic.datamanager.configuration

import java.util.concurrent.CopyOnWriteArrayList

/**
 * <b>A socket-level SMTP endpoint, so the mail stack can be tested against real protocol replies.</b>
 *
 * <p>No mock, because the behaviour under test <em>is</em> the interaction with a server: whether the
 * mail stack offers credentials, whether it demands STARTTLS when {@code mail.smtp.starttls.required}
 * is set, and what it does when a relay offers neither. Mocking {@code JavaMailSenderImpl} would
 * assert what I believe that interaction to be rather than what it is.</p>
 *
 * <p>Deliberately tiny: it speaks just enough SMTP for the stack to get through EHLO and reach the
 * interesting decision. It never stores a message and it never completes a TLS handshake — see
 * {@link Behaviour#STARTTLS}. That limitation is stated here rather than papered over: the cases
 * that matter (a rejected password, a relay that will not upgrade) are reachable without a real
 * handshake, and the success path is covered against a genuinely TLS-capable server only if one is
 * ever needed.</p>
 */
class FakeSmtpServer implements Closeable {

  enum Behaviour {
    /** Offers AUTH PLAIN and LOGIN, and rejects credentials that do not match. */
    AUTH_REQUIRED,

    /**
     * Advertises the STARTTLS extension, then refuses the command.
     *
     * <p>This is how the test can observe that the client <em>asked</em>: the extension is in the
     * EHLO reply, so a client that has {@code starttls.required} set will send {@code STARTTLS}, and
     * that command shows up in {@link #getCommands()}. Refusing afterwards is the behaviour of a
     * partly-configured relay — the client then fails, which is correct and is not what these specs
     * assert on.</p>
     */
    STARTTLS_ADVERTISED,
  }

  /** Credentials the server will accept, as [username, password]. */
  List<String> expectedCredentials

  private final ServerSocket serverSocket
  private final List<String> receivedCommands = new CopyOnWriteArrayList<>()
  private final List<Thread> handlers = new CopyOnWriteArrayList<>()
  private volatile boolean authSucceeded
  private volatile boolean closed

  private FakeSmtpServer(ServerSocket serverSocket) {
    this.serverSocket = serverSocket
  }

  static FakeSmtpServer start(List<Behaviour> behaviours) {
    def server = new FakeSmtpServer(new ServerSocket(0, 50, java.net.InetAddress.getByName('127.0.0.1')))
    def thread = Thread.ofVirtual().name("fake-smtp-${server.port}").start {
      server.acceptLoop(behaviours)
    }
    server.handlers << thread
    return server
  }

  int getPort() {
    return serverSocket.localPort
  }

  /** Every command line the client sent, in order, so a spec can assert STARTTLS was attempted. */
  List<String> getCommands() {
    return List.copyOf(receivedCommands)
  }

  boolean isSawAuthentication() {
    return authSucceeded
  }

  @Override
  void close() {
    closed = true
    try { serverSocket.close() } catch (IOException ignored) { }
    handlers.each { it.interrupt() }
  }

  // ---- protocol ---------------------------------------------------------------------------------

  private void acceptLoop(List<Behaviour> behaviours) {
    while (!closed) {
      try {
        def socket = serverSocket.accept()
        Thread.ofVirtual().name("fake-smtp-conn").start {
          try {
            serve(socket, behaviours)
          } finally {
            try { socket.close() } catch (IOException ignored) { }
          }
        }
      } catch (IOException ignored) {
        return // socket closed; shutting down
      }
    }
  }

  private void serve(Socket socket, List<Behaviour> behaviours) {
    def reader = new BufferedReader(new InputStreamReader(socket.inputStream, 'US-ASCII'))
    def writer = new PrintWriter(new OutputStreamWriter(socket.outputStream, 'US-ASCII'), true)
    writer.print("220 fake-smtp ready\r\n")
    writer.flush()

    String line
    while ((line = reader.readLine()) != null) {
      receivedCommands << line
      def command = line.toUpperCase()
      if (command.startsWith('EHLO') || command.startsWith('HELO')) {
        writer.print(ehloReply(behaviours))
        writer.flush()
      } else if (command.startsWith('STARTTLS')) {
        // Advertised, then refused — see STARTTLS_ADVERTISED.
        writer.print("454 4.7.0 TLS not available\r\n")
        writer.flush()
      } else if (command.startsWith('AUTH')) {
        handleAuth(line, reader, writer, behaviours)
      } else if (command.startsWith('QUIT')) {
        writer.print("221 bye\r\n")
        writer.flush()
        return
      } else {
        writer.print("250 ok\r\n")
        writer.flush()
      }
    }
  }

  private static String ehloReply(List<Behaviour> behaviours) {
    def lines = ['250-fake-smtp greets you']
    if (behaviours.contains(Behaviour.AUTH_REQUIRED)) {
      lines << '250-AUTH PLAIN LOGIN'
    }
    if (behaviours.contains(Behaviour.STARTTLS_ADVERTISED)) {
      lines << '250-STARTTLS'
    }
    lines << '250 8BITMIME'
    return lines.join("\r\n") + "\r\n"
  }

  /**
   * Accepts a correct AUTH PLAIN or AUTH LOGIN exchange, rejects anything else.
   *
   * <p>Correct credentials are what make the TLS-only failure case meaningful: when the password is
   * right and the check still fails, the reason cannot be the password.</p>
   */
  private void handleAuth(String line, BufferedReader reader, PrintWriter writer,
      List<Behaviour> behaviours) {
    if (!behaviours.contains(Behaviour.AUTH_REQUIRED)) {
      writer.print("503 authentication not enabled\r\n")
      writer.flush()
      return
    }
    def parts = line.split(' ')
    def mechanism = parts.length > 1 ? parts[1].toUpperCase() : ''
    def supplied = []

    if (mechanism == 'PLAIN') {
      supplied = decodePlain(parts.length > 2 ? parts[2] : '')
    } else if (mechanism == 'LOGIN') {
      writer.print("334 VXNlcm5hbWU6\r\n")
      writer.flush()
      def userLine = reader.readLine()
      if (userLine != null) { receivedCommands << userLine }
      supplied << decode(userLine ?: '')
      writer.print("334 UGFzc3dvcmQ6\r\n")
      writer.flush()
      def passLine = reader.readLine()
      if (passLine != null) { receivedCommands << passLine }
      supplied << decode(passLine ?: '')
    }

    if (credentialsMatch(supplied)) {
      authSucceeded = true
      writer.print("235 2.7.0 Authentication successful\r\n")
    } else {
      writer.print("535 5.7.8 Authentication credentials invalid\r\n")
    }
    writer.flush()
  }

  private boolean credentialsMatch(List<String> supplied) {
    if (expectedCredentials == null || supplied == null || supplied.size() < 2) {
      return false
    }
    return supplied[0] == expectedCredentials[0] && supplied[1] == expectedCredentials[1]
  }

  /** AUTH PLAIN payload is base64 of NUL user NUL pass. */
  private static List<String> decodePlain(String payload) {
    if (!payload) {
      return []
    }
    def fields = decode(payload).split('\u0000', -1)
    return fields.length >= 3 ? [fields[1], fields[2]] : []
  }

  private static String decode(String base64) {
    try {
      return new String(Base64.decoder.decode(base64), 'UTF-8')
    } catch (IllegalArgumentException ignored) {
      return ''
    }
  }
}
