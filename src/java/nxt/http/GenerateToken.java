package nxt.http;

import nxt.Token;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GenerateToken extends APIServlet.APIRequestHandler {

    static final GenerateToken instance = new GenerateToken();

    private GenerateToken() {
        super("website", "secretPhrase");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String secretPhrase = req.getParameter("secretPhrase");
        String website = req.getParameter("website");
        if (secretPhrase == null) {
            return JSONI18NResponses.getErrorResponse("MISSING_SECRET_PHRASE");
        } else if (website == null) {
            return JSONI18NResponses.getErrorResponse("MISSING_WEBSITE");
        }

        try {

            String tokenString = Token.generateToken(secretPhrase, website.trim());

            JSONObject response = new JSONObject();
            response.put("token", tokenString);

            return response;

        } catch (RuntimeException e) {
            return JSONI18NResponses.getErrorResponse("INCORRECT_WEBSITE");
        }

    }

    @Override
    boolean requirePost() {
        return true;
    }

}
