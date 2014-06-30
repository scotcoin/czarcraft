package nxt.http;

import nxt.Nxt;
import nxt.Transaction;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetTransactionBytes extends APIServlet.APIRequestHandler {

    static final GetTransactionBytes instance = new GetTransactionBytes();

    private GetTransactionBytes() {
        super("transaction");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String transactionValue = req.getParameter("transaction");
        if (transactionValue == null) {
            return JSONI18NResponses.getErrorResponse("MISSING_TRANSACTION");
        }

        Long transactionId;
        Transaction transaction;
        try {
            transactionId = Convert.parseUnsignedLong(transactionValue);
        } catch (RuntimeException e) {
            return JSONI18NResponses.getErrorResponse("INCORRECT_TRANSACTION");
        }

        transaction = Nxt.getBlockchain().getTransaction(transactionId);
        JSONObject response = new JSONObject();
        if (transaction == null) {
            transaction = Nxt.getTransactionProcessor().getUnconfirmedTransaction(transactionId);
            if (transaction == null) {
                return JSONI18NResponses.getErrorResponse("UNKNOWN_TRANSACTION");
            }
        } else {
            response.put("confirmations", Nxt.getBlockchain().getLastBlock().getHeight() - transaction.getHeight());
        }
        response.put("transactionBytes", Convert.toHexString(transaction.getBytes()));
        response.put("unsignedTransactionBytes", Convert.toHexString(transaction.getUnsignedBytes()));
        return response;

    }

}
