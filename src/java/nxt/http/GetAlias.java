package nxt.http;

import nxt.Alias;
import nxt.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAlias extends APIServlet.APIRequestHandler {

    static final GetAlias instance = new GetAlias();

    private GetAlias() {
        super("alias", "aliasName");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        Long aliasId;
        try {
            aliasId = Convert.parseUnsignedLong(Convert.emptyToNull(req.getParameter("alias")));
        } catch (RuntimeException e) {
            return JSONI18NResponses.getErrorResponse("INCORRECT_ALIAS");
        }
        String aliasName = Convert.emptyToNull(req.getParameter("aliasName"));

        Alias alias;
        if (aliasId != null) {
            alias = Alias.getAlias(aliasId);
        } else if (aliasName != null) {
            alias = Alias.getAlias(aliasName);
        } else {
            return JSONI18NResponses.getErrorResponse("MISSING_ALIAS_OR_ALIAS_NAME");
        }
        if (alias == null) {
            return JSONI18NResponses.getErrorResponse("UNKNOWN_ALIAS");
        }

        return JSONData.alias(alias);
    }

}
