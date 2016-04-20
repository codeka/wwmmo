package au.com.codeka.warworlds.client;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFrame;
import com.neovisionaries.ws.client.WebSocketListener;
import com.neovisionaries.ws.client.WebSocketState;

import java.util.List;
import java.util.Map;

import au.com.codeka.warworlds.common.Log;

/**
 * Implementation of {@link WebSocketListener} that we use to listen for messages from the server
 */
public class ServerWebSocketListener implements WebSocketListener {
  private static final Log log = new Log("WS");
  @Override
  public void onStateChanged(
      WebSocket websocket,
      WebSocketState newState) throws Exception {
    log.debug("onStateChanged(%s)", newState);
  }

  @Override
  public void onConnected(
      WebSocket websocket,
      Map<String, List<String>> headers) throws Exception {
    log.debug("onConnected()");
    websocket.sendText("Hello, World");
  }

  @Override
  public void onConnectError(
      WebSocket websocket,
      WebSocketException cause) throws Exception {
    log.debug("onConnectError(%s)", cause.getMessage());
  }

  @Override
  public void onDisconnected(
      WebSocket websocket,
      WebSocketFrame serverCloseFrame,
      WebSocketFrame clientCloseFrame,
      boolean closedByServer) throws Exception {
    log.debug("onDisconnected(%s /* closedByServer */)", closedByServer ? "true" : "false");
  }

  @Override
  public void onFrame(
      WebSocket websocket,
      WebSocketFrame frame) throws Exception {
    log.debug("onFrame(%s)", frame);
  }

  @Override
  public void onContinuationFrame(
      WebSocket websocket,
      WebSocketFrame frame) throws Exception {
    log.debug("onContinuationFrame(%s)", frame);
  }

  @Override
  public void onTextFrame(
      WebSocket websocket,
      WebSocketFrame frame) throws Exception {
    log.debug("onTextFrame(%s)", frame);
  }

  @Override
  public void onBinaryFrame(
      WebSocket websocket,
      WebSocketFrame frame) throws Exception {
    log.debug("onBinaryFrame(%s)", frame);
  }

  @Override
  public void onCloseFrame(
      WebSocket websocket,
      WebSocketFrame frame) throws Exception {
    log.debug("onCloseFrame(%s)", frame);
  }

  @Override
  public void onPingFrame(
      WebSocket websocket,
      WebSocketFrame frame) throws Exception {
    log.debug("onPingFrame(%s)", frame);
  }

  @Override
  public void onPongFrame(
      WebSocket websocket,
      WebSocketFrame frame) throws Exception {
    log.debug("onPongFrame(%s)", frame);
  }

  @Override
  public void onTextMessage(
      WebSocket websocket,
      String text) throws Exception {
    log.debug("onTextMessage(%s)", text);
  }

  @Override
  public void onBinaryMessage(
      WebSocket websocket,
      byte[] binary) throws Exception {
    log.debug("onBinaryMessage(%d bytes)", binary.length);
  }

  @Override
  public void onSendingFrame(
      WebSocket websocket,
      WebSocketFrame frame) throws Exception {
    log.debug("onSendingFrame(%s)", frame);
  }

  @Override
  public void onFrameSent(
      WebSocket websocket,
      WebSocketFrame frame) throws Exception {
    log.debug("onFrameSent(%s)", frame);
  }

  @Override
  public void onFrameUnsent(
      WebSocket websocket,
      WebSocketFrame frame) throws Exception {
    log.debug("onFrameUnsent(%s)", frame);
  }

  @Override
  public void onError(
      WebSocket websocket,
      WebSocketException cause) throws Exception {
    log.debug("onError(%s)", cause);
  }

  @Override
  public void onFrameError(
      WebSocket websocket,
      WebSocketException cause,
      WebSocketFrame frame) throws Exception {
    log.debug("onFrameError(%s)", cause);
  }

  @Override
  public void onMessageError(
      WebSocket websocket,
      WebSocketException cause,
      List<WebSocketFrame> frames) throws Exception {
    log.debug("onMessageError(%s)", cause);
  }

  @Override
  public void onMessageDecompressionError(
      WebSocket websocket,
      WebSocketException cause,
      byte[] compressed) throws Exception {
  }

  @Override
  public void onTextMessageError(
      WebSocket websocket,
      WebSocketException cause,
      byte[] data) throws Exception {
  }

  @Override
  public void onSendError(
      WebSocket websocket,
      WebSocketException cause,
      WebSocketFrame frame) throws Exception {
  }

  @Override
  public void onUnexpectedError(
      WebSocket websocket,
      WebSocketException cause) throws Exception {
  }

  @Override
  public void handleCallbackError(
      WebSocket websocket,
      Throwable cause) throws Exception {
  }

  @Override
  public void onSendingHandshake(
      WebSocket websocket,
      String requestLine,
      List<String[]> headers) throws Exception {
  }
}
