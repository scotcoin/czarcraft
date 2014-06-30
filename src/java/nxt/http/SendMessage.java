package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.Constants;
import nxt.NxtException;
import nxt.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class SendMessage extends CreateTransaction {

    static final SendMessage instance = new SendMessage();

    private SendMessage() {
        super("recipient", "message");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Long recipient = ParameterParser.getRecipientId(req);

        String messageValue = req.getParameter("message");
        if (messageValue == null) {
            return JSONI18NResponses.getErrorResponse("MISSING_MESSAGE");
        }

        byte[] message;
        try {
            message = Convert.parseHexString(messageValue);
        } catch (RuntimeException e) {
            return JSONI18NResponses.getErrorResponse("INCORRECT_ARBITRARY_MESSAGE");
        }
        if (message.length > Constants.MAX_ARBITRARY_MESSAGE_LENGTH) {
            return JSONI18NResponses.getErrorResponse("INCORRECT_ARBITRARY_MESSAGE");
        }

        Account account = ParameterParser.getSenderAccount(req);

        Attachment attachment = new Attachment.MessagingArbitraryMessage(message);
        return createTransaction(req, account, recipient, 0, attachment);

    }

}
