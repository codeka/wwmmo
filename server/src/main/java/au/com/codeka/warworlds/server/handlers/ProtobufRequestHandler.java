package au.com.codeka.warworlds.server.handlers;

import com.squareup.wire.Message;
import com.squareup.wire.ProtoAdapter;

import java.io.IOException;
import java.lang.reflect.Field;

import javax.servlet.http.HttpServletResponse;

import au.com.codeka.warworlds.common.Log;

/**
 * A {@link RequestHandler} with some extra helpers for responding with protobufs.
 */
public class ProtobufRequestHandler extends RequestHandler {
  private final Log log = new Log("ProtobufRequestHandler");

  protected <M extends Message<?, ?>> M readProtobuf(Class<? extends M> msgClass)
      throws RequestException {
    try {
      Field f = msgClass.getField("ADAPTER");
      ProtoAdapter<M> protoAdapter = (ProtoAdapter<M>) f.get(null);
      return protoAdapter.decode(getRequest().getInputStream());
    } catch (NoSuchFieldException | IOException | IllegalAccessException e) {
      throw new RequestException(400, "Error decoding request.", e);
    }
  }

  /** Writes the given protocol buffer to the given {@link HttpServletResponse}. */
  protected <M extends Message<M, B>, B extends Message.Builder<M, B>> void writeProtobuf(
      Message<M, B> msg) {
    byte[] bytes = msg.encode();
    getResponse().setHeader("Content-Type", "application/x-protobuf");
    getResponse().setHeader("Content-Length", Integer.toString(bytes.length));
    try {
      getResponse().getOutputStream().write(bytes);
    } catch (IOException e) {
      log.warning("Error sending protocol buffer to client.", e);
      getResponse().setStatus(500);
    }
  }
}
