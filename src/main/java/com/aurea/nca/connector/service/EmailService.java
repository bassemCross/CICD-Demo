package com.aurea.nca.connector.service;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import javax.mail.Message;
import javax.mail.MessagingException;
import com.aurea.nca.connector.model.Email;


public interface EmailService {

  void send(String host, String user, String password, int port, String from, String to,
      String subject, String text, Optional<File> attachment, boolean auth, boolean tls)
      throws MessagingException;

  void clearAllInboxEmails(String imapHost, String user, String password, int port);

  void clearFilteredInboxEmails(String imapHost, String user, String password, int port,
      Predicate<Message> messageFilter);

  List<Email> getAttachments(String host, String user, String password, int port, String protocol,
      boolean ssl,boolean processSeen);

}
