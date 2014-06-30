package nxt.http;

import nxt.Account;
import nxt.Alias;
import nxt.Attachment;
import nxt.Constants;
import nxt.NxtException;
import nxt.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class SetAlias extends CreateTransaction {

    static final SetAlias instance = new SetAlias();

    private SetAlias() {
        super("aliasName", "aliasURI");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        String aliasName = Convert.emptyToNull(req.getParameter("aliasName"));
        String aliasURI = Convert.nullToEmpty(req.getParameter("aliasURI"));

        if (aliasName == null) {
            return JSONI18NResponses.getErrorResponse("MISSING_ALIAS_NAME");
        }

        aliasName = aliasName.trim();
        if (aliasName.length() == 0 || aliasName.length() > Constants.MAX_ALIAS_LENGTH) {
            return JSONI18NResponses.getErrorResponse("INCORRECT_ALIAS_LENGTH");
        }

        String normalizedAlias = aliasName.toLowerCase();
        for (int i = 0; i < normalizedAlias.length(); i++) {
            if (Constants.ALPHABET.indexOf(normalizedAlias.charAt(i)) < 0) {
                return JSONI18NResponses.getErrorResponse("INCORRECT_ALIAS_NAME");
            }
        }

        aliasURI = aliasURI.trim();
        if (aliasURI.length() > Constants.MAX_ALIAS_URI_LENGTH) {
            return JSONI18NResponses.getErrorResponse("INCORRECT_URI_LENGTH");
        }

        Account account = ParameterParser.getSenderAccount(req);

        Alias alias = Alias.getAlias(normalizedAlias);
        if (alias != null && !alias.getAccount().getId().equals(account.getId())) {           
            return JSONI18NResponses.getErrorResponse("ERROR_ALIAS_USED");
        }

        Attachment attachment = new Attachment.MessagingAliasAssignment(aliasName, aliasURI);
        return createTransaction(req, account, attachment);

    }

}
