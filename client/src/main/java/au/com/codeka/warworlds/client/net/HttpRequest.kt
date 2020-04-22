package au.com.codeka.warworlds.client.net

import au.com.codeka.warworlds.client.util.GameSettings
import au.com.codeka.warworlds.common.Log
import com.google.common.io.ByteStreams
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

/**
 * Simple wrapper around [HttpURLConnection] that lets us make HTTP requests more easily.
 */
class HttpRequest private constructor(builder: Builder) {
  enum class Method {
    GET, POST, PUT, DELETE
  }

  private val url = builder.url
  private var conn: HttpURLConnection? = null
  var exception: IOException? = null
    private set

  private var body: ByteArray? = null
    get() {
      if (exception != null || responseCode != 200) {
        return null
      }
      if (field == null) {
        try {
          field = ByteStreams.toByteArray(conn!!.inputStream)
        } catch (e: IOException) {
          log.warning("Error fetching '%s'", url, e)
          exception = e
          return null
        }
      }
      return field
    }

  val responseCode: Int
    get() = if (exception != null) {
      -1
    } else try {
      conn!!.responseCode
    } catch (e: IOException) {
      log.warning("Error fetching '%s'", url, e)
      exception = e
      -1
    }

  /** Simple wrapper around [.getBody] that casts to a proto message.  */
  fun <T : Message<*, *>?> getBody(protoType: Class<T>): T? {
    val bytes = body ?: return null
    return try {
      val adapterField = protoType.getField("ADAPTER")
      val adapter = adapterField[null] as ProtoAdapter<T>
      adapter.decode(bytes)
    } catch (e: IOException) {
      exception = e
      log.warning("Error fetching '%s'", url, e)
      null
    } catch (e: Exception) {
      log.warning("Error fetching '%s'", url, e)
      null
    }
  }

  class Builder {
    var url: String = ""
    var method: Method
    val headers: HashMap<String, String>
    var body: ByteArray? = null
    var authenticated = false

    fun url(url: String): Builder {
      this.url = url
      return this
    }

    fun method(method: Method): Builder {
      this.method = method
      return this
    }

    fun header(name: String, value: String): Builder {
      headers[name] = value
      return this
    }

    fun authenticated(): Builder {
      authenticated = true
      return this
    }

    fun body(body: ByteArray?): Builder {
      this.body = body
      return this
    }

    fun build(): HttpRequest {
      return HttpRequest(this)
    }

    init {
      method = Method.GET
      headers = HashMap()
    }
  }

  companion object {
    private val log = Log("HttpRequest")
  }

  init {
    log.info("HTTP %s %s (%d bytes)", builder.method, builder.url,
        if (builder.body == null) 0 else builder.body!!.size)
    try {
      val url = URL(builder.url)
      conn = url.openConnection() as HttpURLConnection
      conn!!.requestMethod = builder.method.toString()
      if (builder.authenticated) {
        val cookie = GameSettings.i.getString(GameSettings.Key.COOKIE)
        conn!!.setRequestProperty("COOKIE", cookie)
      }
      for (key in builder.headers.keys) {
        conn!!.setRequestProperty(key, builder.headers[key])
      }
      if (builder.body != null) {
        conn!!.setRequestProperty("Content-Length", Integer.toString(builder.body!!.size))
        conn!!.outputStream.write(builder.body)
      }
    } catch (e: IOException) {
      log.warning("Error fetching '%s'", builder.url, e)
      exception = e
    }
  }
}