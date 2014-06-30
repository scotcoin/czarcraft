package nxt.http;

import nxt.Nxt;
import nxt.NxtException;
import nxt.Transaction;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class ParseTransaction extends APIServlet.APIRequestHandler {

    static final ParseTransaction instance = new ParseTransaction();

    private ParseTransaction() {
        super("transactionBytes");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException.ValidationException {

        String transactionBytes = req.getParameter("transactionBytes");
        if (transactionBytes == null) {
            return JSONI18NResponses.getErrorResponse("MISSING_TRANSACTION_BYTES");
        }
        JSONObject response;
        try {
            byte[] bytes = Convert.parseHexString(transactionBytes);
            Transaction transaction = Nxt.getTransactionProcessor().parseTransaction(bytes);
            transaction.validateAttachment();
            response = JSONData.unconfirmedTransaction(transaction);
            response.put("verify", transaction.verify());
        } catch (NxtException.ValidationException|RuntimeException e) {
            //Logger.logDebugMessage(e.getMessage(), e);
            return JSONI18NResponses.getErrorResponse("INCORRECT_TRANSACTION_BYTES");
        }
        return response;
    }

}
