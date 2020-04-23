package au.com.codeka.warworlds.server.handlers

import au.com.codeka.warworlds.common.Log
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import java.io.IOException
import javax.servlet.http.HttpServletResponse

/** A [RequestHandler] with some extra helpers for responding with protobufs. */
open class ProtobufRequestHandler : RequestHandler() {
  private val log = Log("ProtobufRequestHandler")

  protected fun <M : Message<*, *>?> readProtobuf(msgClass: Class<out M>): M {
    val f = msgClass.getField("ADAPTER")
    val protoAdapter = f[null] as ProtoAdapter<M>
    return protoAdapter.decode(request.inputStream)
  }

  /** Writes the given protocol buffer to the given [HttpServletResponse].  */
  protected fun <M : Message<M, B>, B : Message.Builder<M, B>> writeProtobuf(msg: M) {
    val bytes = msg.encode()
    response.setHeader("Content-Type", "application/x-protobuf")
    response.setHeader("Content-Length", bytes.size.toString())
    try {
      response.outputStream.write(bytes)
    } catch (e: IOException) {
      log.warning("Error sending protocol buffer to client.", e)
      response.status = 500
    }
  }
}
