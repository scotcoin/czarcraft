package nxt.http;

import nxt.Token;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class DecodeToken extends APIServlet.APIRequestHandler {

    static final DecodeToken instance = new DecodeToken();

    private DecodeToken() {
        super("website", "token");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) {

        String website = req.getParameter("website");
        String tokenString = req.getParameter("token");
        if (website == null) {
            return JSONI18NResponses.getErrorResponse("MISSING_WEBSITE");
        } else if (tokenString == null) {
            return JSONI18NResponses.getErrorResponse("MISSING_TOKEN");
        }

        try {

            Token token = Token.parseToken(tokenString, website.trim());

            return JSONData.token(token);

        } catch (RuntimeException e) {
            return JSONI18NResponses.getErrorResponse("INCORRECT_WEBSITE");
        }
    }

}
