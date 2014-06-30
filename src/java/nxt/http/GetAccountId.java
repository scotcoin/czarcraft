package nxt.http;

import nxt.Account;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAccountId extends APIServlet.APIRequestHandler {

    static final GetAccountId instance = new GetAccountId();

    private GetAccountId() {
        super("secretPhrase");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String secretPhrase = req.getParameter("secretPhrase");
        if (secretPhrase == null) {
            return JSONI18NResponses.getErrorResponse("MISSING_SECRET_PHRASE");
        }

        byte[] publicKey = Crypto.getPublicKey(secretPhrase);

        JSONObject response = new JSONObject();
        Long accountId = Account.getId(publicKey);
        response.put("accountId", Convert.toUnsignedLong(accountId));
        response.put("accountRS", Convert.rsAccount(accountId));

        return response;
    }

    @Override
    boolean requirePost() {
        return true;
    }

}
