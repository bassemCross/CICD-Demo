package com.aurea.nca.connector.service.impl;

import static com.aurea.nca.connector.util.EmailConstants.IMAP;
import static com.aurea.nca.connector.util.EmailConstants.IMAPS;
import static com.aurea.nca.connector.util.EmailConstants.INBOX;
import static com.aurea.nca.connector.util.EmailConstants.MAIL_IMAPS_AUTH;
import static com.aurea.nca.connector.util.EmailConstants.MAIL_IMAPS_HOST;
import static com.aurea.nca.connector.util.EmailConstants.MAIL_IMAPS_PASSWORD;
import static com.aurea.nca.connector.util.EmailConstants.MAIL_IMAPS_PORT;
import static com.aurea.nca.connector.util.EmailConstants.MAIL_IMAPS_USER;
import static com.aurea.nca.connector.util.EmailConstants.MAIL_IMAP_SSL_ENABLE;
import static com.aurea.nca.connector.util.EmailConstants.MAIL_POP3_HOST;
import static com.aurea.nca.connector.util.EmailConstants.MAIL_POP3_PORT;
import static com.aurea.nca.connector.util.EmailConstants.MAIL_POP3_SSL_ENABLE;
import static com.aurea.nca.connector.util.EmailConstants.MAIL_SMTP_AUTH;
import static com.aurea.nca.connector.util.EmailConstants.MAIL_SMTP_HOST;
import static com.aurea.nca.connector.util.EmailConstants.MAIL_SMTP_PORT;
import static com.aurea.nca.connector.util.EmailConstants.MAIL_SMTP_STARTTLS_ENABLE;
import static com.aurea.nca.connector.util.EmailConstants.MAIL_STORE_PROTOCOL;
import static com.aurea.nca.connector.util.EmailConstants.POP3;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import org.apache.log4j.Logger;
import com.aurea.nca.connector.model.Email;
import com.aurea.nca.connector.model.EmailAttachment;
import com.aurea.nca.connector.service.EmailService;

public class EmailServiceImpl implements EmailService {

  private final Logger log = Logger.getLogger(this.getClass());

