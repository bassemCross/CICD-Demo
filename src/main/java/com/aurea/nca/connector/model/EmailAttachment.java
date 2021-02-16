package com.aurea.nca.connector.model;

public class EmailAttachment {

  private String fileName;
  private String fileContent;

  public EmailAttachment(String fileName, String fileContent) {
    super();
    this.fileName = fileName;
    this.fileContent = fileContent;
  }

  public String getFileName() {
    return fileName;
  }

  public String getFileContent() {
    return fileContent;
  }

}
