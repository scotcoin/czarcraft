package nxt.http;

import nxt.Account;
import nxt.NxtException;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetGuaranteedBalance extends APIServlet.APIRequestHandler {

    static final GetGuaranteedBalance instance = new GetGuaranteedBalance();

    private GetGuaranteedBalance() {
        super("account", "numberOfConfirmations");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Account account = ParameterParser.getAccount(req);

        String numberOfConfirmationsValue = req.getParameter("numberOfConfirmations");
        if (numberOfConfirmationsValue == null) {
            return JSONI18NResponses.getErrorResponse("MISSING_NUMBER_OF_CONFIRMATIONS");
        }

        JSONObject response = new JSONObject();
        if (account == null) {
            response.put("guaranteedBalanceNQT", "0");
        } else {
            try {
                int numberOfConfirmations = Integer.parseInt(numberOfConfirmationsValue);
                response.put("guaranteedBalanceNQT", String.valueOf(account.getGuaranteedBalanceNQT(numberOfConfirmations)));
            } catch (NumberFormatException e) {
                return JSONI18NResponses.getErrorResponse("INCORRECT_NUMBER_OF_CONFIRMATIONS");
            }
        }

        return response;
    }

}
