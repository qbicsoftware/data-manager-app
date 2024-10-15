package life.qbic.datamanager.export.serializer;

import life.qbic.datamanager.export.model.isa.Investigation;

/**
 * <b>Serializer interface for various export formats</b>
 *
 * @since 1.5.0
 */
public interface Serializer {

  String serialize(Investigation investigation) throws SerializationException;

  class SerializationException extends RuntimeException {
    public SerializationException(String message, Throwable cause) {}
  }

}