  @Override
  public void send(String host, String user, String password, int port, String from, String to,
      String subject, String text, Optional<File> attachment, boolean auth, boolean tls)
      throws MessagingException {
    Properties properties = new Properties();
    properties.put(MAIL_SMTP_HOST, host);
    properties.put(MAIL_SMTP_PORT, port);
    properties.put(MAIL_SMTP_AUTH, auth);
    properties.put(MAIL_SMTP_STARTTLS_ENABLE, tls);
    Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(user, password);
      }
    });

    Message message = new MimeMessage(session);
    message.setFrom(new InternetAddress(from));
    message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
    message.setSubject(subject);
    if (attachment.isPresent()) {
      File f = attachment.get();
      Multipart multipart = new MimeMultipart();
      MimeBodyPart attachmentPart = new MimeBodyPart();
      MimeBodyPart textPart = new MimeBodyPart();
      try {
        attachmentPart.attachFile(f);
        textPart.setText(text);
        multipart.addBodyPart(textPart);
        multipart.addBodyPart(attachmentPart);
        message.setContent(multipart);
      } catch (IOException | MessagingException e) {
        throw new RuntimeException(e);
      }
    } else {
      message.setText(text);
    }
    Transport.send(message);
  }

  @Override
  public void clearAllInboxEmails(String imapHost, String user, String password, int port) {
    Properties props = new Properties();
    props.put(MAIL_STORE_PROTOCOL, IMAPS);
    props.put(MAIL_IMAPS_HOST, imapHost);
    props.put(MAIL_IMAPS_USER, user);
    props.put(MAIL_IMAPS_PASSWORD, password);
    props.put(MAIL_IMAPS_PORT, port);
    props.put(MAIL_IMAP_SSL_ENABLE, true);
    props.put(MAIL_IMAPS_AUTH, true);
    Session session = Session.getDefaultInstance(props);
    Store store = null;
    try {
      store = session.getStore(IMAP);
      store.connect(imapHost, user, password);
      deleteInboxMessages(store);
    } catch (MessagingException e) {
      log.error(e.getMessage(), e);
    } finally {
      if (store != null) {
        try {
          store.close();
        } catch (MessagingException e) {
          log.error(e.getMessage(), e);
        }
      }
    }
  }

  @Override
  public void clearFilteredInboxEmails(String imapHost, String user, String password, int port,
      Predicate<Message> messageFilter) {
    Properties props = new Properties();
    props.put(MAIL_STORE_PROTOCOL, IMAPS);
    props.put(MAIL_IMAPS_HOST, imapHost);
    props.put(MAIL_IMAPS_USER, user);
    props.put(MAIL_IMAPS_PASSWORD, password);
    props.put(MAIL_IMAPS_PORT, port);
    props.put(MAIL_IMAP_SSL_ENABLE, true);
    props.put(MAIL_IMAPS_AUTH, true);
    Session session = Session.getDefaultInstance(props);
    Store store = null;
    try {
      store = session.getStore(IMAP);
      store.connect(imapHost, user, password);
      deleteInboxMessages(store, messageFilter);
    } catch (MessagingException e) {
      log.error(e.getMessage(), e);
    } finally {
      if (store != null) {
        try {
          store.close();
        } catch (MessagingException e) {
          log.error(e.getMessage(), e);
        }
      }
    }
  }

  @Override
  public List<Email> getAttachments(String host, String user, String password, int port,
      String protocol, boolean ssl, boolean processSeen) {
    log.debug("Get Attachment from Host: " + host + " on port: " + port
        + " with protocol: " + protocol + " with user: " + user + " And SSL: " + ssl);
    Properties props = new Properties();
    if (POP3.equalsIgnoreCase(protocol)) {
      props.put(MAIL_POP3_HOST, host);
      props.put(MAIL_POP3_PORT, port);
      props.put(MAIL_STORE_PROTOCOL, protocol);
      props.put(MAIL_POP3_SSL_ENABLE, ssl);
    } else if (IMAP.equalsIgnoreCase(protocol)) {
      props.put(MAIL_STORE_PROTOCOL, IMAPS);
      props.put(MAIL_IMAPS_HOST, host);
      props.put(MAIL_IMAPS_USER, user);
      props.put(MAIL_IMAPS_PASSWORD, password);
      props.put(MAIL_IMAPS_PORT, port);
      props.put(MAIL_IMAP_SSL_ENABLE, ssl);
      props.put(MAIL_IMAPS_AUTH, true);
    }

    Session session = Session.getInstance(props);
    Store store = null;
    try {
      store = session.getStore(protocol);
      store.connect(host, user, password);
      return processMessages(store, processSeen);
    } catch (MessagingException e) {
      log.error(e.getMessage(), e);
      return new ArrayList();
    } finally {
      if (store != null) {
        try {
          store.close();
        } catch (MessagingException e) {
          log.error(e.getMessage(), e);
        }
      }
    }
  }

  private void deleteInboxMessages(Store store) throws MessagingException {
    Folder emailFolder = null;
    try {
      emailFolder = store.getFolder(INBOX);
      emailFolder.open(Folder.READ_WRITE);
      Arrays.stream(emailFolder.getMessages()).forEach(m -> {
        try {
          m.setFlag(Flag.DELETED, true);
        } catch (MessagingException e) {
          log.error(e.getMessage(), e);
        }
      });
    } finally {
      if (emailFolder != null) {
        try {
          emailFolder.close(true);
        } catch (final MessagingException e) {
          log.error(e.getMessage(), e);
        }
      }
    }
  }

  private void deleteInboxMessages(Store store, Predicate<Message> messageFilter)
      throws MessagingException {
    Folder emailFolder = null;
    try {
      emailFolder = store.getFolder(INBOX);
      emailFolder.open(Folder.READ_WRITE);
      Arrays.stream(emailFolder.getMessages()).filter(messageFilter).forEach(m -> {
        try {
          m.setFlag(Flag.DELETED, true);
        } catch (MessagingException e) {
          log.error(e.getMessage(), e);
        }
      });
    } finally {
      if (emailFolder != null) {
        try {
          emailFolder.close(true);
        } catch (final MessagingException e) {
          log.error(e.getMessage(), e);
        }
      }
    }
  }

  private List<Email> processMessages(Store store, boolean processSeen) throws MessagingException {
    final List<Email> list = new ArrayList<>();
    Folder emailFolder = null;
    try {
      emailFolder = store.getFolder(INBOX);
      emailFolder.open(Folder.READ_WRITE);
      Message[] messages = emailFolder.getMessages();
      for (int i = 0; i < messages.length; i++) {
        Message message = messages[i];
        String messageId = message.getHeader("Message-ID")[0].toString();
        String from = Arrays.stream(message.getFrom()).map(Address::toString)
            .collect(Collectors.joining(","));
        String to = Arrays.stream(message.getRecipients(RecipientType.TO)).map(Address::toString)
            .collect(Collectors.joining(","));
        String subject = message.getSubject();
        Email email = new Email(messageId, from, to, subject);
        if (!processSeen && !message.getFlags().contains(Flag.SEEN)) {
          Optional<Multipart> multiParts = multipart(message);
          multiParts.ifPresent(m -> {
            partStream(m).forEach(p -> {
              if (fileName(p) != null) {
                email.addAttachment(new EmailAttachment(fileName(p), fileContent(p)));
              }
            });
          });
        }
        list.add(email);
      }
    } finally {
      if (emailFolder != null) {
        try {
          emailFolder.close(true);
        } catch (final MessagingException e) {
          log.error(e.getMessage(), e);
        }
      }
    }
    return list;
  }

  private Optional<Multipart> multipart(Message m) {
    try {
      Object content = m.getContent();
      m.getFlags().add(Flag.SEEN);
      if (content instanceof Multipart) {
        return Optional.of((Multipart) content);
      } else {
        return Optional.empty();
      }
    } catch (IOException | MessagingException e) {
      log.error(e.getMessage(), e);
      return Optional.empty();
    }
  }

  private Stream<BodyPart> partStream(Multipart multipart) {
    try {
      List<BodyPart> list = new ArrayList<>();
      for (int k = 0; k < multipart.getCount(); k++) {
        BodyPart bodyPart = multipart.getBodyPart(k);
        list.add(bodyPart);
      }
      return list.stream();
    } catch (MessagingException e) {
      log.error(e.getMessage(), e);
      return Stream.empty();
    }
  }

  private String fileName(BodyPart bodyPart) {
    try {
      return bodyPart.getFileName();
    } catch (MessagingException e) {
      log.error(e.getMessage(), e);
      return null;
    }
  }

  private String fileContent(BodyPart bodyPart) {
    try (BufferedReader br = new BufferedReader(
        new InputStreamReader(bodyPart.getInputStream(), StandardCharsets.UTF_8));) {
      return br.lines().collect(Collectors.joining(System.lineSeparator()));
    } catch (MessagingException | IOException e) {
      log.error(e.getMessage(), e);
      return null;
    }
  }

}
