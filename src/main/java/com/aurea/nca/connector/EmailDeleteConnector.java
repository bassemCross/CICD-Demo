
package com.aurea.nca.connector;

import java.util.Arrays;
import java.util.function.Predicate;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import org.apache.log4j.Logger;
import com.aurea.nca.connector.service.EmailService;
import com.aurea.nca.connector.service.impl.EmailServiceImpl;

public class EmailDeleteConnector {

  private final Logger log = Logger.getLogger(this.getClass());

  /**
   * Constructor for a DeleteEmailConnector
   */
  public EmailDeleteConnector() {

  }

  /**
   * Called by the container on service initialization or re-load Provides access to XQ APIs for
   * this service
   */
  private com.sonicsw.xq.XQInitContext m_initialContext;

  public void init(com.sonicsw.xq.XQInitContext initialContext) {
    m_initialContext = initialContext;
  }

  /**
   * Called by the container on service start.
   */
  public void start() {}

  /**
   * Called by the container on service stop.
   */
  public void stop() {}

  /**
   * Called by the container on service destroy and unload. Clean up and get ready to destroy the
   * service.
   */
  public void destroy() {}


  /**
   * setters/getters for init parameters in DeleteEmailConnector
   */


  /**
   * Description for init parameter host
   *
   */
  private String m_host;

  public void setHost(String p_host) {
    m_host = p_host;
  }

  public String getHost() {
    return m_host;
  }

  /**
   * Description for init parameter port
   *
   */
  private int m_port;

  public void setPort(int p_port) {
    m_port = p_port;
  }

  public int getPort() {
    return m_port;
  }

  /**
   * Description for init parameter user
   *
   */
  private String m_user;

  public void setUser(String p_user) {
    m_user = p_user;
  }

  public String getUser() {
    return m_user;
  }

  /**
   * Description for init parameter password
   *
   */
  private String m_password;

  public void setPassword(String p_password) {
    m_password = p_password;
  }

  public String getPassword() {
    return m_password;
  }

  /**
   * Description for init parameter ssl
   *
   */
  private boolean m_ssl;

  public void setSsl(boolean p_ssl) {
    m_ssl = p_ssl;
  }

  public boolean getSsl() {
    return m_ssl;
  }


  /** Service operations callable from ESB itineraries. */

  /**
   * Description for operation deleteByMessageId
   *
   */

  public String deleteByMessageId(String messageId) {
    log.debug("Delete Message Id:" + messageId);
    EmailService emailService = new EmailServiceImpl();
    Predicate<Message> messageFilter = m -> {
      String mId;
      try {
        String[] messageIds = m.getHeader("Message-ID");
        if (messageIds.length > 0) {
          mId = m.getHeader("Message-ID")[0].toString();
          log.debug(
              "compare Email Message Id: " + mId + " with input param Message Id:" + messageId);
          return messageId != null && messageId.equals(mId);
        }
        return false;
      } catch (MessagingException e) {
        log.error(e.getMessage(), e);
        return false;
      }
    };
    emailService.clearFilteredInboxEmails(getHost(), getUser(), getPassword(), getPort(),
        messageFilter);
    return Boolean.TRUE.toString();
  }



  /**
   * Description for operation deleteByFromAndTo
   *
   */

  public String deleteByFromAndTo(String from, String to) {

    EmailService emailService = new EmailServiceImpl();
    Predicate<Message> messageFilter = m -> {
      try {
        return (from != null && !from.isEmpty()
            && Arrays.stream(m.getFrom()).map(Address::toString).distinct()
                .filter(f -> f.equals(from)).findAny().isPresent())
            && (to != null && !to.isEmpty() && Arrays.stream(m.getRecipients(RecipientType.TO))
                .map(Address::toString).distinct().filter(t -> t.equals(to)).findAny().isPresent());
      } catch (MessagingException e) {
        return false;
      }
    };
    emailService.clearFilteredInboxEmails(getHost(), getUser(), getPassword(), getPort(),
        messageFilter);
    return Boolean.TRUE.toString();
  }



}
