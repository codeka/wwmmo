package au.com.codeka.warworlds.server.util;

import org.simplejavamail.email.Email;
import org.simplejavamail.mailer.Mailer;
import org.simplejavamail.mailer.config.ServerConfig;
import org.simplejavamail.mailer.config.TransportStrategy;

import java.util.Properties;

import au.com.codeka.warworlds.server.Configuration;

/**
 * Helper class for sending {@link Email} messages.
 */
public class SmtpHelper {
  public static final SmtpHelper i = new SmtpHelper();

  private Mailer mailer;

  public void start() {
    mailer = new Mailer(
        new ServerConfig(
            Configuration.i.getSmtp().getHost(),
            Configuration.i.getSmtp().getPort(),
            Configuration.i.getSmtp().getUserName(),
            Configuration.i.getSmtp().getPassword()),
        TransportStrategy.SMTP_TLS);

    Properties props = new Properties();
    props.put("mail.smtp.localhost", "codeka.com.au");
    mailer.applyProperties(props);
  }

  public void stop() {
  }

  public void send(Email email) {
    email.setFromAddress(
        Configuration.i.getSmtp().getSenderAddr(),
        Configuration.i.getSmtp().getSenderAddr());
    mailer.sendMail(email, true);
  }
}
