package au.com.codeka.warworlds.server;

import com.squareup.wire.Message;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;

import au.com.codeka.warworlds.common.Log;

/**
 * A {@link HttpServlet} with some extra helpers for responding with protobufs.
 */
public class ProtobufHttpServlet extends HttpServlet {
  private final Log log = new Log("ProtobufHttpServlet");

  /** Writes the given protocol buffer to the given {@link HttpServletResponse}. */
  protected <M extends Message<M, B>, B extends Message.Builder<M, B>> void writeProtobuf(
      HttpServletResponse response, Message<M, B> msg) {
    byte[] bytes = msg.encode();
    response.setHeader("Content-Type", "application/x-protobuf");
    response.setHeader("Content-Length", Integer.toString(bytes.length));
    try {
      response.getOutputStream().write(bytes);
    } catch (IOException e) {
      log.warning("Error sending protocol buffer to client.", e);
      response.setStatus(500);
    }
  }
}
