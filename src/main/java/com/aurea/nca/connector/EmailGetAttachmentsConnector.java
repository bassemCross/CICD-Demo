package com.aurea.nca.connector;

import static com.aurea.nca.connector.util.EmailConstants.HOST;
import static com.aurea.nca.connector.util.EmailConstants.IMAP;
import static com.aurea.nca.connector.util.EmailConstants.PASSWORD;
import static com.aurea.nca.connector.util.EmailConstants.POP3;
import static com.aurea.nca.connector.util.EmailConstants.PORT;
import static com.aurea.nca.connector.util.EmailConstants.PROTOCOL;
import static com.aurea.nca.connector.util.EmailConstants.SSL;
import static com.aurea.nca.connector.util.EmailConstants.USER;
import org.apache.log4j.Logger;
import com.aurea.nca.connector.model.EmailAttachment;
import com.aurea.nca.connector.service.EmailService;
import com.aurea.nca.connector.service.impl.EmailServiceImpl;
import com.sonicsw.esb.service.common.SFCParameters;
import com.sonicsw.esb.service.common.SFCServiceContext;
import com.sonicsw.esb.service.common.impl.AbstractSFCServiceImpl;
import com.sonicsw.xq.XQAddressNotFoundException;
import com.sonicsw.xq.XQConstants;
import com.sonicsw.xq.XQDispatchException;
import com.sonicsw.xq.XQEnvelope;
import com.sonicsw.xq.XQMessage;
import com.sonicsw.xq.XQMessageException;
import com.sonicsw.xq.XQPart;
import com.sonicsw.xq.XQProcessAddress;
import com.sonicsw.xq.XQServiceException;

/**
 * EmailConnector SFC Service
 */
public class EmailGetAttachmentsConnector extends AbstractSFCServiceImpl {
  // access to the SFC's logging mechanism
  private final Logger log = Logger.getLogger(this.getClass());

  /**
   * Process each incoming message
   * 
   * @param _ctx runtime context of processing
   * @param _envelope contains the incoming message
   * @throws XQServiceException if the message cannot be correctly processed - message will be set
   *         to RME
   * @see com.sonicsw.esb.service.common.impl.AbstractSFCServiceImpl#doService(
   *      com.sonicsw.esb.service.common.SFCServiceContext, com.sonicsw.xq.XQEnvelope)
   */
  public final void doService(final SFCServiceContext _ctx, final XQEnvelope _envelope)
      throws XQServiceException {
    // get the parameters from the Service Context
    final SFCParameters parameters = _ctx.getParameters();
    String host = parameters.getParameter(HOST);
    String user = parameters.getParameter(USER);
    String password = parameters.getParameter(PASSWORD);
    int port = parameters.getIntParameter(PORT);
    int protocol = parameters.getIntParameter(PROTOCOL);
    boolean ssl = parameters.getBooleanParameter(SSL);
    String dest = parameters.getParameter("dest");
    log.debug("Connect to Email host: " + host + " with port: " + port + " and user: " + user
        + " and password: " + password + " over protocol: " + protocol + " and ssl: " + ssl);
    EmailService service = new EmailServiceImpl();
    service.getAttachments(host, user, password, port, (protocol == 0) ? POP3 : IMAP, ssl, false)
        .forEach(m -> {
          XQMessage message = _envelope.getMessage();
          try {
            message.removeAllParts();

            message.setStringHeader("messageId", m.getMessageId());
            message.setStringHeader("from", m.getFrom());
            message.setStringHeader("to", m.getTo());
            message.setStringHeader("subject", m.getSubject());
            int index = 0;
            for (EmailAttachment a : m.getAttachments()) {
              XQPart part;
              try {
                part = message.createPart();
                part.setContent(a.getFileContent(), XQConstants.CONTENT_TYPE_TEXT);
                part.setContentId("EmailAttachment" + index++);
                part.getHeader().setValue("fileName", a.getFileName());
                message.addPart(part);
              } catch (XQMessageException e) {
                log.error(e.getMessage(), e);
              }
            }

            XQProcessAddress paddr = _ctx.getAddressFactory().createProcessAddress(dest);
            XQEnvelope dispatchEnv = _ctx.getEnvelopeFactory().createTargetedEnvelope(paddr);
            dispatchEnv.setMessage(message);
            _ctx.getDispatcher().dispatch(dispatchEnv);


          } catch (XQMessageException | XQAddressNotFoundException | XQDispatchException e) {
            log.error(e.getMessage(), e);
          }
        });
  }
}
