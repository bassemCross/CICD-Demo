package com.aurea.nca.connector;

import static com.aurea.nca.connector.util.EmailConstants.IMAP;
import static org.junit.Assert.assertEquals;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.aurea.nca.connector.model.Email;
import com.aurea.nca.connector.service.EmailService;
import com.aurea.nca.connector.service.impl.EmailServiceImpl;

public class EmailDeleteConnectorIT {
  private static final int SLEEP = 5000;
  private static final String IMAP_HOST = "imap.gmail.com";
  private static final String SMTP_HOST = "smtp.gmail.com";
  private static final String USER = "nca_test@aurea.com";
  private static final String PASSWORD = "orbxhjoxkfypfmkj";
  private static final String FROM = "nca_test@aurea.com";
  private static final String TO = "nca_test@aurea.com";
  private static final String SUBJECT = "test";
  private static final String TEXT = "test";
  private static final String ATTACH_FILE_NAME = "test.csv";
  private static final String ATTACH_FILE_CONTENT_HEADER = "c1,c2,c3,UUID";
  private static final String ATTACH_FILE_CONTENT = "test,test,test";
  private static final int IMAP_PORT = 993;
  private static final int SMTP_PORT = 587;
  private static final boolean AUTH = true;
  private static final boolean TLS = true;
  private static final boolean SSL = true;

  EmailService service = new EmailServiceImpl();
  EmailDeleteConnector connector = new EmailDeleteConnector();

  @Before
  public void setup() {
    Predicate<Message> messageFilter = m -> {
      try {
        return m.getFrom()[0].toString().equals(FROM)
            && m.getRecipients(RecipientType.TO)[0].toString().equals(TO);
      } catch (MessagingException e) {
        return false;
      }
    };
    service.clearFilteredInboxEmails(IMAP_HOST, USER, PASSWORD, IMAP_PORT, messageFilter);
    connector.setHost(IMAP_HOST);
    connector.setUser(USER);
    connector.setPassword(PASSWORD);
    connector.setPort(IMAP_PORT);
  }

  @After
  public void teardown() throws InterruptedException {
  }

  @Test
  public final void deleteMessageByIdTest()
      throws InterruptedException, IOException, MessagingException {
    String token = UUID.randomUUID().toString();


    service.send(SMTP_HOST, USER, PASSWORD, SMTP_PORT, FROM, TO, SUBJECT + token, TEXT + token,
        Optional.empty(), AUTH, TLS);
    Thread.sleep(SLEEP);
    List<Email> emails =
        service.getAttachments(IMAP_HOST, USER, PASSWORD, IMAP_PORT, IMAP, SSL, false);
    assertEquals(1, emails.size());
    connector.deleteByMessageId(emails.get(0).getMessageId());
    emails = service.getAttachments(IMAP_HOST, USER, PASSWORD, IMAP_PORT, IMAP, SSL, true);
    assertEquals(0, emails.size());
  }

}
