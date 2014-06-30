package nxt.http;

import nxt.Account;
import nxt.Generator;
import nxt.Nxt;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetForging extends APIServlet.APIRequestHandler {

    static final GetForging instance = new GetForging();

    private GetForging() {
        super("secretPhrase");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String secretPhrase = req.getParameter("secretPhrase");
        if (secretPhrase == null) {
            return JSONI18NResponses.getErrorResponse("MISSING_SECRET_PHRASE");
        }
        Account account = Account.getAccount(Crypto.getPublicKey(secretPhrase));
        if (account == null) {
            return JSONI18NResponses.getErrorResponse("UNKNOWN_ACCOUNT");
        }

        Generator generator = Generator.getGenerator(secretPhrase);
        if (generator == null) {
            return JSONI18NResponses.getErrorResponse("NOT_FORGING");
        }

        JSONObject response = new JSONObject();
        long deadline = generator.getDeadline();
        response.put("deadline", deadline);
        int elapsedTime = Convert.getEpochTime() - Nxt.getBlockchain().getLastBlock().getTimestamp();
        response.put("remaining", Math.max(deadline - elapsedTime, 0));
        return response;

    }

    @Override
    boolean requirePost() {
        return true;
    }

}
