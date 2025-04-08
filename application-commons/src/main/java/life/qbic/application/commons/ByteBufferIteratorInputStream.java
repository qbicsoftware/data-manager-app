package life.qbic.application.commons;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Objects;

/**
 * An input stream generated from a (lazy) iterator of {@link ByteBuffer}s.
 */
public class ByteBufferIteratorInputStream extends InputStream {

  private final Iterator<ByteBuffer> bufferIterator;
  private ByteBuffer currentBuffer;

  public ByteBufferIteratorInputStream(Iterator<ByteBuffer> iterator) {
    this.bufferIterator = iterator;
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    Objects.checkFromIndexSize(off, len, b.length);
    if (len == 0) {
      return 0;
    }
    boolean bufferAvailable = makeBufferAvailable();
    if (!bufferAvailable) {
      return -1;
    }

    int requestedBytes = Math.min(currentBuffer.remaining(), len);
    currentBuffer.get(currentBuffer.position(), b, off, requestedBytes);
    currentBuffer.position(currentBuffer.position() + requestedBytes);

    return requestedBytes;
  }

  @Override
  public int read() {
    boolean bufferAvailable = makeBufferAvailable();
    if (!bufferAvailable) {
      return -1;
    }
    var currentPosition = currentBuffer.position();
    var value = currentBuffer.get(currentPosition) & 0xFF;
    currentBuffer.position(currentPosition + 1);
    return value;
  }

  /**
   * Ensures a readable buffer is set.
   *
   * @return true if a readable buffer is selected; false if no readable buffer is available.
   */
  private boolean makeBufferAvailable() {
    var buffer = currentBuffer;
    if (buffer != null && !buffer.hasRemaining()) {
      // the current buffer is read completely
      buffer = null;
    }
    while (buffer == null) {
      if (!bufferIterator.hasNext()) {
        return false;
      }
      buffer = bufferIterator.next();
    }
    currentBuffer = buffer;
    return true;
  }
}
