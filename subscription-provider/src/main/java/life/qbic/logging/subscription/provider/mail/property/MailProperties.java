package life.qbic.logging.subscription.provider.mail.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <b>Mail Properties</b>
 * <p>
 * Binds the e-mail configuration from the application properties to a strongly typed object.
 * <p>
 * The properties are read from the prefix {@code qbic.logging.mail}.
 *
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "qbic.logging.mail")
public class MailProperties {

  private String host;

  private int port;

  private String username;

  private String password;

  private boolean smtpAuth = true;

  private boolean startTls = true;

  private String sender;

  private String recipient;

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public boolean isSmtpAuth() {
    return smtpAuth;
  }

  public void setSmtpAuth(boolean smtpAuth) {
    this.smtpAuth = smtpAuth;
  }

  public boolean isStartTls() {
    return startTls;
  }

  public void setStartTls(boolean startTls) {
    this.startTls = startTls;
  }

  public String getSender() {
    return sender;
  }

  public void setSender(String sender) {
    this.sender = sender;
  }

  public String getRecipient() {
    return recipient;
  }

  public void setRecipient(String recipient) {
    this.recipient = recipient;
  }
}