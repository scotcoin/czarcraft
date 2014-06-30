package nxt.http;

import nxt.Constants;
import nxt.peer.Hallmark;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class MarkHost extends APIServlet.APIRequestHandler {

    static final MarkHost instance = new MarkHost();

    private MarkHost() {
        super("secretPhrase", "host", "weight", "date");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String secretPhrase = req.getParameter("secretPhrase");
        String host = req.getParameter("host");
        String weightValue = req.getParameter("weight");
        String dateValue = req.getParameter("date");
        if (secretPhrase == null) {
            return JSONI18NResponses.getErrorResponse("MISSING_SECRET_PHRASE");
        } else if (host == null) {
            return JSONI18NResponses.getErrorResponse("MISSING_HOST");
        } else if (weightValue == null) {
            return JSONI18NResponses.getErrorResponse("MISSING_WEIGHT");
        } else if (dateValue == null) {
            return JSONI18NResponses.getErrorResponse("MISSING_DATE");
        }

        if (host.length() > 100) {
            return JSONI18NResponses.getErrorResponse("INCORRECT_HOST");
        }

        int weight;
        try {
            weight = Integer.parseInt(weightValue);
            if (weight <= 0 || weight > Constants.MAX_BALANCE_NXT) {
                return JSONI18NResponses.getErrorResponse("INCORRECT_WEIGHT");
            }
        } catch (NumberFormatException e) {
            return JSONI18NResponses.getErrorResponse("INCORRECT_WEIGHT");
        }

        try {

            String hallmark = Hallmark.generateHallmark(secretPhrase, host, weight, Hallmark.parseDate(dateValue));

            JSONObject response = new JSONObject();
            response.put("hallmark", hallmark);
            return response;

        } catch (RuntimeException e) {
            return JSONI18NResponses.getErrorResponse("INCORRECT_DATE");
        }

    }

    @Override
    boolean requirePost() {
        return true;
    }

}
