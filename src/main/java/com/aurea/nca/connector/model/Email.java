package com.aurea.nca.connector.model;

import java.util.ArrayList;
import java.util.List;

public class Email {
  private String messageId;
  private String from;
  private String to;
  private String subject;
  private List<EmailAttachment> attachments;

  public Email(String messageId,String from, String to, String subject) {
    super();
    this.messageId = messageId;
    this.from = from;
    this.to = to;
    this.subject = subject;
    this.attachments = new ArrayList<>();
  }

  public String getMessageId() {
    return messageId;
  }

  public List<EmailAttachment> getAttachments() {
    return attachments;
  }
  
  public String getFrom() {
    return from;
  }

  public String getTo() {
    return to;
  }

  public String getSubject() {
    return subject;
  }
  
  public void addAttachment(EmailAttachment attachment) {
    getAttachments().add(attachment);
  }


}
