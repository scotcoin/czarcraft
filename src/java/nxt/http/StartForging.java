package nxt.http;

import nxt.Generator;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class StartForging extends APIServlet.APIRequestHandler {

    static final StartForging instance = new StartForging();

    private StartForging() {
        super("secretPhrase");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String secretPhrase = req.getParameter("secretPhrase");
        if (secretPhrase == null) {
            return JSONI18NResponses.getErrorResponse("MISSING_SECRET_PHRASE");
        }

        Generator generator = Generator.startForging(secretPhrase);
        if (generator == null) {
            return JSONI18NResponses.getErrorResponse("UNKNOWN_ACCOUNT");
        }

        JSONObject response = new JSONObject();
        response.put("deadline", generator.getDeadline());
        return response;

    }

    @Override
    boolean requirePost() {
        return true;
    }

}
