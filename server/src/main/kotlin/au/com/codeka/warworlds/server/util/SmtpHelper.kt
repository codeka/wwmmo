package au.com.codeka.warworlds.server.util

import au.com.codeka.warworlds.server.Configuration
import org.simplejavamail.email.Email
import org.simplejavamail.mailer.Mailer
import org.simplejavamail.mailer.config.ServerConfig
import org.simplejavamail.mailer.config.TransportStrategy
import java.util.*

/**
 * Helper class for sending [Email] messages.
 */
class SmtpHelper {
  private var mailer: Mailer? = null
  fun start() {
    mailer = Mailer(
        ServerConfig(
            Configuration.i.smtp.host, Configuration.i.smtp.port, Configuration.i.smtp.userName,
            Configuration.i.smtp.password),
        TransportStrategy.SMTP_TLS)
    val props = Properties()
    props["mail.smtp.localhost"] = "codeka.com.au"
    mailer!!.applyProperties(props)
  }

  fun stop() {}
  fun send(email: Email) {
    email.setFromAddress(Configuration.i.smtp.senderAddr, Configuration.i.smtp.senderAddr)
    mailer!!.sendMail(email, true)
  }

  companion object {
    val i = SmtpHelper()
  }
}